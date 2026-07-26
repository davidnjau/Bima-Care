package care.bima.iam.api

import kotlinx.serialization.Serializable

@Serializable
data class IdentityResponse(
    val subject: String,
    val roles: List<String>,
    val patientId: String?,
)
