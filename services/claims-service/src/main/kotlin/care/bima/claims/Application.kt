package care.bima.claims

import care.bima.claims.api.ClaimRouteDependencies
import care.bima.claims.api.claimRoutes
import care.bima.claims.clients.DocumentClient
import care.bima.claims.clients.EligibilityClient
import care.bima.claims.clients.EncounterClient
import care.bima.claims.db.ClaimRepository
import care.bima.claims.events.ClaimEventPublisher
import care.bima.claims.identity.DemoProviderIdentityResolver
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

    val repository = ClaimRepository().also { it.createSchema() }
    val publisher =
        ClaimEventPublisher(
            bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092",
        )
    val serviceClient = ServiceToServiceClient()
    val eligibilityClient = EligibilityClient(serviceClient)
    val encounterClient = EncounterClient(serviceClient)
    val documentClient = DocumentClient(serviceClient)
    val identityResolver = DemoProviderIdentityResolver()

    val deps =
        ClaimRouteDependencies(
            repository,
            publisher,
            eligibilityClient,
            encounterClient,
            documentClient,
            identityResolver,
        )

    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8088,
        module = { module(deps) },
    ).start(wait = true)
}

fun Application.module(deps: ClaimRouteDependencies) {
    install(CallLogging)
    install(ContentNegotiation) { json() }
    configureErrorHandling()
    configureKeycloakAuth()
    configureHealthCheck()
    routing {
        claimRoutes(deps)
    }
}
