package care.bima.document.domain

import java.time.LocalDateTime
import java.util.UUID

enum class DocumentStatus { CURRENT, SUPERSEDED, ENTERED_IN_ERROR }

data class Document(
    val id: UUID,
    val patientId: UUID,
    val contentType: String,
    val title: String,
    val category: String,
    val storageKey: String,
    val status: DocumentStatus,
    val createdAt: LocalDateTime,
)
