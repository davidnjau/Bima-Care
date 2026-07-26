package care.bima.audit.db

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

data class AuditRecord(
    val id: UUID,
    val eventType: String,
    val resourceId: String,
    val version: Int,
    val occurredAt: String,
    val payload: String,
)

class AuditEventRepository {
    fun createSchema() =
        transaction {
            SchemaUtils.create(AuditEventsTable)
        }

    // insertIgnore: redelivered Kafka records share the same eventId (our PK), so a
    // duplicate poll is a silent no-op rather than a constraint-violation crash.
    fun record(event: AuditRecord) =
        transaction {
            AuditEventsTable.insertIgnore {
                it[id] = event.id
                it[eventType] = event.eventType
                it[resourceId] = event.resourceId
                it[version] = event.version
                it[occurredAt] = event.occurredAt
                it[payload] = event.payload
            }
        }

    fun findById(id: UUID): AuditRecord? =
        transaction {
            AuditEventsTable.selectAll().where { AuditEventsTable.id eq id }
                .map { it.toAuditRecord() }
                .singleOrNull()
        }

    fun findAll(resourceId: String?): List<AuditRecord> =
        transaction {
            val query = AuditEventsTable.selectAll()
            resourceId?.let { query.andWhere { AuditEventsTable.resourceId eq it } }
            query.map { it.toAuditRecord() }
        }

    private fun ResultRow.toAuditRecord() =
        AuditRecord(
            id = this[AuditEventsTable.id],
            eventType = this[AuditEventsTable.eventType],
            resourceId = this[AuditEventsTable.resourceId],
            version = this[AuditEventsTable.version],
            occurredAt = this[AuditEventsTable.occurredAt],
            payload = this[AuditEventsTable.payload],
        )
}
