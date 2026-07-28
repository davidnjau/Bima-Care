package care.bima.encounter.domain

import java.time.LocalDateTime
import java.util.UUID

enum class EncounterStatus { IN_PROGRESS, FINISHED, CANCELLED }

data class Encounter(
    val id: UUID,
    val patientId: UUID,
    val practitionerId: UUID?,
    val organizationId: UUID,
    val status: EncounterStatus,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
)
