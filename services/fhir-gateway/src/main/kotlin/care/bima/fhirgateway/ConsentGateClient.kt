package care.bima.fhirgateway

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ConsentGateClient(
    private val client: HttpClient,
    private val consentServiceUrl: String = System.getenv("CONSENT_SERVICE_URL") ?: "http://localhost:8092",
) {
    suspend fun hasActiveConsent(
        granteeId: String,
        patientId: String,
        authHeader: String?,
    ): Boolean {
        val response =
            client.get("$consentServiceUrl/consents/verify") {
                parameter("patientId", patientId)
                parameter("granteeId", granteeId)
                authHeader?.let { header("Authorization", it) }
            }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return body["active"]?.jsonPrimitive?.booleanOrNull ?: false
    }
}
