package care.bima.claims.clients

import care.bima.shared.service.ServiceToServiceClient
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

class EligibilityClient(
    private val client: ServiceToServiceClient,
    private val eligibilityServiceUrl: String = System.getenv("ELIGIBILITY_SERVICE_URL") ?: "http://localhost:8084",
) {
    data class Result(val eligible: Boolean, val coverageId: UUID?)

    suspend fun verifyEligibility(patientId: UUID): Result {
        val response = client.get("$eligibilityServiceUrl/coverages/verify/$patientId")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val eligible = body["eligible"]?.jsonPrimitive?.content?.toBoolean() ?: false
        val coverageId =
            body["coverage"]?.jsonObject?.get("id")?.jsonPrimitive?.content?.let(UUID::fromString)
        return Result(eligible, coverageId)
    }
}
