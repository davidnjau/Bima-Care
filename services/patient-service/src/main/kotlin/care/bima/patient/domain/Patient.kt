package care.bima.patient.domain

import java.time.LocalDate
import java.util.UUID

enum class Gender { MALE, FEMALE, OTHER, UNKNOWN }

data class Patient(
    val id: UUID,
    val nationalId: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String? = null,
    val gender: Gender,
    val dob: LocalDate,
    val isActive: Boolean = true,
)
