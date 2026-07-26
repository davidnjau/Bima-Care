package care.bima.eligibility.api

import care.bima.eligibility.clients.ReferenceValidationClient
import care.bima.eligibility.db.CoverageRepository
import care.bima.eligibility.domain.Coverage
import care.bima.eligibility.domain.CoverageStatus
import care.bima.eligibility.events.EligibilityEventPublisher
import care.bima.eligibility.fhir.CoverageFhirMapper
import care.bima.shared.fhir.FhirContextProvider
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

fun Routing.coverageRoutes(
    repository: CoverageRepository,
    publisher: EligibilityEventPublisher,
    referenceValidationClient: ReferenceValidationClient,
) {
    authenticate("keycloak") {
        route("/coverages") {
            post { createCoverage(call, repository, publisher, referenceValidationClient) }

            get("/{id}") {
                val id = parseId(call.parameters["id"], "id")
                val coverage = repository.findById(id) ?: throw NotFoundException("Coverage $id not found")
                call.respond(coverage.toResponse())
            }

            get("/{id}/fhir") {
                val id = parseId(call.parameters["id"], "id")
                val coverage = repository.findById(id) ?: throw NotFoundException("Coverage $id not found")
                val fhirJson = FhirContextProvider.encodeToJson(CoverageFhirMapper.toFhir(coverage))
                call.respondText(fhirJson, ContentType.Application.Json)
            }

            get {
                call.respond(repository.findAll().map { it.toResponse() })
            }

            get("/verify/{patientId}") {
                val patientId = parseId(call.parameters["patientId"], "patientId")
                val coverage = repository.findActiveCoverage(patientId, LocalDate.now())
                if (coverage != null) {
                    publisher.publishCoverageVerified(coverage)
                    call.respond(EligibilityResult(eligible = true, coverage = coverage.toResponse()))
                } else {
                    call.respond(EligibilityResult(eligible = false))
                }
            }
        }
    }
}

private suspend fun createCoverage(
    call: ApplicationCall,
    repository: CoverageRepository,
    publisher: EligibilityEventPublisher,
    referenceValidationClient: ReferenceValidationClient,
) {
    val request = call.receive<CreateCoverageRequest>()
    val patientId = parseId(request.patientId, "patientId")
    val insurerId = parseId(request.insurerId, "insurerId")
    val status =
        runCatching { CoverageStatus.valueOf(request.status.uppercase()) }
            .getOrElse { throw ValidationException("Invalid coverage status: ${request.status}") }
    val startDate = parseDate(request.startDate, "startDate")
    val endDate = request.endDate?.let { parseDate(it, "endDate") }

    if (!referenceValidationClient.patientExists(patientId)) {
        throw ValidationException("Patient $patientId not found")
    }
    if (!referenceValidationClient.organizationExists(insurerId)) {
        throw ValidationException("Insurer organization $insurerId not found")
    }

    val coverage =
        Coverage(
            id = UUID.randomUUID(),
            patientId = patientId,
            insurerId = insurerId,
            status = status,
            startDate = startDate,
            endDate = endDate,
            planTier = request.planTier,
            policyId = request.policyId?.let { parseId(it, "policyId") },
        )

    val created = repository.create(coverage)
    publisher.publishCoverageVerified(created)
    call.respond(HttpStatusCode.Created, created.toResponse())
}

private fun parseId(
    raw: String?,
    field: String,
): UUID = runCatching { UUID.fromString(raw) }.getOrElse { throw ValidationException("Invalid $field: $raw") }

private fun parseDate(
    raw: String,
    field: String,
): LocalDate =
    runCatching { LocalDate.parse(raw) }
        .getOrElse { throw ValidationException("Invalid $field, expected ISO-8601: $raw") }

private fun Coverage.toResponse() =
    CoverageResponse(
        id = id.toString(),
        patientId = patientId.toString(),
        insurerId = insurerId.toString(),
        status = status.name,
        startDate = startDate.toString(),
        endDate = endDate?.toString(),
        planTier = planTier,
        policyId = policyId?.toString(),
    )
