package care.bima.provider.domain

import java.util.UUID

data class Practitioner(
    val id: UUID,
    val licenseNumber: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val specialty: String,
    val isActive: Boolean = true,
)
