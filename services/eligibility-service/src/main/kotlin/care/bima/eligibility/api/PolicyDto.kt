package care.bima.eligibility.api

import kotlinx.serialization.Serializable

// No policyNumber here - it's server-generated, not user-supplied.
@Serializable
data class CreatePolicyRequest(
    val name: String,
    val type: String,
    val premium: String,
    val startDate: String,
    val endDate: String? = null,
)

@Serializable
data class UpdatePolicyStatusRequest(
    val status: String,
)

@Serializable
data class PolicyResponse(
    val id: String,
    val insurerId: String,
    val policyNumber: String,
    val name: String,
    val type: String,
    val premium: String,
    val startDate: String,
    val endDate: String?,
    val status: String,
)
