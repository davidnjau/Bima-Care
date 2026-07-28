package care.bima.claims.clients

import care.bima.shared.service.ServiceToServiceClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

class DocumentClient(
    private val client: ServiceToServiceClient,
    private val documentServiceUrl: String = System.getenv("DOCUMENT_SERVICE_URL") ?: "http://localhost:8093",
) {
    suspend fun belongsToPatient(
        documentId: UUID,
        patientId: UUID,
    ): Boolean {
        val response = client.get("$documentServiceUrl/documents/$documentId")
        if (response.status != HttpStatusCode.OK) return false
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return body["patientId"]?.jsonPrimitive?.content == patientId.toString()
    }
}
