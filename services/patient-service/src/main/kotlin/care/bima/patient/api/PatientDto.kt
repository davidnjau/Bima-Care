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
    // Lets an offline client (see Workstream C) pre-generate the patient's id before it has
    // connectivity, so a retried sync of the same request is a safe no-op rather than a duplicate.
    val id: String? = null,
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
