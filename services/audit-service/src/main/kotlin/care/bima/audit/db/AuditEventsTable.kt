package care.bima.audit.db

import org.jetbrains.exposed.sql.Table

object AuditEventsTable : Table("audit_events") {
    val id = uuid("id")
    val eventType = varchar("event_type", 128)
    val resourceId = varchar("resource_id", 128)
    val version = integer("version")
    val occurredAt = varchar("occurred_at", 64)
    val payload = text("payload")

    override val primaryKey = PrimaryKey(id)
}
