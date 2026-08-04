package care.bima.patient.domain

import java.util.UUID

enum class RelationshipType { SPOUSE, CHILD, OTHER }

data class Dependent(
    val id: UUID,
    val primaryPatientId: UUID,
    val dependentPatientId: UUID,
    val relationship: RelationshipType,
)
