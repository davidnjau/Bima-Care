package care.bima.audit.api

import kotlinx.serialization.Serializable

@Serializable
data class AuditEventResponse(
    val id: String,
    val eventType: String,
    val resourceId: String,
    val version: Int,
    val occurredAt: String,
    val payload: String,
)
