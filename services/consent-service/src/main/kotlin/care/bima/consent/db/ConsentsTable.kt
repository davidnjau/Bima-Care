package care.bima.consent.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ConsentsTable : Table("consents") {
    val id = uuid("consent_id")
    val patientId = uuid("patient_id").index()
    val granteeId = uuid("grantee_id").index()
    val scope = varchar("scope", 128)
    val status = varchar("status", 32)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
