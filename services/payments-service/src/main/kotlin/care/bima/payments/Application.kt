package care.bima.payments

import care.bima.payments.api.paymentRoutes
import care.bima.payments.db.PaymentRepository
import care.bima.payments.events.ClaimAdjudicatedConsumer
import care.bima.payments.events.PaymentEventPublisher
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

    val repository = PaymentRepository().also { it.createSchema() }
    val bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092"
    val publisher = PaymentEventPublisher(bootstrapServers)
    ClaimAdjudicatedConsumer(bootstrapServers, repository, publisher).start()

    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8089,
        module = { module(repository) },
    ).start(wait = true)
}

fun Application.module(repository: PaymentRepository) {
    install(CallLogging)
    install(ContentNegotiation) { json() }
    configureErrorHandling()
    configureKeycloakAuth()
    configureHealthCheck()
    routing {
        paymentRoutes(repository)
    }
}
