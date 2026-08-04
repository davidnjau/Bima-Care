package care.bima.patient

import care.bima.patient.api.dependentRoutes
import care.bima.patient.api.patientRoutes
import care.bima.patient.db.DependentRepository
import care.bima.patient.db.PatientRepository
import care.bima.patient.events.PatientEventPublisher
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

    val repository = PatientRepository().also { it.createSchema() }
    val dependentRepository = DependentRepository().also { it.createSchema() }
    val publisher =
        PatientEventPublisher(
            bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092",
        )

    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8081,
        module = { module(repository, dependentRepository, publisher) },
    ).start(wait = true)
}

fun Application.module(
    repository: PatientRepository,
    dependentRepository: DependentRepository,
    publisher: PatientEventPublisher,
) {
    install(CallLogging)
    install(ContentNegotiation) { json() }
    configureErrorHandling()
    configureKeycloakAuth()
    configureHealthCheck()
    routing {
        patientRoutes(repository, publisher)
        dependentRoutes(repository, dependentRepository)
    }
}
