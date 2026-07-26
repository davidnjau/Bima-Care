package care.bima.eligibility.clients

import care.bima.shared.service.ServiceToServiceClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

class ReferenceValidationClient(
    private val client: ServiceToServiceClient,
    private val patientServiceUrl: String = System.getenv("PATIENT_SERVICE_URL") ?: "http://localhost:8081",
    private val organizationServiceUrl: String = System.getenv("ORGANIZATION_SERVICE_URL") ?: "http://localhost:8083",
) {
    suspend fun patientExists(patientId: UUID): Boolean {
        return client.get("$patientServiceUrl/patients/$patientId").status == HttpStatusCode.OK
    }

    suspend fun organizationExists(organizationId: UUID): Boolean {
        return client.get("$organizationServiceUrl/organizations/$organizationId").status == HttpStatusCode.OK
    }

    suspend fun isInsurerOrganization(organizationId: UUID): Boolean {
        val response = client.get("$organizationServiceUrl/organizations/$organizationId")
        if (response.status != HttpStatusCode.OK) return false
        val type = Json.parseToJsonElement(response.bodyAsText()).jsonObject["type"]?.jsonPrimitive?.content
        return type == "INSURER"
    }
}
