package care.bima.iam.api

import care.bima.shared.service.realmRoles
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Routing.identityRoutes() {
    authenticate("keycloak") {
        route("/me") {
            get {
                val principal = call.principal<JWTPrincipal>()
                if (principal == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }
                call.respond(
                    IdentityResponse(
                        subject = principal.payload.subject,
                        roles = principal.realmRoles(),
                    ),
                )
            }
        }
    }
}
