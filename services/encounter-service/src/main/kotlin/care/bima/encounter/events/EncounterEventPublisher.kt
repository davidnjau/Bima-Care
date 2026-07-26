package care.bima.encounter.events

import care.bima.encounter.domain.Encounter
import care.bima.shared.events.EncounterStartedPayload
import care.bima.shared.events.EventEnvelope
import care.bima.shared.events.Topics
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Instant
import java.util.Properties
import java.util.UUID

class EncounterEventPublisher(bootstrapServers: String) {
    private val producer =
        KafkaProducer<String, String>(
            Properties().apply {
                put("bootstrap.servers", bootstrapServers)
                put("key.serializer", StringSerializer::class.java.name)
                put("value.serializer", StringSerializer::class.java.name)
            },
        )

    fun publishEncounterStarted(encounter: Encounter) {
        val envelope =
            EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = Topics.ENCOUNTER_STARTED,
                resourceId = encounter.id.toString(),
                version = 1,
                occurredAt = Instant.now().toString(),
                payload =
                    EncounterStartedPayload(
                        encounterId = encounter.id.toString(),
                        patientId = encounter.patientId.toString(),
                        practitionerId = encounter.practitionerId.toString(),
                        organizationId = encounter.organizationId.toString(),
                    ),
            )
        val json = Json.encodeToString(EventEnvelope.serializer(EncounterStartedPayload.serializer()), envelope)
        producer.send(ProducerRecord(Topics.ENCOUNTER_STARTED, encounter.patientId.toString(), json))
    }

    fun close() = producer.close()
}
