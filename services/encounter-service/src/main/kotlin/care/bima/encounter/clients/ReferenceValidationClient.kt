package care.bima.encounter.clients

import care.bima.shared.service.ServiceToServiceClient
import io.ktor.http.HttpStatusCode
import java.util.UUID

class ReferenceValidationClient(
    private val client: ServiceToServiceClient,
    private val patientServiceUrl: String = System.getenv("PATIENT_SERVICE_URL") ?: "http://localhost:8081",
    private val providerServiceUrl: String = System.getenv("PROVIDER_SERVICE_URL") ?: "http://localhost:8082",
    private val organizationServiceUrl: String = System.getenv("ORGANIZATION_SERVICE_URL") ?: "http://localhost:8083",
) {
    suspend fun patientExists(patientId: UUID): Boolean {
        return client.get("$patientServiceUrl/patients/$patientId").status == HttpStatusCode.OK
    }

    suspend fun practitionerExists(practitionerId: UUID): Boolean {
        return client.get("$providerServiceUrl/practitioners/$practitionerId").status == HttpStatusCode.OK
    }

    suspend fun organizationExists(organizationId: UUID): Boolean {
        return client.get("$organizationServiceUrl/organizations/$organizationId").status == HttpStatusCode.OK
    }
}
