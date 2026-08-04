package care.bima.patient.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateDependentRequest(
    val dependentPatientId: String,
    val relationship: String,
)

@Serializable
data class DependentResponse(
    val id: String,
    val primaryPatientId: String,
    val dependentPatientId: String,
    val relationship: String,
)
