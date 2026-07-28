package care.bima.encounter.api

import care.bima.encounter.clients.ReferenceValidationClient
import care.bima.encounter.db.EncounterRepository
import care.bima.encounter.domain.Encounter
import care.bima.encounter.domain.EncounterStatus
import care.bima.encounter.events.EncounterEventPublisher
import care.bima.encounter.fhir.EncounterFhirMapper
import care.bima.shared.fhir.FhirContextProvider
import care.bima.shared.service.NotFoundException
import care.bima.shared.service.ValidationException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.LocalDateTime
import java.util.UUID

fun Routing.encounterRoutes(
    repository: EncounterRepository,
    publisher: EncounterEventPublisher,
    referenceValidationClient: ReferenceValidationClient,
) {
    authenticate("keycloak") {
        route("/encounters") {
            post {
                val request = call.receive<CreateEncounterRequest>()
                val patientId = parseId(request.patientId, "patientId")
                val practitionerId = request.practitionerId?.let { parseId(it, "practitionerId") }
                val organizationId = parseId(request.organizationId, "organizationId")
                val status =
                    runCatching { EncounterStatus.valueOf(request.status.uppercase()) }
                        .getOrElse { throw ValidationException("Invalid encounter status: ${request.status}") }

                if (!referenceValidationClient.patientExists(patientId)) {
                    throw ValidationException("Patient $patientId not found")
                }
                if (practitionerId != null && !referenceValidationClient.practitionerExists(practitionerId)) {
                    throw ValidationException("Practitioner $practitionerId not found")
                }
                if (!referenceValidationClient.organizationExists(organizationId)) {
                    throw ValidationException("Organization $organizationId not found")
                }

                val encounter =
                    Encounter(
                        id = UUID.randomUUID(),
                        patientId = patientId,
                        practitionerId = practitionerId,
                        organizationId = organizationId,
                        status = status,
                        startedAt = LocalDateTime.now(),
                        endedAt = null,
                    )

                val created = repository.create(encounter)
                publisher.publishEncounterStarted(created)
                call.respond(HttpStatusCode.Created, created.toResponse())
            }

            get("/{id}") {
                val id = parseId(call.parameters["id"], "id")
                val encounter = repository.findById(id) ?: throw NotFoundException("Encounter $id not found")
                call.respond(encounter.toResponse())
            }

            get("/{id}/fhir") {
                val id = parseId(call.parameters["id"], "id")
                val encounter = repository.findById(id) ?: throw NotFoundException("Encounter $id not found")
                val fhirJson = FhirContextProvider.encodeToJson(EncounterFhirMapper.toFhir(encounter))
                call.respondText(fhirJson, ContentType.Application.Json)
            }

            get {
                val patientId = call.request.queryParameters["patientId"]
                val encounters =
                    if (patientId != null) {
                        repository.findByPatientId(parseId(patientId, "patientId"))
                    } else {
                        repository.findAll()
                    }
                call.respond(encounters.map { it.toResponse() })
            }
        }
    }
}

private fun parseId(
    raw: String?,
    field: String,
): UUID = runCatching { UUID.fromString(raw) }.getOrElse { throw ValidationException("Invalid $field: $raw") }

private fun Encounter.toResponse() =
    EncounterResponse(
        id = id.toString(),
        patientId = patientId.toString(),
        practitionerId = practitionerId?.toString(),
        organizationId = organizationId.toString(),
        status = status.name,
        startedAt = startedAt.toString(),
        endedAt = endedAt?.toString(),
    )
