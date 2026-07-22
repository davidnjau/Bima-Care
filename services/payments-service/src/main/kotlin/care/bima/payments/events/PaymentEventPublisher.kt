package care.bima.payments.events

import care.bima.payments.domain.Payment
import care.bima.shared.events.EventEnvelope
import care.bima.shared.events.PaymentReleasedPayload
import care.bima.shared.events.Topics
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Instant
import java.util.Properties
import java.util.UUID

class PaymentEventPublisher(bootstrapServers: String) {
    private val producer =
        KafkaProducer<String, String>(
            Properties().apply {
                put("bootstrap.servers", bootstrapServers)
                put("key.serializer", StringSerializer::class.java.name)
                put("value.serializer", StringSerializer::class.java.name)
            },
        )

    fun publishPaymentReleased(payment: Payment) {
        val envelope =
            EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = Topics.PAYMENT_RELEASED,
                resourceId = payment.id.toString(),
                version = 1,
                occurredAt = Instant.now().toString(),
                payload =
                    PaymentReleasedPayload(
                        paymentId = payment.id.toString(),
                        claimId = payment.claimId.toString(),
                        patientId = payment.patientId.toString(),
                        amount = payment.amount.toPlainString(),
                    ),
            )
        val json = Json.encodeToString(EventEnvelope.serializer(PaymentReleasedPayload.serializer()), envelope)
        producer.send(ProducerRecord(Topics.PAYMENT_RELEASED, payment.patientId.toString(), json))
    }

    fun close() = producer.close()
}
