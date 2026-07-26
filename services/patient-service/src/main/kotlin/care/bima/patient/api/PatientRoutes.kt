package care.bima.patient.api

import care.bima.patient.db.PatientRepository
import care.bima.patient.domain.Gender
import care.bima.patient.domain.Patient
import care.bima.patient.events.PatientEventPublisher
import care.bima.patient.fhir.PatientFhirMapper
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
import java.time.LocalDate
import java.util.UUID

fun Routing.patientRoutes(
    repository: PatientRepository,
    publisher: PatientEventPublisher,
) {
    authenticate("keycloak") {
        route("/patients") {
            post {
                val request = call.receive<CreatePatientRequest>()
                val existing = repository.findByNationalId(request.nationalId)
                if (existing != null) {
                    respondExistingOrConflict(call, existing, request.id)
                    return@post
                }

                val patient = request.toNewPatient()
                val created = repository.create(patient)
                publisher.publishPatientCreated(created)
                call.respond(HttpStatusCode.Created, created.toResponse())
            }

            get("/{id}") {
                val id = parseId(call.parameters["id"])
                val patient = repository.findById(id) ?: throw NotFoundException("Patient $id not found")
                call.respond(patient.toResponse())
            }

            get("/{id}/fhir") {
                val id = parseId(call.parameters["id"])
                val patient = repository.findById(id) ?: throw NotFoundException("Patient $id not found")
                val fhirJson = FhirContextProvider.encodeToJson(PatientFhirMapper.toFhir(patient))
                call.respondText(fhirJson, ContentType.Application.Json)
            }

            get {
                call.respond(repository.findAll().map { it.toResponse() })
            }
        }
    }
}

private fun parseId(raw: String?): UUID =
    runCatching { UUID.fromString(raw) }.getOrElse { throw ValidationException("Invalid patient id: $raw") }

// Only a request that supplies the SAME pre-generated id as the existing record is a safe replay
// of a prior offline sync (see Workstream C) - otherwise this is a genuine conflict (two different
// registrations sharing a nationalId), even if no id was supplied at all. Silently returning the
// existing record for an id-less request would mask real data-entry conflicts.
private suspend fun respondExistingOrConflict(
    call: ApplicationCall,
    existing: Patient,
    requestedId: String?,
) {
    val requestedUuid = requestedId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    if (requestedUuid != null && requestedUuid == existing.id) {
        call.respond(HttpStatusCode.OK, existing.toResponse())
        return
    }
    throw ConflictException("A patient with this nationalId already exists")
}

private fun CreatePatientRequest.toNewPatient(): Patient {
    val gender =
        runCatching { Gender.valueOf(gender.uppercase()) }
            .getOrElse { throw ValidationException("Invalid gender: $gender") }
    val parsedDob =
        runCatching { LocalDate.parse(dob) }
            .getOrElse { throw ValidationException("Invalid dob, expected ISO-8601: $dob") }
    val patientId =
        id?.let { runCatching { UUID.fromString(it) }.getOrElse { throw ValidationException("Invalid id: $it") } }
            ?: UUID.randomUUID()

    return Patient(
        id = patientId,
        nationalId = nationalId,
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        gender = gender,
        dob = parsedDob,
    )
}

private fun Patient.toResponse() =
    PatientResponse(
        id = id.toString(),
        nationalId = nationalId,
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        gender = gender.name,
        dob = dob.toString(),
        isActive = isActive,
    )
