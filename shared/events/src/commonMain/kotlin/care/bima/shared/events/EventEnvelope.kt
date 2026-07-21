package care.bima.shared.events

import kotlinx.serialization.Serializable

@Serializable
data class EventEnvelope<T>(
    val eventId: String,
    val eventType: String,
    val resourceId: String,
    val version: Int,
    val occurredAt: String,
    val payload: T,
)
