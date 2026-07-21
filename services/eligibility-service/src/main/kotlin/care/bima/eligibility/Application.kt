package care.bima.eligibility

import care.bima.eligibility.api.coverageRoutes
import care.bima.eligibility.clients.ReferenceValidationClient
import care.bima.eligibility.db.CoverageRepository
import care.bima.eligibility.events.EligibilityEventPublisher
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

    val repository = CoverageRepository().also { it.createSchema() }
    val publisher =
        EligibilityEventPublisher(
            bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092",
        )
    val referenceValidationClient = ReferenceValidationClient(ServiceToServiceClient())

    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8084,
        module = { module(repository, publisher, referenceValidationClient) },
    ).start(wait = true)
}

fun Application.module(
    repository: CoverageRepository,
    publisher: EligibilityEventPublisher,
    referenceValidationClient: ReferenceValidationClient,
) {
    install(CallLogging)
    install(ContentNegotiation) { json() }
    configureErrorHandling()
    configureKeycloakAuth()
    configureHealthCheck()
    routing {
        coverageRoutes(repository, publisher, referenceValidationClient)
    }
}
