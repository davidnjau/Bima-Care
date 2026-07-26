package care.bima.payments.events

import care.bima.payments.db.PaymentRepository
import care.bima.payments.domain.Payment
import care.bima.shared.events.ClaimAdjudicatedPayload
import care.bima.shared.events.EventEnvelope
import care.bima.shared.events.Topics
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

private val logger = LoggerFactory.getLogger(ClaimAdjudicatedConsumer::class.java)
private const val POLL_TIMEOUT_MILLIS = 500L
private val PAYABLE_STATUSES = setOf("APPROVED", "PARTIALLY_APPROVED")

/**
 * The first real Kafka *consumer* in this codebase (every other service so far only produces).
 * Auto-cascades claim.adjudicated -> payment.released with no manual release step - there's no
 * real bank integration yet, so an approved/partially-approved claim just becomes a ledger entry.
 */
class ClaimAdjudicatedConsumer(
    bootstrapServers: String,
    private val repository: PaymentRepository,
    private val publisher: PaymentEventPublisher,
) {
    private val consumer =
        KafkaConsumer<String, String>(
            Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(ConsumerConfig.GROUP_ID_CONFIG, "payments-service")
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            },
        )

    @Volatile private var running = true

    fun start(): Thread {
        consumer.subscribe(listOf(Topics.CLAIM_ADJUDICATED))
        val thread =
            Thread {
                while (running) {
                    pollOnce()
                }
            }
        thread.isDaemon = true
        thread.name = "claim-adjudicated-consumer"
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
                // Per-record, not per-batch: see the comment in
                // MemberAccountProvisioningConsumer.pollOnce() for why this must not be shared.
                logger.error("Failed to process claim.adjudicated record at offset ${record.offset()}", e)
            }
        }
    }

    private fun handleRecord(value: String) {
        val envelope =
            Json.decodeFromString(EventEnvelope.serializer(ClaimAdjudicatedPayload.serializer()), value)
        val payload = envelope.payload
        if (payload.status !in PAYABLE_STATUSES || payload.approvedAmount == null) return

        val claimId = UUID.fromString(payload.claimId)
        if (repository.findByClaimId(claimId) != null) return // already released - redelivery no-op

        val payment =
            Payment(
                id = UUID.randomUUID(),
                claimId = claimId,
                patientId = UUID.fromString(payload.patientId),
                amount = BigDecimal(payload.approvedAmount),
                releasedAt = LocalDateTime.now(),
            )
        val created = repository.create(payment)
        publisher.publishPaymentReleased(created)
    }
}
