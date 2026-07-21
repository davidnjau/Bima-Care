package care.bima.provider

import care.bima.provider.api.practitionerRoutes
import care.bima.provider.db.PractitionerRepository
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

    val repository = PractitionerRepository().also { it.createSchema() }

    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8082,
        module = { module(repository) },
    ).start(wait = true)
}

fun Application.module(repository: PractitionerRepository) {
    install(CallLogging)
    install(ContentNegotiation) { json() }
    configureErrorHandling()
    configureKeycloakAuth()
    configureHealthCheck()
    routing {
        practitionerRoutes(repository)
    }
}
