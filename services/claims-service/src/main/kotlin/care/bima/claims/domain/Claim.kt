package care.bima.claims.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class ClaimStatus { SUBMITTED, APPROVED, PARTIALLY_APPROVED, REJECTED }

// PROVIDER_SUBMITTED: a provider submitted this on behalf of a patient they just saw, tied to
// a real Practitioner. MEMBER_REIMBURSEMENT: the member paid cash and is self-reporting the
// visit for a refund - there's no linked Practitioner, only the Organization they name and the
// claim form/itemised receipt/ETR they upload as evidence.
enum class ClaimType { PROVIDER_SUBMITTED, MEMBER_REIMBURSEMENT }

data class Claim(
    val id: UUID,
    val patientId: UUID,
    val encounterId: UUID,
    val coverageId: UUID,
    val practitionerId: UUID?,
    val organizationId: UUID,
    val serviceType: String,
    val diagnosisCode: String,
    val treatmentDetails: String,
    val requestedAmount: BigDecimal,
    val approvedAmount: BigDecimal?,
    val status: ClaimStatus,
    val claimType: ClaimType,
    // Only ever set for MEMBER_REIMBURSEMENT claims - provider-submitted claims are same-day,
    // so submittedAt already serves that purpose for them.
    val dateOfService: LocalDate?,
    val claimFormDocumentId: UUID?,
    val itemizedReceiptDocumentId: UUID?,
    val etrDocumentId: UUID?,
    val submittedAt: LocalDateTime,
    val adjudicatedAt: LocalDateTime?,
)
