package care.bima.document

import care.bima.document.api.DocumentRouteDependencies
import care.bima.document.api.documentRoutes
import care.bima.document.clients.ReferenceValidationClient
import care.bima.document.db.DocumentRepository
import care.bima.document.events.DocumentEventPublisher
import care.bima.document.storage.ObjectStorageClient
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

    val repository = DocumentRepository().also { it.createSchema() }
    val publisher =
        DocumentEventPublisher(
            bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092",
        )
    val referenceValidationClient = ReferenceValidationClient(ServiceToServiceClient())
    val storageClient = ObjectStorageClient()

    val deps = DocumentRouteDependencies(repository, publisher, referenceValidationClient, storageClient)

    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8093,
        module = { module(deps) },
    ).start(wait = true)
}

fun Application.module(deps: DocumentRouteDependencies) {
    install(CallLogging)
    install(ContentNegotiation) { json() }
    configureErrorHandling()
    configureKeycloakAuth()
    configureHealthCheck()
    routing {
        documentRoutes(deps)
    }
}
