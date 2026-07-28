package care.bima.eligibility.api

import care.bima.eligibility.clients.ReferenceValidationClient
import care.bima.eligibility.db.PolicyRepository
import care.bima.eligibility.domain.Policy
import care.bima.eligibility.domain.PolicyStatus
import care.bima.eligibility.domain.PolicyType
import care.bima.eligibility.events.EligibilityEventPublisher
import care.bima.eligibility.fhir.PolicyFhirMapper
import care.bima.eligibility.identity.DemoInsurerIdentityResolver
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
import java.time.LocalDate
import java.util.UUID

private const val POLICY_NUMBER_SUFFIX_LENGTH = 8

class PolicyRouteDependencies(
    val repository: PolicyRepository,
    val publisher: EligibilityEventPublisher,
    val referenceValidationClient: ReferenceValidationClient,
    val identityResolver: DemoInsurerIdentityResolver,
)

fun Routing.policyRoutes(deps: PolicyRouteDependencies) {
    authenticate("keycloak") {
        route("/policies") {
            post { createPolicy(call, deps) }

            get("/{id}") {
                val id = parseId(call.parameters["id"], "id")
                val policy = deps.repository.findById(id) ?: throw NotFoundException("Policy $id not found")
                call.respond(policy.toResponse())
            }

            get("/{id}/fhir") {
                val id = parseId(call.parameters["id"], "id")
                val policy = deps.repository.findById(id) ?: throw NotFoundException("Policy $id not found")
                val fhirJson = FhirContextProvider.encodeToJson(PolicyFhirMapper.toFhir(policy))
                call.respondText(fhirJson, ContentType.Application.Json)
            }

            get { listPolicies(call, deps) }

            post("/{id}/status") { updateStatus(call, deps) }
        }
    }
}

private suspend fun createPolicy(
    call: ApplicationCall,
    deps: PolicyRouteDependencies,
) {
    val principal = call.principal<JWTPrincipal>()
    val insurerId = deps.identityResolver.resolve(principal?.username())
    if (!deps.referenceValidationClient.isInsurerOrganization(insurerId)) {
        throw ValidationException("Organization $insurerId is not a valid insurer")
    }

    val request = call.receive<CreatePolicyRequest>()
    val type =
        runCatching { PolicyType.valueOf(request.type.uppercase()) }
            .getOrElse { throw ValidationException("Invalid policy type: ${request.type}") }
    val premium =
        runCatching { BigDecimal(request.premium) }
            .getOrElse { throw ValidationException("Invalid premium: ${request.premium}") }
    val startDate = parseDate(request.startDate, "startDate")
    val endDate = request.endDate?.let { parseDate(it, "endDate") }
    if (endDate != null && !endDate.isAfter(startDate)) {
        throw ValidationException("endDate must be after startDate")
    }

    val policy =
        Policy(
            id = UUID.randomUUID(),
            insurerId = insurerId,
            policyNumber = generatePolicyNumber(),
            name = request.name,
            type = type,
            premium = premium,
            startDate = startDate,
            endDate = endDate,
            status = PolicyStatus.ACTIVE,
        )
    val created = deps.repository.create(policy)
    deps.publisher.publishPolicyCreated(created)
    call.respond(HttpStatusCode.Created, created.toResponse())
}

private suspend fun listPolicies(
    call: ApplicationCall,
    deps: PolicyRouteDependencies,
) {
    val principal = call.principal<JWTPrincipal>()
    val isAdmin = "Admin" in (principal?.realmRoles() ?: emptyList())
    val insurerId =
        if (isAdmin) {
            call.request.queryParameters["insurerId"]?.let { parseId(it, "insurerId") }
        } else {
            deps.identityResolver.resolve(principal?.username())
        }
    call.respond(deps.repository.findAll(insurerId).map { it.toResponse() })
}

private suspend fun updateStatus(
    call: ApplicationCall,
    deps: PolicyRouteDependencies,
) {
    val id = parseId(call.parameters["id"], "id")
    val existing = deps.repository.findById(id) ?: throw NotFoundException("Policy $id not found")

    val principal = call.principal<JWTPrincipal>()
    val isAdmin = "Admin" in (principal?.realmRoles() ?: emptyList())
    if (!isAdmin && deps.identityResolver.resolve(principal?.username()) != existing.insurerId) {
        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Not authorized to modify this policy"))
        return
    }

    val request = call.receive<UpdatePolicyStatusRequest>()
    val status =
        runCatching { PolicyStatus.valueOf(request.status.uppercase()) }
            .getOrElse { throw ValidationException("Invalid status: ${request.status}") }
    val updated = deps.repository.updateStatus(id, status) ?: throw NotFoundException("Policy $id not found")
    call.respond(updated.toResponse())
}

private fun parseId(
    raw: String?,
    field: String,
): UUID = runCatching { UUID.fromString(raw) }.getOrElse { throw ValidationException("Invalid $field: $raw") }

// e.g. POL-2026-4F9A2B1C - year plus 8 random hex chars is unique enough at this volume
// without a DB round-trip to check for collisions.
private fun generatePolicyNumber(): String =
    "POL-${LocalDate.now().year}-${UUID.randomUUID().toString().take(POLICY_NUMBER_SUFFIX_LENGTH).uppercase()}"

private fun parseDate(
    raw: String,
    field: String,
): LocalDate =
    runCatching { LocalDate.parse(raw) }
        .getOrElse { throw ValidationException("Invalid $field, expected ISO-8601: $raw") }

private fun Policy.toResponse() =
    PolicyResponse(
        id = id.toString(),
        insurerId = insurerId.toString(),
        policyNumber = policyNumber,
        name = name,
        type = type.name,
        premium = premium.toPlainString(),
        startDate = startDate.toString(),
        endDate = endDate?.toString(),
        status = status.name,
    )
