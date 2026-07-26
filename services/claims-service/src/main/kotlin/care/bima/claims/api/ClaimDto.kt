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
    val practitionerId: String,
    val organizationId: String,
    val serviceType: String,
    val diagnosisCode: String,
    val treatmentDetails: String,
    val requestedAmount: String,
    val approvedAmount: String?,
    val status: String,
    val submittedAt: String,
    val adjudicatedAt: String?,
)
