package care.bima.shared.service

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import java.net.URI
import java.util.concurrent.TimeUnit

private const val JWK_CACHE_SIZE = 10L
private const val JWK_CACHE_HOURS = 24L
private const val JWK_RATE_LIMIT_REQUESTS = 10L
private const val JWK_RATE_LIMIT_MINUTES = 1L

private const val DEFAULT_KEYCLOAK_ISSUER = "http://localhost:8180/realms/bima-care"

fun Application.configureKeycloakAuth(issuer: String = System.getenv("KEYCLOAK_ISSUER") ?: DEFAULT_KEYCLOAK_ISSUER) {
    val jwkProvider =
        JwkProviderBuilder(URI("$issuer/protocol/openid-connect/certs").toURL())
            .cached(JWK_CACHE_SIZE, JWK_CACHE_HOURS, TimeUnit.HOURS)
            .rateLimited(JWK_RATE_LIMIT_REQUESTS, JWK_RATE_LIMIT_MINUTES, TimeUnit.MINUTES)
            .build()

    install(Authentication) {
        jwt("keycloak") {
            verifier(jwkProvider, issuer)
            validate { credential ->
                if (credential.payload.subject != null) JWTPrincipal(credential.payload) else null
            }
        }
    }
}

fun JWTPrincipal.realmRoles(): List<String> {
    @Suppress("UNCHECKED_CAST")
    val roles = (payload.getClaim("realm_access").asMap()?.get("roles") as? List<String>)
    return roles ?: emptyList()
}

fun JWTPrincipal.username(): String? = payload.getClaim("preferred_username").asString()

fun JWTPrincipal.patientId(): String? = payload.getClaim("patientId").asString()
