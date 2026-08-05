package care.bima.iam.clients

import care.bima.shared.service.ServiceToServiceClient
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

data class OrganizationSummary(
    val id: UUID,
    val name: String,
    val phone: String,
    val email: String?,
    val type: String,
)

class OrganizationClient(
    private val client: ServiceToServiceClient,
    private val organizationServiceUrl: String = System.getenv("ORGANIZATION_SERVICE_URL") ?: "http://localhost:8083",
) {
    suspend fun getOrganization(organizationId: UUID): OrganizationSummary {
        val response = client.get("$organizationServiceUrl/organizations/$organizationId")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return OrganizationSummary(
            id = organizationId,
            name = body.getValue("name").jsonPrimitive.content,
            phone = body.getValue("phone").jsonPrimitive.content,
            email = body["email"]?.takeIf { it != JsonNull }?.jsonPrimitive?.content,
            type = body.getValue("type").jsonPrimitive.content,
        )
    }
}
