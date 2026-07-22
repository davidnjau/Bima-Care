package care.bima.document.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object DocumentsTable : Table("documents") {
    val id = uuid("document_id")
    val patientId = uuid("patient_id").index()
    val contentType = varchar("content_type", 128)
    val title = varchar("title", 256)
    val category = varchar("category", 64)
    val storageKey = varchar("storage_key", 512)
    val status = varchar("status", 32)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
