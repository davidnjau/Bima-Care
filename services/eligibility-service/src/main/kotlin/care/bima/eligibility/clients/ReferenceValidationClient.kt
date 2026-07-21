package care.bima.eligibility.clients

import care.bima.shared.service.ServiceToServiceClient
import io.ktor.http.HttpStatusCode
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
}
