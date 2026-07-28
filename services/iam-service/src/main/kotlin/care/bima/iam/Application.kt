package care.bima.iam

import care.bima.iam.api.identityRoutes
import care.bima.iam.clients.KeycloakAdminClient
import care.bima.iam.clients.OrganizationClient
import care.bima.iam.clients.PatientClient
import care.bima.iam.events.InsurerAccountProvisioningConsumer
import care.bima.iam.events.MemberAccountProvisioningConsumer
import care.bima.shared.service.ServiceToServiceClient
import care.bima.shared.service.configureErrorHandling
import care.bima.shared.service.configureHealthCheck
import care.bima.shared.service.configureKeycloakAuth
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

fun main() {
    val patientClient = PatientClient(ServiceToServiceClient())
    val organizationClient = OrganizationClient(ServiceToServiceClient())
    val adminServiceClient =
        ServiceToServiceClient(
            clientId = System.getenv("IAM_PROVISIONER_CLIENT_ID") ?: "bima-iam-provisioner",
            clientSecret = System.getenv("IAM_PROVISIONER_CLIENT_SECRET") ?: "local-dev-only-changeme",
        )
    val keycloakAdminClient = KeycloakAdminClient(adminServiceClient)
    val bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092"
    MemberAccountProvisioningConsumer(
        bootstrapServers = bootstrapServers,
        patientClient = patientClient,
        keycloakAdminClient = keycloakAdminClient,
    ).start()
    InsurerAccountProvisioningConsumer(
        bootstrapServers = bootstrapServers,
        organizationClient = organizationClient,
        keycloakAdminClient = keycloakAdminClient,
    ).start()

    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8085,
        module = Application::module,
    ).start(wait = true)
}

fun Application.module() {
    install(CallLogging)
    install(ContentNegotiation) { json() }
    configureErrorHandling()
    configureKeycloakAuth()
    configureHealthCheck()
    routing {
        identityRoutes()
    }
}
