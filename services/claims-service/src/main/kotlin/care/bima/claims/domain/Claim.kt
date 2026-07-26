package care.bima.claims.domain

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class ClaimStatus { SUBMITTED, APPROVED, PARTIALLY_APPROVED, REJECTED }

data class Claim(
    val id: UUID,
    val patientId: UUID,
    val encounterId: UUID,
    val coverageId: UUID,
    val practitionerId: UUID,
    val organizationId: UUID,
    val serviceType: String,
    val diagnosisCode: String,
    val treatmentDetails: String,
    val requestedAmount: BigDecimal,
    val approvedAmount: BigDecimal?,
    val status: ClaimStatus,
    val submittedAt: LocalDateTime,
    val adjudicatedAt: LocalDateTime?,
)
