package care.bima.consent.api

import care.bima.consent.clients.ReferenceValidationClient
import care.bima.consent.db.ConsentRepository
import care.bima.consent.domain.Consent
import care.bima.consent.domain.ConsentStatus
import care.bima.consent.events.ConsentEventPublisher
import care.bima.consent.fhir.ConsentFhirMapper
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

fun Routing.consentRoutes(
    repository: ConsentRepository,
    publisher: ConsentEventPublisher,
    referenceValidationClient: ReferenceValidationClient,
) {
    authenticate("keycloak") {
        route("/consents") {
            post {
                val request = call.receive<CreateConsentRequest>()
                val patientId = parseId(request.patientId, "patientId")
                val granteeId = parseId(request.granteeId, "granteeId")

                if (!referenceValidationClient.patientExists(patientId)) {
                    throw ValidationException("Patient $patientId not found")
                }

                val consent =
                    Consent(
                        id = UUID.randomUUID(),
                        patientId = patientId,
                        granteeId = granteeId,
                        scope = request.scope,
                        status = ConsentStatus.ACTIVE,
                        createdAt = LocalDateTime.now(),
                    )

                val created = repository.create(consent)
                publisher.publishConsentUpdated(created)
                call.respond(HttpStatusCode.Created, created.toResponse())
            }

            get("/{id}") {
                val id = parseId(call.parameters["id"], "id")
                val consent = repository.findById(id) ?: throw NotFoundException("Consent $id not found")
                call.respond(consent.toResponse())
            }

            get("/{id}/fhir") {
                val id = parseId(call.parameters["id"], "id")
                val consent = repository.findById(id) ?: throw NotFoundException("Consent $id not found")
                val fhirJson = FhirContextProvider.encodeToJson(ConsentFhirMapper.toFhir(consent))
                call.respondText(fhirJson, ContentType.Application.Json)
            }

            get {
                val patientId = call.request.queryParameters["patientId"]?.let { parseId(it, "patientId") }
                val consents = if (patientId != null) repository.findByPatientId(patientId) else emptyList()
                call.respond(consents.map { it.toResponse() })
            }

            get("/verify") {
                val patientId = parseId(call.request.queryParameters["patientId"], "patientId")
                val granteeId = parseId(call.request.queryParameters["granteeId"], "granteeId")
                val active = repository.findActiveConsent(patientId, granteeId) != null
                call.respond(ConsentVerifyResult(active = active))
            }

            post("/{id}/revoke") {
                val id = parseId(call.parameters["id"], "id")
                val revoked = repository.revoke(id) ?: throw NotFoundException("Consent $id not found")
                publisher.publishConsentUpdated(revoked)
                call.respond(revoked.toResponse())
            }
        }
    }
}

private fun parseId(
    raw: String?,
    field: String,
): UUID = runCatching { UUID.fromString(raw) }.getOrElse { throw ValidationException("Invalid $field: $raw") }

private fun Consent.toResponse() =
    ConsentResponse(
        id = id.toString(),
        patientId = patientId.toString(),
        granteeId = granteeId.toString(),
        scope = scope,
        status = status.name,
        createdAt = createdAt.toString(),
    )
