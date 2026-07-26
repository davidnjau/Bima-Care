package care.bima.consent.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateConsentRequest(
    val patientId: String,
    val granteeId: String,
    val scope: String,
)

@Serializable
data class ConsentResponse(
    val id: String,
    val patientId: String,
    val granteeId: String,
    val scope: String,
    val status: String,
    val createdAt: String,
)

@Serializable
data class ConsentVerifyResult(
    val active: Boolean,
)
