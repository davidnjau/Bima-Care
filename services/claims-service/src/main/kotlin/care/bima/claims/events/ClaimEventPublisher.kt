package care.bima.claims.events

import care.bima.claims.domain.Claim
import care.bima.shared.events.ClaimAdjudicatedPayload
import care.bima.shared.events.ClaimSubmittedPayload
import care.bima.shared.events.EventEnvelope
import care.bima.shared.events.Topics
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Instant
import java.util.Properties
import java.util.UUID

class ClaimEventPublisher(bootstrapServers: String) {
    private val producer =
        KafkaProducer<String, String>(
            Properties().apply {
                put("bootstrap.servers", bootstrapServers)
                put("key.serializer", StringSerializer::class.java.name)
                put("value.serializer", StringSerializer::class.java.name)
            },
        )

    fun publishClaimSubmitted(claim: Claim) {
        val envelope =
            EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = Topics.CLAIM_SUBMITTED,
                resourceId = claim.id.toString(),
                version = 1,
                occurredAt = Instant.now().toString(),
                payload =
                    ClaimSubmittedPayload(
                        claimId = claim.id.toString(),
                        patientId = claim.patientId.toString(),
                        encounterId = claim.encounterId.toString(),
                        coverageId = claim.coverageId.toString(),
                        amount = claim.requestedAmount.toPlainString(),
                    ),
            )
        val json = Json.encodeToString(EventEnvelope.serializer(ClaimSubmittedPayload.serializer()), envelope)
        producer.send(ProducerRecord(Topics.CLAIM_SUBMITTED, claim.patientId.toString(), json))
    }

    fun publishClaimAdjudicated(claim: Claim) {
        val envelope =
            EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = Topics.CLAIM_ADJUDICATED,
                resourceId = claim.id.toString(),
                version = 1,
                occurredAt = Instant.now().toString(),
                payload =
                    ClaimAdjudicatedPayload(
                        claimId = claim.id.toString(),
                        patientId = claim.patientId.toString(),
                        status = claim.status.name,
                        approvedAmount = claim.approvedAmount?.toPlainString(),
                    ),
            )
        val json = Json.encodeToString(EventEnvelope.serializer(ClaimAdjudicatedPayload.serializer()), envelope)
        producer.send(ProducerRecord(Topics.CLAIM_ADJUDICATED, claim.patientId.toString(), json))
    }

    fun close() = producer.close()
}
