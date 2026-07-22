package care.bima.consent

import care.bima.consent.api.consentRoutes
import care.bima.consent.clients.ReferenceValidationClient
import care.bima.consent.db.ConsentRepository
import care.bima.consent.events.ConsentEventPublisher
import care.bima.shared.service.ServiceToServiceClient
import care.bima.shared.service.configureErrorHandling
import care.bima.shared.service.configureHealthCheck
import care.bima.shared.service.configureKeycloakAuth
import care.bima.shared.service.connectToPostgres
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

fun main() {
    connectToPostgres()

    val repository = ConsentRepository().also { it.createSchema() }
    val publisher =
        ConsentEventPublisher(
            bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092",
        )
    val referenceValidationClient = ReferenceValidationClient(ServiceToServiceClient())

    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8092,
        module = { module(repository, publisher, referenceValidationClient) },
    ).start(wait = true)
}

fun Application.module(
    repository: ConsentRepository,
    publisher: ConsentEventPublisher,
    referenceValidationClient: ReferenceValidationClient,
) {
    install(CallLogging)
    install(ContentNegotiation) { json() }
    configureErrorHandling()
    configureKeycloakAuth()
    configureHealthCheck()
    routing {
        consentRoutes(repository, publisher, referenceValidationClient)
    }
}
