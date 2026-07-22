package care.bima.payments.api

import care.bima.payments.db.PaymentRepository
import care.bima.payments.domain.Payment
import care.bima.payments.fhir.PaymentFhirMapper
import care.bima.shared.fhir.FhirContextProvider
import care.bima.shared.service.NotFoundException
import care.bima.shared.service.ValidationException
import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.util.UUID

fun Routing.paymentRoutes(repository: PaymentRepository) {
    authenticate("keycloak") {
        route("/payments") {
            get("/{id}") {
                val id = parseId(call.parameters["id"])
                val payment = repository.findById(id) ?: throw NotFoundException("Payment $id not found")
                call.respond(payment.toResponse())
            }

            get("/{id}/fhir") {
                val id = parseId(call.parameters["id"])
                val payment = repository.findById(id) ?: throw NotFoundException("Payment $id not found")
                val fhirJson = FhirContextProvider.encodeToJson(PaymentFhirMapper.toFhir(payment))
                call.respondText(fhirJson, ContentType.Application.Json)
            }

            get {
                val patientId = call.request.queryParameters["patientId"]?.let { parseId(it) }
                call.respond(repository.findAll(patientId).map { it.toResponse() })
            }
        }
    }
}

private fun parseId(raw: String?): UUID =
    runCatching { UUID.fromString(raw) }.getOrElse { throw ValidationException("Invalid payment id: $raw") }

private fun Payment.toResponse() =
    PaymentResponse(
        id = id.toString(),
        claimId = claimId.toString(),
        patientId = patientId.toString(),
        amount = amount.toPlainString(),
        releasedAt = releasedAt.toString(),
    )
