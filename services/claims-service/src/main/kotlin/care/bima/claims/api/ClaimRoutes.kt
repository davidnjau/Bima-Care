package care.bima.claims.api

import care.bima.claims.clients.EligibilityClient
import care.bima.claims.clients.EncounterClient
import care.bima.claims.db.ClaimRepository
import care.bima.claims.domain.Claim
import care.bima.claims.domain.ClaimStatus
import care.bima.claims.events.ClaimEventPublisher
import care.bima.claims.fhir.ClaimFhirMapper
import care.bima.claims.identity.DemoProviderIdentityResolver
import care.bima.shared.fhir.FhirContextProvider
import care.bima.shared.service.ErrorResponse
import care.bima.shared.service.NotFoundException
import care.bima.shared.service.ValidationException
import care.bima.shared.service.realmRoles
import care.bima.shared.service.username
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class ClaimRouteDependencies(
    val repository: ClaimRepository,
    val publisher: ClaimEventPublisher,
    val eligibilityClient: EligibilityClient,
    val encounterClient: EncounterClient,
    val identityResolver: DemoProviderIdentityResolver,
)

fun Routing.claimRoutes(deps: ClaimRouteDependencies) {
    authenticate("keycloak") {
        route("/claims") {
            post { submitClaim(call, deps) }

            get("/{id}") {
                val id = parseId(call.parameters["id"], "id")
                val claim = deps.repository.findById(id) ?: throw NotFoundException("Claim $id not found")
                call.respond(claim.toResponse())
            }

            get("/{id}/fhir") {
                val id = parseId(call.parameters["id"], "id")
                val claim = deps.repository.findById(id) ?: throw NotFoundException("Claim $id not found")
                val fhirJson = FhirContextProvider.encodeToJson(ClaimFhirMapper.toFhir(claim))
                call.respondText(fhirJson, ContentType.Application.Json)
            }

            get {
                val status = call.request.queryParameters["status"]?.let { parseStatus(it) }
                val patientId = call.request.queryParameters["patientId"]?.let { parseId(it, "patientId") }
                val organizationId =
                    call.request.queryParameters["organizationId"]?.let { parseId(it, "organizationId") }
                call.respond(deps.repository.findAll(status, patientId, organizationId).map { it.toResponse() })
            }

            post("/{id}/adjudicate") { adjudicateClaim(call, deps) }
        }
    }
}

private suspend fun submitClaim(
    call: ApplicationCall,
    deps: ClaimRouteDependencies,
) {
    val principal = call.principal<JWTPrincipal>()
    val identity = deps.identityResolver.resolve(principal?.username())

    val request = call.receive<SubmitClaimRequest>()
    val patientId = parseId(request.patientId, "patientId")
    val amount =
        runCatching { BigDecimal(request.amount) }
            .getOrElse { throw ValidationException("Invalid amount: ${request.amount}") }

    val eligibility = deps.eligibilityClient.verifyEligibility(patientId)
    if (!eligibility.eligible || eligibility.coverageId == null) {
        throw ValidationException("Patient $patientId has no active coverage")
    }

    val encounterId =
        deps.encounterClient.createEncounter(patientId, identity.practitionerId, identity.organizationId)

    val claim =
        Claim(
            id = UUID.randomUUID(),
            patientId = patientId,
            encounterId = encounterId,
            coverageId = eligibility.coverageId,
            practitionerId = identity.practitionerId,
            organizationId = identity.organizationId,
            serviceType = request.serviceType,
            diagnosisCode = request.diagnosisCode,
            treatmentDetails = request.treatmentDetails,
            requestedAmount = amount,
            approvedAmount = null,
            status = ClaimStatus.SUBMITTED,
            submittedAt = LocalDateTime.now(),
            adjudicatedAt = null,
        )

    val created = deps.repository.create(claim)
    deps.publisher.publishClaimSubmitted(created)
    call.respond(HttpStatusCode.Created, created.toResponse())
}

private suspend fun adjudicateClaim(
    call: ApplicationCall,
    deps: ClaimRouteDependencies,
) {
    val principal = call.principal<JWTPrincipal>()
    if ("Admin" !in (principal?.realmRoles() ?: emptyList())) {
        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Admin role required to adjudicate claims"))
        return
    }

    val id = parseId(call.parameters["id"], "id")
    val existing = deps.repository.findById(id) ?: throw NotFoundException("Claim $id not found")
    if (existing.status != ClaimStatus.SUBMITTED) {
        throw ValidationException("Claim $id has already been adjudicated")
    }

    val request = call.receive<AdjudicateClaimRequest>()
    val decision = parseDecision(request.decision)
    val approvedAmount = resolveApprovedAmount(decision, request.approvedAmount, existing.requestedAmount)

    val adjudicated =
        deps.repository.adjudicate(id, decision, approvedAmount, LocalDateTime.now())
            ?: throw NotFoundException("Claim $id not found")
    deps.publisher.publishClaimAdjudicated(adjudicated)
    call.respond(adjudicated.toResponse())
}

private fun parseDecision(raw: String): ClaimStatus {
    val decision =
        runCatching { ClaimStatus.valueOf(raw.uppercase()) }
            .getOrElse { throw ValidationException("Invalid decision: $raw") }
    if (decision == ClaimStatus.SUBMITTED) {
        throw ValidationException("Decision must be APPROVED, PARTIALLY_APPROVED, or REJECTED")
    }
    return decision
}

private fun resolveApprovedAmount(
    decision: ClaimStatus,
    requestedApprovedAmount: String?,
    requestedAmount: BigDecimal,
): BigDecimal? =
    when (decision) {
        ClaimStatus.APPROVED -> requestedApprovedAmount?.let(::BigDecimal) ?: requestedAmount
        ClaimStatus.PARTIALLY_APPROVED ->
            requestedApprovedAmount?.let(::BigDecimal)
                ?: throw ValidationException("approvedAmount is required for a partial approval")
        else -> null
    }

private fun parseId(
    raw: String?,
    field: String,
): UUID = runCatching { UUID.fromString(raw) }.getOrElse { throw ValidationException("Invalid $field: $raw") }

private fun parseStatus(raw: String): ClaimStatus =
    runCatching { ClaimStatus.valueOf(raw.uppercase()) }.getOrElse { throw ValidationException("Invalid status: $raw") }

private fun Claim.toResponse() =
    ClaimResponse(
        id = id.toString(),
        patientId = patientId.toString(),
        encounterId = encounterId.toString(),
        coverageId = coverageId.toString(),
        practitionerId = practitionerId.toString(),
        organizationId = organizationId.toString(),
        serviceType = serviceType,
        diagnosisCode = diagnosisCode,
        treatmentDetails = treatmentDetails,
        requestedAmount = requestedAmount.toPlainString(),
        approvedAmount = approvedAmount?.toPlainString(),
        status = status.name,
        submittedAt = submittedAt.toString(),
        adjudicatedAt = adjudicatedAt?.toString(),
    )
