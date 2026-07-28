package care.bima.claims.api

import kotlinx.serialization.Serializable

@Serializable
data class SubmitClaimRequest(
    val patientId: String,
    val serviceType: String,
    val diagnosisCode: String,
    val treatmentDetails: String,
    val amount: String,
)

// No patientId here - it's resolved from the caller's JWT (patientId claim), so a member
// can never submit a reimbursement claim on someone else's behalf.
@Serializable
data class SubmitReimbursementClaimRequest(
    val organizationId: String,
    val serviceType: String,
    val diagnosisCode: String,
    val treatmentDetails: String,
    val amount: String,
    val dateOfService: String,
    val claimFormDocumentId: String,
    val itemizedReceiptDocumentId: String,
    val etrDocumentId: String,
)

@Serializable
data class AdjudicateClaimRequest(
    val decision: String,
    val approvedAmount: String? = null,
)

@Serializable
data class ClaimResponse(
    val id: String,
    val patientId: String,
    val encounterId: String,
    val coverageId: String,
    val practitionerId: String?,
    val organizationId: String,
    val serviceType: String,
    val diagnosisCode: String,
    val treatmentDetails: String,
    val requestedAmount: String,
    val approvedAmount: String?,
    val status: String,
    val claimType: String,
    val dateOfService: String?,
    val claimFormDocumentId: String?,
    val itemizedReceiptDocumentId: String?,
    val etrDocumentId: String?,
    val submittedAt: String,
    val adjudicatedAt: String?,
)
