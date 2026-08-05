package care.bima.iam.events

import care.bima.iam.clients.KeycloakAdminClient
import care.bima.iam.clients.OrganizationClient
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

private val logger = LoggerFactory.getLogger(InsurerAccountProvisioningConsumer::class.java)
private const val POLL_TIMEOUT_MILLIS = 500L
private const val INSURER_ORGANIZATION_TYPE = "INSURER"

/**
 * Gives every newly-registered Insurer organization a real Keycloak login, the same way
 * MemberAccountProvisioningConsumer does for Patients - reacts to `organization.created`, which
 * fires for every organization type (hospitals, clinics, labs, pharmacies too), so this skips
 * anything that isn't an Insurer. Emails the temp password when the organization has one on
 * file; falls back to logging it when they don't, or if delivery fails.
 */
class InsurerAccountProvisioningConsumer(
    bootstrapServers: String,
    private val organizationClient: OrganizationClient,
    private val keycloakAdminClient: KeycloakAdminClient,
    private val mailer: SmtpMailer,
) {
    private val consumer =
        KafkaConsumer<String, String>(
            Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(ConsumerConfig.GROUP_ID_CONFIG, "iam-service-insurer-provisioning")
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            },
        )

    @Volatile private var running = true

    fun start(): Thread {
        consumer.subscribe(listOf(Topics.ORGANIZATION_CREATED))
        val thread =
            Thread {
                while (running) {
                    pollOnce()
                }
            }
        thread.isDaemon = true
        thread.name = "insurer-account-provisioning-consumer"
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
                // Isolated per-record - see MemberAccountProvisioningConsumer for why this can't
                // be a single try/catch around the whole forEach.
                logger.error("Failed to provision an insurer account for record at offset ${record.offset()}", e)
            }
        }
    }

    private suspend fun provision(organizationId: UUID) {
        val organization = organizationClient.getOrganization(organizationId)
        if (organization.type != INSURER_ORGANIZATION_TYPE) return

        val tempPassword = keycloakAdminClient.provisionInsurerAccount(organization)
        val emailed =
            organization.email
                ?.let { mailer.sendTempPasswordEmail(it, organization.name, organization.phone, tempPassword) }
                ?: false
        if (emailed) {
            logger.info("Provisioned Insurer account for organization $organizationId - emailed temp password")
        } else {
            logger.warn(
                "Provisioned Insurer account for organization $organizationId - " +
                    "username='${organization.phone}' temp password='$tempPassword' (no email on file or " +
                    "delivery failed - this log line is the only record of this password)",
            )
        }
    }

    private fun handleRecord(value: String) {
        val resourceId = Json.parseToJsonElement(value).jsonObject["resourceId"]?.jsonPrimitive?.content ?: return
        kotlinx.coroutines.runBlocking { provision(UUID.fromString(resourceId)) }
    }
}
