package care.bima.eligibility.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

enum class PolicyType { FAMILY, INDIVIDUAL, CORPORATE }

enum class PolicyStatus { ACTIVE, SUSPENDED, EXPIRED }

data class Policy(
    val id: UUID,
    val insurerId: UUID,
    val policyNumber: String,
    val name: String,
    val type: PolicyType,
    val premium: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val status: PolicyStatus,
)
