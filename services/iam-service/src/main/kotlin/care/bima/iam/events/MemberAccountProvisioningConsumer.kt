package care.bima.iam.events

import care.bima.iam.clients.KeycloakAdminClient
import care.bima.iam.clients.PatientClient
import care.bima.iam.clients.SmtpMailer
import care.bima.shared.events.Topics
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties
import java.util.UUID

private val logger = LoggerFactory.getLogger(MemberAccountProvisioningConsumer::class.java)
private const val POLL_TIMEOUT_MILLIS = 500L

/**
 * Gives every newly-registered Patient a real Keycloak login - previously the Member portal was
 * demo-only (see IMPLEMENTATION_GUIDE.md §9). Emails the temp password when the patient has one
 * on file; falls back to logging it (the only delivery path before a real email was wired up)
 * when they don't, or if delivery fails.
 */
class MemberAccountProvisioningConsumer(
    bootstrapServers: String,
    private val patientClient: PatientClient,
    private val keycloakAdminClient: KeycloakAdminClient,
    private val mailer: SmtpMailer,
) {
    private val consumer =
        KafkaConsumer<String, String>(
            Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(ConsumerConfig.GROUP_ID_CONFIG, "iam-service-member-provisioning")
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            },
        )

    @Volatile private var running = true

    fun start(): Thread {
        consumer.subscribe(listOf(Topics.PATIENT_CREATED))
        val thread =
            Thread {
                while (running) {
                    pollOnce()
                }
            }
        thread.isDaemon = true
        thread.name = "member-account-provisioning-consumer"
        thread.start()
        return thread
    }

    fun stop() {
        running = false
        consumer.wakeup()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun pollOnce() {
        val records = consumer.poll(Duration.ofMillis(POLL_TIMEOUT_MILLIS))
        records.forEach { record ->
            try {
                handleRecord(record.value())
            } catch (e: Exception) {
                // Isolated per-record: one bad/duplicate registration must not skip every other
                // record already fetched in this same poll() batch - offsets auto-commit past the
                // whole batch regardless of per-record outcome, so a shared try/catch around the
                // forEach silently drops every sibling after the first failure.
                logger.error("Failed to provision a member account for record at offset ${record.offset()}", e)
            }
        }
    }

    private suspend fun provision(patientId: UUID) {
        val patient = patientClient.getPatient(patientId)
        val tempPassword = keycloakAdminClient.provisionMemberAccount(patient)
        val emailed =
            patient.email?.let { mailer.sendTempPasswordEmail(it, patient.firstName, patient.phone, tempPassword) }
                ?: false
        if (emailed) {
            logger.info("Provisioned Member account for patient $patientId - emailed temp password to on-file address")
        } else {
            logger.warn(
                "Provisioned Member account for patient $patientId - username='${patient.phone}' " +
                    "temp password='$tempPassword' (no email on file or delivery failed - this log line " +
                    "is the only record of this password, relay it to the member out of band)",
            )
        }
    }

    private fun handleRecord(value: String) {
        val resourceId = Json.parseToJsonElement(value).jsonObject["resourceId"]?.jsonPrimitive?.content ?: return
        kotlinx.coroutines.runBlocking { provision(UUID.fromString(resourceId)) }
    }
}
