package care.bima.audit.events

import care.bima.audit.db.AuditEventRepository
import care.bima.shared.events.Topics
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties

private val ALL_TOPICS =
    listOf(
        Topics.PATIENT_CREATED,
        Topics.COVERAGE_VERIFIED,
        Topics.ENCOUNTER_STARTED,
        Topics.CLAIM_SUBMITTED,
        Topics.CLAIM_ADJUDICATED,
        Topics.PAYMENT_RELEASED,
        Topics.CONSENT_UPDATED,
        Topics.DOCUMENT_UPLOADED,
    )

private const val POLL_TIMEOUT_MILLIS = 500L
private val POLL_TIMEOUT = Duration.ofMillis(POLL_TIMEOUT_MILLIS)

class AuditEventConsumer(
    bootstrapServers: String,
    private val repository: AuditEventRepository,
) {
    private val log = LoggerFactory.getLogger(AuditEventConsumer::class.java)

    private val consumer =
        KafkaConsumer<String, String>(
            Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(ConsumerConfig.GROUP_ID_CONFIG, "audit-service")
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            },
        )

    @Volatile private var running = true

    fun start(): Thread {
        consumer.subscribe(ALL_TOPICS)
        val thread =
            Thread {
                try {
                    while (running) {
                        pollOnce()
                    }
                } finally {
                    consumer.close()
                }
            }
        thread.name = "audit-consumer"
        thread.isDaemon = true
        thread.start()
        return thread
    }

    private fun pollOnce() {
        val records =
            try {
                consumer.poll(POLL_TIMEOUT)
            } catch (e: WakeupException) {
                if (running) throw e
                return
            }
        records.forEach { record ->
            try {
                handle(record)
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                // Per-record, not per-batch: a shared try/catch around the whole forEach means one
                // bad record silently skips every sibling already fetched in this same poll() -
                // offsets auto-commit past the whole batch regardless of per-record outcome.
                log.error("Failed to process audit record at offset ${record.offset()}", e)
            }
        }
    }

    private fun handle(record: ConsumerRecord<String, String>) {
        repository.record(EventEnvelopeParser.parse(record.value()))
    }

    fun stop() {
        running = false
        consumer.wakeup()
    }
}
