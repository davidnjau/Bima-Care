package care.bima.gateway

import care.bima.shared.service.configureErrorHandling
import care.bima.shared.service.configureHealthCheck
import care.bima.shared.service.configureKeycloakAuth
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing

private fun upstreams() =
    listOf(
        UpstreamRoute("/me", System.getenv("IAM_SERVICE_URL") ?: "http://localhost:8085"),
        UpstreamRoute("/patients", System.getenv("PATIENT_SERVICE_URL") ?: "http://localhost:8081"),
        UpstreamRoute("/practitioners", System.getenv("PROVIDER_SERVICE_URL") ?: "http://localhost:8082"),
        UpstreamRoute("/organizations", System.getenv("ORGANIZATION_SERVICE_URL") ?: "http://localhost:8083"),
        UpstreamRoute("/coverages", System.getenv("ELIGIBILITY_SERVICE_URL") ?: "http://localhost:8084"),
        UpstreamRoute("/policies", System.getenv("ELIGIBILITY_SERVICE_URL") ?: "http://localhost:8084"),
        UpstreamRoute("/encounters", System.getenv("ENCOUNTER_SERVICE_URL") ?: "http://localhost:8087"),
        UpstreamRoute("/claims", System.getenv("CLAIMS_SERVICE_URL") ?: "http://localhost:8088"),
        UpstreamRoute("/payments", System.getenv("PAYMENTS_SERVICE_URL") ?: "http://localhost:8089"),
        UpstreamRoute("/consents", System.getenv("CONSENT_SERVICE_URL") ?: "http://localhost:8092"),
        // Note: document-service's upload endpoint is multipart/form-data, but this gateway's
        // proxyRequest() reads the body via receiveText() - fine for its JSON GET routes, but
        // don't proxy the upload route through here yet, it would corrupt binary file bytes.
        UpstreamRoute("/documents", System.getenv("DOCUMENT_SERVICE_URL") ?: "http://localhost:8093"),
    )

fun main() {
    val client = HttpClient(CIO)

    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8080,
        module = { module(client) },
    ).start(wait = true)
}

private val WEB_APP_ORIGIN = System.getenv("WEB_APP_ORIGIN") ?: "localhost:5173"

fun Application.module(client: HttpClient) {
    install(CallLogging)
    install(ContentNegotiation) { json() }
    install(CORS) {
        allowHost(WEB_APP_ORIGIN, schemes = listOf("http", "https"))
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }
    configureErrorHandling()
    configureKeycloakAuth()
    configureHealthCheck()
    routing {
        gatewayRoutes(client, upstreams())
    }
}
