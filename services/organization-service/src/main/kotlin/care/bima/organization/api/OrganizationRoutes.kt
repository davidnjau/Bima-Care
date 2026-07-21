package care.bima.organization.api

import care.bima.organization.db.OrganizationRepository
import care.bima.organization.domain.Organization
import care.bima.organization.domain.OrganizationType
import care.bima.organization.fhir.OrganizationFhirMapper
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

fun Routing.organizationRoutes(repository: OrganizationRepository) {
    authenticate("keycloak") {
        route("/organizations") {
            post {
                val request = call.receive<CreateOrganizationRequest>()
                val type =
                    runCatching { OrganizationType.valueOf(request.type.uppercase()) }
                        .getOrElse { throw ValidationException("Invalid organization type: ${request.type}") }

                val organization =
                    Organization(
                        id = UUID.randomUUID(),
                        registrationNumber = request.registrationNumber,
                        name = request.name,
                        type = type,
                        phone = request.phone,
                        address = request.address,
                    )
                val created = repository.create(organization)
                call.respond(HttpStatusCode.Created, created.toResponse())
            }

            get("/{id}") {
                val id = parseId(call.parameters["id"])
                val organization = repository.findById(id) ?: throw NotFoundException("Organization $id not found")
                call.respond(organization.toResponse())
            }

            get("/{id}/fhir") {
                val id = parseId(call.parameters["id"])
                val organization = repository.findById(id) ?: throw NotFoundException("Organization $id not found")
                val fhirJson = FhirContextProvider.encodeToJson(OrganizationFhirMapper.toFhir(organization))
                call.respondText(fhirJson, ContentType.Application.Json)
            }

            get {
                call.respond(repository.findAll().map { it.toResponse() })
            }
        }
    }
}

private fun parseId(raw: String?): UUID =
    runCatching { UUID.fromString(raw) }.getOrElse { throw ValidationException("Invalid organization id: $raw") }

private fun Organization.toResponse() =
    OrganizationResponse(
        id = id.toString(),
        registrationNumber = registrationNumber,
        name = name,
        type = type.name,
        phone = phone,
        address = address,
        isActive = isActive,
    )
