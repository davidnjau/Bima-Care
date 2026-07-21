package care.bima.patient.api

import kotlinx.serialization.Serializable

@Serializable
data class CreatePatientRequest(
    val nationalId: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val gender: String,
    val dob: String,
)

@Serializable
data class PatientResponse(
    val id: String,
    val nationalId: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val gender: String,
    val dob: String,
    val isActive: Boolean,
)
