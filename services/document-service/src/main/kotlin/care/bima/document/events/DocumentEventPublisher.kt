package care.bima.document.events

import care.bima.document.domain.Document
import care.bima.shared.events.DocumentUploadedPayload
import care.bima.shared.events.EventEnvelope
import care.bima.shared.events.Topics
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Instant
import java.util.Properties
import java.util.UUID

class DocumentEventPublisher(bootstrapServers: String) {
    private val producer =
        KafkaProducer<String, String>(
            Properties().apply {
                put("bootstrap.servers", bootstrapServers)
                put("key.serializer", StringSerializer::class.java.name)
                put("value.serializer", StringSerializer::class.java.name)
            },
        )

    fun publishDocumentUploaded(document: Document) {
        val envelope =
            EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = Topics.DOCUMENT_UPLOADED,
                resourceId = document.id.toString(),
                version = 1,
                occurredAt = Instant.now().toString(),
                payload =
                    DocumentUploadedPayload(
                        documentId = document.id.toString(),
                        patientId = document.patientId.toString(),
                        contentType = document.contentType,
                        category = document.category,
                    ),
            )
        val json = Json.encodeToString(EventEnvelope.serializer(DocumentUploadedPayload.serializer()), envelope)
        producer.send(ProducerRecord(Topics.DOCUMENT_UPLOADED, document.patientId.toString(), json))
    }

    fun close() = producer.close()
}
