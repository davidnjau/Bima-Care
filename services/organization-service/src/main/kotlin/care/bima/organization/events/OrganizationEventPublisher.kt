package care.bima.organization.events

import care.bima.organization.domain.Organization
import care.bima.shared.events.EventEnvelope
import care.bima.shared.events.Topics
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Instant
import java.util.Properties
import java.util.UUID

class OrganizationEventPublisher(bootstrapServers: String) {
    private val producer =
        KafkaProducer<String, String>(
            Properties().apply {
                put("bootstrap.servers", bootstrapServers)
                put("key.serializer", StringSerializer::class.java.name)
                put("value.serializer", StringSerializer::class.java.name)
            },
        )

    fun publishOrganizationCreated(organization: Organization) {
        val envelope =
            EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = Topics.ORGANIZATION_CREATED,
                resourceId = organization.id.toString(),
                version = 1,
                occurredAt = Instant.now().toString(),
                payload = organization.registrationNumber,
            )
        val json = Json.encodeToString(EventEnvelope.serializer(String.serializer()), envelope)
        producer.send(ProducerRecord(Topics.ORGANIZATION_CREATED, organization.id.toString(), json))
    }

    fun close() = producer.close()
}
