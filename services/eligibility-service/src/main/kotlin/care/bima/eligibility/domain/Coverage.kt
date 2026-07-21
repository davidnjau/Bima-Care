package care.bima.eligibility.domain

import java.time.LocalDate
import java.util.UUID

enum class CoverageStatus { ACTIVE, CANCELLED, DRAFT, ENTERED_IN_ERROR }

data class Coverage(
    val id: UUID,
    val patientId: UUID,
    val insurerId: UUID,
    val status: CoverageStatus,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val planTier: String,
)
