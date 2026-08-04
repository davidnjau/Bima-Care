package care.bima.patient.api

import care.bima.patient.db.DependentRepository
import care.bima.patient.db.PatientRepository
import care.bima.patient.domain.Dependent
import care.bima.patient.domain.RelationshipType
import care.bima.patient.fhir.RelatedPersonFhirMapper
import care.bima.shared.fhir.FhirContextProvider
import care.bima.shared.service.ConflictException
import care.bima.shared.service.NotFoundException
import care.bima.shared.service.ValidationException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
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

fun Routing.dependentRoutes(
    patientRepository: PatientRepository,
    dependentRepository: DependentRepository,
) {
    authenticate("keycloak") {
        route("/patients/{id}/dependents") {
            post { createDependent(call, patientRepository, dependentRepository) }

            get {
                val primaryPatientId = parseId(call.parameters["id"], "id")
                call.respond(dependentRepository.findByPrimaryPatientId(primaryPatientId).map { it.toResponse() })
            }

            get("/{depId}/fhir") {
                val depId = parseId(call.parameters["depId"], "depId")
                val dependent =
                    dependentRepository.findById(depId) ?: throw NotFoundException("Dependent $depId not found")
                val fhirJson = FhirContextProvider.encodeToJson(RelatedPersonFhirMapper.toFhir(dependent))
                call.respondText(fhirJson, ContentType.Application.Json)
            }
        }
    }
}

private suspend fun createDependent(
    call: ApplicationCall,
    patientRepository: PatientRepository,
    dependentRepository: DependentRepository,
) {
    val primaryPatientId = parseId(call.parameters["id"], "id")
    patientRepository.findById(primaryPatientId) ?: throw NotFoundException("Patient $primaryPatientId not found")

    val request = call.receive<CreateDependentRequest>()
    val dependentPatientId = parseId(request.dependentPatientId, "dependentPatientId")
    patientRepository.findById(dependentPatientId)
        ?: throw ValidationException("Dependent patient $dependentPatientId not found")
    if (dependentRepository.findLink(primaryPatientId, dependentPatientId) != null) {
        throw ConflictException("This dependent is already linked to this member")
    }
    val relationship =
        runCatching { RelationshipType.valueOf(request.relationship.uppercase()) }
            .getOrElse { throw ValidationException("Invalid relationship: ${request.relationship}") }

    val dependent =
        Dependent(
            id = UUID.randomUUID(),
            primaryPatientId = primaryPatientId,
            dependentPatientId = dependentPatientId,
            relationship = relationship,
        )
    val created = dependentRepository.create(dependent)
    call.respond(HttpStatusCode.Created, created.toResponse())
}

private fun parseId(
    raw: String?,
    field: String,
): UUID = runCatching { UUID.fromString(raw) }.getOrElse { throw ValidationException("Invalid $field: $raw") }

private fun Dependent.toResponse() =
    DependentResponse(
        id = id.toString(),
        primaryPatientId = primaryPatientId.toString(),
        dependentPatientId = dependentPatientId.toString(),
        relationship = relationship.name,
    )
