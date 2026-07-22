package care.bima.payments.api

import kotlinx.serialization.Serializable

@Serializable
data class PaymentResponse(
    val id: String,
    val claimId: String,
    val patientId: String,
    val amount: String,
    val releasedAt: String,
)
