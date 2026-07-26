package care.bima.audit.events

import care.bima.audit.db.AuditRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

// Parses only the common EventEnvelope shape (shared/events/EventEnvelope.kt) — audit-service
// deliberately never deserializes into a producer-specific payload type, so it stays decoupled
// from every other service's event schema. The payload is stored verbatim as raw JSON text.
object EventEnvelopeParser {
    fun parse(rawJson: String): AuditRecord {
        val envelope = Json.parseToJsonElement(rawJson).jsonObject
        return AuditRecord(
            id = UUID.fromString(envelope.getValue("eventId").jsonPrimitive.content),
            eventType = envelope.getValue("eventType").jsonPrimitive.content,
            resourceId = envelope.getValue("resourceId").jsonPrimitive.content,
            version = envelope.getValue("version").jsonPrimitive.content.toInt(),
            occurredAt = envelope.getValue("occurredAt").jsonPrimitive.content,
            payload = envelope["payload"]?.toString() ?: "null",
        )
    }
}
