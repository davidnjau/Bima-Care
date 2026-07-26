package care.bima.sync

import care.bima.shared.service.ServiceToServiceClient
import care.bima.shared.service.configureHealthCheck
import care.bima.sync.events.RegistrationSyncConsumer
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun main() {
    val bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092"
    val hapiFhirServerUrl = System.getenv("HAPI_FHIR_SERVER_URL") ?: "http://localhost:8094/fhir"

    val httpClient = HttpClient(CIO)
    val serviceClient = ServiceToServiceClient()
    RegistrationSyncConsumer(bootstrapServers, serviceClient, httpClient, hapiFhirServerUrl).start()

    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8095,
        module = { module() },
    ).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    configureHealthCheck()
}
