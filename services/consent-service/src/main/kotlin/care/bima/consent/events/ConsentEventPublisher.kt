package care.bima.consent.events

import care.bima.consent.domain.Consent
import care.bima.shared.events.ConsentUpdatedPayload
import care.bima.shared.events.EventEnvelope
import care.bima.shared.events.Topics
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Instant
import java.util.Properties
import java.util.UUID

class ConsentEventPublisher(bootstrapServers: String) {
    private val producer =
        KafkaProducer<String, String>(
            Properties().apply {
                put("bootstrap.servers", bootstrapServers)
                put("key.serializer", StringSerializer::class.java.name)
                put("value.serializer", StringSerializer::class.java.name)
            },
        )

    fun publishConsentUpdated(consent: Consent) {
        val envelope =
            EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = Topics.CONSENT_UPDATED,
                resourceId = consent.id.toString(),
                version = 1,
                occurredAt = Instant.now().toString(),
                payload =
                    ConsentUpdatedPayload(
                        consentId = consent.id.toString(),
                        patientId = consent.patientId.toString(),
                        granteeId = consent.granteeId.toString(),
                        status = consent.status.name,
                    ),
            )
        val json = Json.encodeToString(EventEnvelope.serializer(ConsentUpdatedPayload.serializer()), envelope)
        producer.send(ProducerRecord(Topics.CONSENT_UPDATED, consent.patientId.toString(), json))
    }

    fun close() = producer.close()
}
