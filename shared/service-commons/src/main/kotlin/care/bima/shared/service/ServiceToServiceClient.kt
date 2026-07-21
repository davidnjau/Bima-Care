package care.bima.shared.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ServiceToServiceClient(
    private val issuer: String = System.getenv("KEYCLOAK_ISSUER") ?: "http://localhost:8180/realms/bima-care",
    private val clientId: String = System.getenv("SERVICE_CLIENT_ID") ?: "bima-gateway",
    private val clientSecret: String = System.getenv("SERVICE_CLIENT_SECRET") ?: "local-dev-only-changeme",
) {
    private val client = HttpClient(CIO)

    private suspend fun fetchAccessToken(): String {
        val response =
            client.post("$issuer/protocol/openid-connect/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("grant_type=client_credentials&client_id=$clientId&client_secret=$clientSecret")
            }
        val body = Json.parseToJsonElement(response.body<String>()).jsonObject
        return body.getValue("access_token").jsonPrimitive.content
    }

    suspend fun get(url: String): HttpResponse {
        val token = fetchAccessToken()
        return client.get(url) { header("Authorization", "Bearer $token") }
    }
}
