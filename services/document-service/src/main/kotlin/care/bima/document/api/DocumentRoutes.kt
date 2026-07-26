package care.bima.document.api

import care.bima.document.clients.ReferenceValidationClient
import care.bima.document.db.DocumentRepository
import care.bima.document.domain.Document
import care.bima.document.domain.DocumentStatus
import care.bima.document.events.DocumentEventPublisher
import care.bima.document.fhir.DocumentReferenceFhirMapper
import care.bima.document.storage.ObjectStorageClient
import care.bima.shared.fhir.FhirContextProvider
import care.bima.shared.service.NotFoundException
import care.bima.shared.service.ValidationException
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.LocalDateTime
import java.util.UUID

class DocumentRouteDependencies(
    val repository: DocumentRepository,
    val publisher: DocumentEventPublisher,
    val referenceValidationClient: ReferenceValidationClient,
    val storageClient: ObjectStorageClient,
)

private class UploadForm(
    var patientId: String? = null,
    var title: String? = null,
    var category: String? = null,
    var contentType: String = "application/octet-stream",
    var bytes: ByteArray? = null,
)

fun Routing.documentRoutes(deps: DocumentRouteDependencies) {
    authenticate("keycloak") {
        route("/documents") {
            post { uploadDocument(call, deps) }

            get("/{id}") {
                val id = parseId(call.parameters["id"], "id")
                val document = deps.repository.findById(id) ?: throw NotFoundException("Document $id not found")
                call.respond(document.toResponse())
            }

            get("/{id}/fhir") {
                val id = parseId(call.parameters["id"], "id")
                val document = deps.repository.findById(id) ?: throw NotFoundException("Document $id not found")
                val fhirJson = FhirContextProvider.encodeToJson(DocumentReferenceFhirMapper.toFhir(document))
                call.respondText(fhirJson, ContentType.Application.Json)
            }

            get("/{id}/content") {
                val id = parseId(call.parameters["id"], "id")
                val document = deps.repository.findById(id) ?: throw NotFoundException("Document $id not found")
                val bytes = deps.storageClient.download(document.storageKey)
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.FileName, document.title)
                        .toString(),
                )
                call.respondBytes(bytes, ContentType.parse(document.contentType))
            }

            get {
                val patientId = call.request.queryParameters["patientId"]?.let { parseId(it, "patientId") }
                val documents =
                    if (patientId != null) deps.repository.findByPatientId(patientId) else emptyList()
                call.respond(documents.map { it.toResponse() })
            }
        }
    }
}

private suspend fun uploadDocument(
    call: ApplicationCall,
    deps: DocumentRouteDependencies,
) {
    val form = UploadForm()
    call.receiveMultipart().forEachPart { part ->
        when (part) {
            is PartData.FormItem -> {
                when (part.name) {
                    "patientId" -> form.patientId = part.value
                    "title" -> form.title = part.value
                    "category" -> form.category = part.value
                }
            }
            is PartData.FileItem -> {
                form.contentType = part.contentType?.toString() ?: form.contentType
                form.bytes = part.streamProvider().readBytes()
            }
            else -> {}
        }
        part.dispose()
    }

    val patientId = parseId(form.patientId, "patientId")
    val title = form.title ?: throw ValidationException("Missing title")
    val category = form.category ?: throw ValidationException("Missing category")
    val bytes = form.bytes ?: throw ValidationException("Missing file content")

    if (!deps.referenceValidationClient.patientExists(patientId)) {
        throw ValidationException("Patient $patientId not found")
    }

    val id = UUID.randomUUID()
    val storageKey = "$patientId/$id"
    deps.storageClient.upload(storageKey, form.contentType, bytes)

    val document =
        Document(
            id = id,
            patientId = patientId,
            contentType = form.contentType,
            title = title,
            category = category,
            storageKey = storageKey,
            status = DocumentStatus.CURRENT,
            createdAt = LocalDateTime.now(),
        )

    val created = deps.repository.create(document)
    deps.publisher.publishDocumentUploaded(created)
    call.respond(HttpStatusCode.Created, created.toResponse())
}

private fun parseId(
    raw: String?,
    field: String,
): UUID = runCatching { UUID.fromString(raw) }.getOrElse { throw ValidationException("Invalid $field: $raw") }

private fun Document.toResponse() =
    DocumentResponse(
        id = id.toString(),
        patientId = patientId.toString(),
        contentType = contentType,
        title = title,
        category = category,
        status = status.name,
        createdAt = createdAt.toString(),
    )
