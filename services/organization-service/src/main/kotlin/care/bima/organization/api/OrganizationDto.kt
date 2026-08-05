package care.bima.organization.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrganizationRequest(
    val registrationNumber: String,
    val name: String,
    val type: String,
    val phone: String,
    // Optional - lets provisioning email a real temp-password delivery instead of only
    // logging it server-side.
    val email: String? = null,
    val address: String,
)

@Serializable
data class OrganizationResponse(
    val id: String,
    val registrationNumber: String,
    val name: String,
    val type: String,
    val phone: String,
    val email: String?,
    val address: String,
    val isActive: Boolean,
)
