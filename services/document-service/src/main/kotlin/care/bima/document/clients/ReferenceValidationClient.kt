package care.bima.document.clients

import care.bima.shared.service.ServiceToServiceClient
import io.ktor.http.HttpStatusCode
import java.util.UUID

class ReferenceValidationClient(
    private val client: ServiceToServiceClient,
    private val patientServiceUrl: String = System.getenv("PATIENT_SERVICE_URL") ?: "http://localhost:8081",
) {
    suspend fun patientExists(patientId: UUID): Boolean {
        return client.get("$patientServiceUrl/patients/$patientId").status == HttpStatusCode.OK
    }
}
