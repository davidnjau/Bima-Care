package care.bima.sync.events

import care.bima.shared.service.ServiceToServiceClient
import care.bima.sync.RegistrationMapping
import care.bima.sync.registrationMappings
import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties

private val logger = LoggerFactory.getLogger(RegistrationSyncConsumer::class.java)
private const val POLL_TIMEOUT_MILLIS = 500L

/**
 * Mirrors each newly-registered Patient/Practitioner/Organization into hapi-fhir-server, using
 * our own UUID as the FHIR resource id (PUT, not POST) - that's what keeps cross-referencing
 * trivial later and makes this handler naturally idempotent on Kafka redelivery: re-syncing the
 * same resource just creates a new version of the same FHIR resource, never a duplicate.
 */
class RegistrationSyncConsumer(
    bootstrapServers: String,
    private val serviceClient: ServiceToServiceClient,
    private val httpClient: HttpClient,
    private val hapiFhirServerUrl: String,
) {
    private val mappingsByTopic: Map<String, RegistrationMapping> = registrationMappings().associateBy { it.topic }

    private val consumer =
        KafkaConsumer<String, String>(
            Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(ConsumerConfig.GROUP_ID_CONFIG, "sync-service")
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            },
        )

    @Volatile private var running = true

    fun start(): Thread {
        consumer.subscribe(mappingsByTopic.keys.toList())
        val thread =
            Thread {
                while (running) {
                    pollOnce()
                }
            }
        thread.isDaemon = true
        thread.name = "registration-sync-consumer"
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
                handleRecord(record.topic(), record.value())
            } catch (e: Exception) {
                // Per-record, not per-batch: see the comment in
                // MemberAccountProvisioningConsumer.pollOnce() for why this must not be shared.
                logger.error("Failed to sync record at offset ${record.offset()} on ${record.topic()}", e)
            }
        }
    }

    private suspend fun sync(
        mapping: RegistrationMapping,
        resourceId: String,
    ) {
        val fhirResponse =
            serviceClient.get("${mapping.internalBaseUrl}${mapping.internalPluralPath}/$resourceId/fhir")
        val fhirJson = fhirResponse.bodyAsText()

        httpClient.put("$hapiFhirServerUrl/${mapping.fhirResourceType}/$resourceId") {
            contentType(ContentType.parse("application/fhir+json"))
            setBody(fhirJson)
        }
        logger.info("Synced ${mapping.fhirResourceType}/$resourceId into hapi-fhir-server")
    }

    private fun handleRecord(
        topic: String,
        value: String,
    ) {
        val mapping = mappingsByTopic[topic] ?: return
        val resourceId = Json.parseToJsonElement(value).jsonObject["resourceId"]?.jsonPrimitive?.content ?: return
        kotlinx.coroutines.runBlocking { sync(mapping, resourceId) }
    }
}
