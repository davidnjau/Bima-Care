package care.bima.organization.domain

import java.util.UUID

enum class OrganizationType { HOSPITAL, CLINIC, INSURER, LAB, PHARMACY }

data class Organization(
    val id: UUID,
    val registrationNumber: String,
    val name: String,
    val type: OrganizationType,
    val phone: String,
    val email: String? = null,
    val address: String,
    val isActive: Boolean = true,
)
