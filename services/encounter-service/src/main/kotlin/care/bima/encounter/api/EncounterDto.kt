package care.bima.encounter.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateEncounterRequest(
    val patientId: String,
    // Null for a member-reported reimbursement encounter, where there's no linkage from a
    // Practitioner to the Organization the member names, so no real practitionerId exists.
    val practitionerId: String? = null,
    val organizationId: String,
    val status: String = "IN_PROGRESS",
)

@Serializable
data class EncounterResponse(
    val id: String,
    val patientId: String,
    val practitionerId: String?,
    val organizationId: String,
    val status: String,
    val startedAt: String,
    val endedAt: String?,
)
