package care.bima.audit

import care.bima.audit.api.auditEventRoutes
import care.bima.audit.db.AuditEventRepository
import care.bima.audit.events.AuditEventConsumer
import care.bima.shared.service.configureErrorHandling
import care.bima.shared.service.configureHealthCheck
import care.bima.shared.service.configureKeycloakAuth
import care.bima.shared.service.connectToPostgres
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

fun main() {
    connectToPostgres()

    val repository = AuditEventRepository().also { it.createSchema() }
    val consumer =
        AuditEventConsumer(
            bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092",
            repository = repository,
        )
    consumer.start()

    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8090,
        module = { module(repository) },
    ).start(wait = true)
}

fun Application.module(repository: AuditEventRepository) {
    install(ContentNegotiation) { json() }
    configureErrorHandling()
    configureKeycloakAuth()
    configureHealthCheck()
    routing {
        auditEventRoutes(repository)
    }
}
