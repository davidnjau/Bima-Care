package care.bima.shared.events

import kotlinx.serialization.Serializable

@Serializable
data class EncounterStartedPayload(
    val encounterId: String,
    val patientId: String,
    val practitionerId: String,
    val organizationId: String,
)

@Serializable
data class ClaimSubmittedPayload(
    val claimId: String,
    val patientId: String,
    val encounterId: String,
    val coverageId: String,
    val amount: String,
)

@Serializable
data class ClaimAdjudicatedPayload(
    val claimId: String,
    val patientId: String,
    val status: String,
    val approvedAmount: String?,
)

@Serializable
data class PaymentReleasedPayload(
    val paymentId: String,
    val claimId: String,
    val patientId: String,
    val amount: String,
)
