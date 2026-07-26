package care.bima.document.db

import care.bima.document.domain.Document
import care.bima.document.domain.DocumentStatus
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class DocumentRepository {
    fun createSchema() =
        transaction {
            SchemaUtils.create(DocumentsTable)
        }

    fun create(document: Document): Document =
        transaction {
            DocumentsTable.insert {
                it[id] = document.id
                it[patientId] = document.patientId
                it[contentType] = document.contentType
                it[title] = document.title
                it[category] = document.category
                it[storageKey] = document.storageKey
                it[status] = document.status.name
                it[createdAt] = document.createdAt
            }
            document
        }

    fun findById(id: UUID): Document? =
        transaction {
            DocumentsTable.selectAll().where { DocumentsTable.id eq id }
                .map { it.toDocument() }
                .singleOrNull()
        }

    fun findByPatientId(patientId: UUID): List<Document> =
        transaction {
            DocumentsTable.selectAll().where { DocumentsTable.patientId eq patientId }
                .map { it.toDocument() }
        }

    private fun ResultRow.toDocument() =
        Document(
            id = this[DocumentsTable.id],
            patientId = this[DocumentsTable.patientId],
            contentType = this[DocumentsTable.contentType],
            title = this[DocumentsTable.title],
            category = this[DocumentsTable.category],
            storageKey = this[DocumentsTable.storageKey],
            status = DocumentStatus.valueOf(this[DocumentsTable.status]),
            createdAt = this[DocumentsTable.createdAt],
        )
}
