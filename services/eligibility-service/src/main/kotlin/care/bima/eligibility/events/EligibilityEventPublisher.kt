package care.bima.eligibility.events

import care.bima.eligibility.domain.Coverage
import care.bima.eligibility.domain.Policy
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

class EligibilityEventPublisher(bootstrapServers: String) {
    private val producer =
        KafkaProducer<String, String>(
            Properties().apply {
                put("bootstrap.servers", bootstrapServers)
                put("key.serializer", StringSerializer::class.java.name)
                put("value.serializer", StringSerializer::class.java.name)
            },
        )

    fun publishCoverageVerified(coverage: Coverage) {
        val envelope =
            EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = Topics.COVERAGE_VERIFIED,
                resourceId = coverage.id.toString(),
                version = 1,
                occurredAt = Instant.now().toString(),
                payload = coverage.patientId.toString(),
            )
        val json = Json.encodeToString(EventEnvelope.serializer(String.serializer()), envelope)
        producer.send(ProducerRecord(Topics.COVERAGE_VERIFIED, coverage.patientId.toString(), json))
    }

    fun publishPolicyCreated(policy: Policy) {
        val envelope =
            EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = Topics.POLICY_CREATED,
                resourceId = policy.id.toString(),
                version = 1,
                occurredAt = Instant.now().toString(),
                payload = policy.policyNumber,
            )
        val json = Json.encodeToString(EventEnvelope.serializer(String.serializer()), envelope)
        producer.send(ProducerRecord(Topics.POLICY_CREATED, policy.id.toString(), json))
    }

    fun close() = producer.close()
}
