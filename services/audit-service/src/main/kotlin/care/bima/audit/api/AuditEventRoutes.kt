package care.bima.audit.api

import care.bima.audit.db.AuditEventRepository
import care.bima.audit.db.AuditRecord
import care.bima.audit.fhir.AuditEventFhirMapper
import care.bima.audit.fhir.ProvenanceFhirMapper
import care.bima.shared.fhir.FhirContextProvider
import care.bima.shared.service.NotFoundException
import care.bima.shared.service.ValidationException
import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.util.UUID

fun Routing.auditEventRoutes(repository: AuditEventRepository) {
    authenticate("keycloak") {
        route("/audit-events") {
            get("/{id}") {
                val id = parseId(call.parameters["id"])
                val record = repository.findById(id) ?: throw NotFoundException("Audit event $id not found")
                call.respond(record.toResponse())
            }

            get("/{id}/fhir") {
                val id = parseId(call.parameters["id"])
                val record = repository.findById(id) ?: throw NotFoundException("Audit event $id not found")
                val fhirJson = FhirContextProvider.encodeToJson(AuditEventFhirMapper.toFhir(record))
                call.respondText(fhirJson, ContentType.Application.Json)
            }

            get("/{id}/provenance") {
                val id = parseId(call.parameters["id"])
                val record = repository.findById(id) ?: throw NotFoundException("Audit event $id not found")
                val fhirJson = FhirContextProvider.encodeToJson(ProvenanceFhirMapper.toFhir(record))
                call.respondText(fhirJson, ContentType.Application.Json)
            }

            get {
                val resourceId = call.request.queryParameters["resourceId"]
                call.respond(repository.findAll(resourceId).map { it.toResponse() })
            }
        }
    }
}

private fun parseId(raw: String?): UUID =
    runCatching { UUID.fromString(raw) }.getOrElse { throw ValidationException("Invalid audit event id: $raw") }

private fun AuditRecord.toResponse() =
    AuditEventResponse(
        id = id.toString(),
        eventType = eventType,
        resourceId = resourceId,
        version = version,
        occurredAt = occurredAt,
        payload = payload,
    )
