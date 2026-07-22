package care.bima.claims.identity

import care.bima.shared.service.ValidationException
import java.util.UUID

private const val IDENTITY_ENTRY_FIELD_COUNT = 3

data class ProviderIdentity(val practitionerId: UUID, val organizationId: UUID)

/**
 * Provider Keycloak users aren't linked to a Practitioner/Organization record anywhere yet
 * (no such mapping exists in iam-service or provider-service) - this is a known Phase 2
 * shortcut, tracked in implementation-ui.md's "Practitioner/Organization identity for a
 * logged-in provider" open decision. Resolves a demo provider's username straight to a
 * pre-seeded practitionerId/organizationId pair via env var, so claims can reference real,
 * validated Practitioner/Organization rows without building real identity linkage yet.
 *
 * Format: "username1:practitionerId1:organizationId1;username2:practitionerId2:organizationId2"
 */
class DemoProviderIdentityResolver(
    mapping: String = System.getenv("DEMO_PROVIDER_IDENTITIES") ?: "",
) {
    private val identities: Map<String, ProviderIdentity> = parse(mapping)

    fun resolve(username: String?): ProviderIdentity {
        if (username == null) throw ValidationException("Missing preferred_username claim on token")
        return identities[username]
            ?: throw ValidationException(
                "No demo provider identity configured for '$username' - set DEMO_PROVIDER_IDENTITIES",
            )
    }

    private fun parse(raw: String): Map<String, ProviderIdentity> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").filter { it.isNotBlank() }.associate { entry ->
            val parts = entry.split(":")
            require(parts.size == IDENTITY_ENTRY_FIELD_COUNT) { "Invalid DEMO_PROVIDER_IDENTITIES entry: $entry" }
            val (username, practitionerId, organizationId) = parts
            username to ProviderIdentity(UUID.fromString(practitionerId), UUID.fromString(organizationId))
        }
    }
}
