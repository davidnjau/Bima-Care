package care.bima.claims.clients

import care.bima.shared.service.ServiceToServiceClient
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

class EncounterClient(
    private val client: ServiceToServiceClient,
    private val encounterServiceUrl: String = System.getenv("ENCOUNTER_SERVICE_URL") ?: "http://localhost:8087",
) {
    suspend fun createEncounter(
        patientId: UUID,
        practitionerId: UUID?,
        organizationId: UUID,
    ): UUID {
        val practitionerField = practitionerId?.let { "\"$it\"" } ?: "null"
        val body =
            """{"patientId":"$patientId","practitionerId":$practitionerField,"organizationId":"$organizationId"}"""
        val response = client.post("$encounterServiceUrl/encounters", body)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return UUID.fromString(json.getValue("id").jsonPrimitive.content)
    }
}
