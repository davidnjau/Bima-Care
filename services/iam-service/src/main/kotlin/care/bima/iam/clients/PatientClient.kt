package care.bima.iam.clients

import care.bima.shared.service.ServiceToServiceClient
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

data class PatientSummary(
    val id: UUID,
    val phone: String,
    val email: String?,
    val firstName: String,
    val lastName: String,
)

class PatientClient(
    private val client: ServiceToServiceClient,
    private val patientServiceUrl: String = System.getenv("PATIENT_SERVICE_URL") ?: "http://localhost:8086",
) {
    suspend fun getPatient(patientId: UUID): PatientSummary {
        val response = client.get("$patientServiceUrl/patients/$patientId")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return PatientSummary(
            id = patientId,
            phone = body.getValue("phone").jsonPrimitive.content,
            email = body["email"]?.takeIf { it != JsonNull }?.jsonPrimitive?.content,
            firstName = body.getValue("firstName").jsonPrimitive.content,
            lastName = body.getValue("lastName").jsonPrimitive.content,
        )
    }
}
