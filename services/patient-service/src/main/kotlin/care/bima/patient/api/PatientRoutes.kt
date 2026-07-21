package care.bima.patient.api

import care.bima.patient.db.PatientRepository
import care.bima.patient.domain.Gender
import care.bima.patient.domain.Patient
import care.bima.patient.events.PatientEventPublisher
import care.bima.patient.fhir.PatientFhirMapper
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
                val gender =
                    runCatching { Gender.valueOf(request.gender.uppercase()) }
                        .getOrElse { throw ValidationException("Invalid gender: ${request.gender}") }
                val dob =
                    runCatching { LocalDate.parse(request.dob) }
                        .getOrElse { throw ValidationException("Invalid dob, expected ISO-8601: ${request.dob}") }

                val patient =
                    Patient(
                        id = UUID.randomUUID(),
                        nationalId = request.nationalId,
                        firstName = request.firstName,
                        lastName = request.lastName,
                        phone = request.phone,
                        gender = gender,
                        dob = dob,
                    )

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
