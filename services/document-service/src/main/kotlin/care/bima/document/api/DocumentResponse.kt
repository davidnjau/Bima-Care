package care.bima.document.api

import kotlinx.serialization.Serializable

@Serializable
data class DocumentResponse(
    val id: String,
    val patientId: String,
    val contentType: String,
    val title: String,
    val category: String,
    val status: String,
    val createdAt: String,
)
