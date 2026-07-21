package care.bima.provider.api

import kotlinx.serialization.Serializable

@Serializable
data class CreatePractitionerRequest(
    val licenseNumber: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val specialty: String,
)

@Serializable
data class PractitionerResponse(
    val id: String,
    val licenseNumber: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val specialty: String,
    val isActive: Boolean,
)
