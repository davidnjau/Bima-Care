package care.bima.eligibility.identity

import care.bima.shared.service.ValidationException
import java.util.UUID

private const val IDENTITY_ENTRY_FIELD_COUNT = 2

/**
 * Insurer Keycloak users aren't linked to an Organization record anywhere yet - same known
 * Phase 2 shortcut as claims-service's DemoProviderIdentityResolver, just for the Insurer role
 * instead of Provider. Resolves a demo insurer's username to a pre-seeded Organization id
 * (type INSURER) via env var.
 *
 * Format: "username1:insurerId1;username2:insurerId2"
 */
class DemoInsurerIdentityResolver(
    mapping: String = System.getenv("DEMO_INSURER_IDENTITIES") ?: "",
) {
    private val identities: Map<String, UUID> = parse(mapping)

    fun resolve(username: String?): UUID {
        if (username == null) throw ValidationException("Missing preferred_username claim on token")
        return identities[username]
            ?: throw ValidationException(
                "No demo insurer identity configured for '$username' - set DEMO_INSURER_IDENTITIES",
            )
    }

    private fun parse(raw: String): Map<String, UUID> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").filter { it.isNotBlank() }.associate { entry ->
            val parts = entry.split(":")
            require(parts.size == IDENTITY_ENTRY_FIELD_COUNT) { "Invalid DEMO_INSURER_IDENTITIES entry: $entry" }
            val (username, insurerId) = parts
            username to UUID.fromString(insurerId)
        }
    }
}
