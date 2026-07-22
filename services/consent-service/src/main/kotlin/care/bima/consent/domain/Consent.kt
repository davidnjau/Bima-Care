package care.bima.consent.domain

import java.time.LocalDateTime
import java.util.UUID

enum class ConsentStatus { ACTIVE, REVOKED }

data class Consent(
    val id: UUID,
    val patientId: UUID,
    val granteeId: UUID,
    val scope: String,
    val status: ConsentStatus,
    val createdAt: LocalDateTime,
)
