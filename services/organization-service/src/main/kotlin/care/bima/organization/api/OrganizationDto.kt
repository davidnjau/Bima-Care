package care.bima.organization.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrganizationRequest(
    val registrationNumber: String,
    val name: String,
    val type: String,
    val phone: String,
    val address: String,
)

@Serializable
data class OrganizationResponse(
    val id: String,
    val registrationNumber: String,
    val name: String,
    val type: String,
    val phone: String,
    val address: String,
    val isActive: Boolean,
)
