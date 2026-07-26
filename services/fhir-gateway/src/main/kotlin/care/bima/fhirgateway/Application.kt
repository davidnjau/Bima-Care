package care.bima.fhirgateway

import care.bima.shared.service.configureErrorHandling
import care.bima.shared.service.configureHealthCheck
import care.bima.shared.service.configureKeycloakAuth
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

fun main() {
    val client = HttpClient(CIO)
    val consentGateClient = ConsentGateClient(client)

    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8091,
        module = { module(client, consentGateClient) },
    ).start(wait = true)
}

fun Application.module(
    client: HttpClient,
    consentGateClient: ConsentGateClient,
) {
    install(CallLogging)
    install(ContentNegotiation) { json() }
    configureErrorHandling()
    configureKeycloakAuth()
    configureHealthCheck()
    routing {
        fhirGatewayRoutes(client, consentGateClient)
    }
}
