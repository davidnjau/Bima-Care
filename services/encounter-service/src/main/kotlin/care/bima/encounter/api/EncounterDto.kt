package care.bima.encounter.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateEncounterRequest(
    val patientId: String,
    val practitionerId: String,
    val organizationId: String,
    val status: String = "IN_PROGRESS",
)

@Serializable
data class EncounterResponse(
    val id: String,
    val patientId: String,
    val practitionerId: String,
    val organizationId: String,
    val status: String,
    val startedAt: String,
    val endedAt: String?,
)
