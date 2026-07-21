package care.bima.patient.events

import care.bima.patient.domain.Patient
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

class PatientEventPublisher(bootstrapServers: String) {
    private val producer =
        KafkaProducer<String, String>(
            Properties().apply {
                put("bootstrap.servers", bootstrapServers)
                put("key.serializer", StringSerializer::class.java.name)
                put("value.serializer", StringSerializer::class.java.name)
            },
        )

    fun publishPatientCreated(patient: Patient) {
        val envelope =
            EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = Topics.PATIENT_CREATED,
                resourceId = patient.id.toString(),
                version = 1,
                occurredAt = Instant.now().toString(),
                payload = patient.nationalId,
            )
        val json = Json.encodeToString(EventEnvelope.serializer(String.serializer()), envelope)
        producer.send(ProducerRecord(Topics.PATIENT_CREATED, patient.id.toString(), json))
    }

    fun close() = producer.close()
}
