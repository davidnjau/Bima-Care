package care.bima.provider.api

import care.bima.provider.db.PractitionerRepository
import care.bima.provider.domain.Practitioner
import care.bima.provider.events.PractitionerEventPublisher
import care.bima.provider.fhir.PractitionerFhirMapper
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
import java.util.UUID

fun Routing.practitionerRoutes(
    repository: PractitionerRepository,
    publisher: PractitionerEventPublisher,
) {
    authenticate("keycloak") {
        route("/practitioners") {
            post {
                val request = call.receive<CreatePractitionerRequest>()
                val practitioner =
                    Practitioner(
                        id = UUID.randomUUID(),
                        licenseNumber = request.licenseNumber,
                        firstName = request.firstName,
                        lastName = request.lastName,
                        phone = request.phone,
                        specialty = request.specialty,
                    )
                val created = repository.create(practitioner)
                publisher.publishPractitionerCreated(created)
                call.respond(HttpStatusCode.Created, created.toResponse())
            }

            get("/{id}") {
                val id = parseId(call.parameters["id"])
                val practitioner = repository.findById(id) ?: throw NotFoundException("Practitioner $id not found")
                call.respond(practitioner.toResponse())
            }

            get("/{id}/fhir") {
                val id = parseId(call.parameters["id"])
                val practitioner = repository.findById(id) ?: throw NotFoundException("Practitioner $id not found")
                val fhirJson = FhirContextProvider.encodeToJson(PractitionerFhirMapper.toFhir(practitioner))
                call.respondText(fhirJson, ContentType.Application.Json)
            }

            get {
                call.respond(repository.findAll().map { it.toResponse() })
            }
        }
    }
}

private fun parseId(raw: String?): UUID =
    runCatching { UUID.fromString(raw) }.getOrElse { throw ValidationException("Invalid practitioner id: $raw") }

private fun Practitioner.toResponse() =
    PractitionerResponse(
        id = id.toString(),
        licenseNumber = licenseNumber,
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        specialty = specialty,
        isActive = isActive,
    )
