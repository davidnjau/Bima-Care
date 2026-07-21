package care.bima.eligibility.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateCoverageRequest(
    val patientId: String,
    val insurerId: String,
    val status: String,
    val startDate: String,
    val endDate: String? = null,
    val planTier: String,
)

@Serializable
data class CoverageResponse(
    val id: String,
    val patientId: String,
    val insurerId: String,
    val status: String,
    val startDate: String,
    val endDate: String?,
    val planTier: String,
)

@Serializable
data class EligibilityResult(
    val eligible: Boolean,
    val coverage: CoverageResponse? = null,
)
