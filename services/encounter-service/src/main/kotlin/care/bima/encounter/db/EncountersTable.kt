package care.bima.encounter.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object EncountersTable : Table("encounters") {
    val id = uuid("encounter_id")
    val patientId = uuid("patient_id").index()
    val practitionerId = uuid("practitioner_id").index()
    val organizationId = uuid("organization_id").index()
    val status = varchar("status", 32)
    val startedAt = datetime("started_at")
    val endedAt = datetime("ended_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
