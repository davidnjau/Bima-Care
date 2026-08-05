package care.bima.iam.clients

import care.bima.shared.service.ServiceToServiceClient
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.random.Random

private const val TEMP_PASSWORD_LENGTH = 12
private val PASSWORD_CHARS = ('A'..'Z') + ('a'..'z') + ('0'..'9')

private data class AccountAttribute(val key: String, val value: String)

// Bundled together (rather than 4 separate params) to keep provisionAccount's parameter
// count from tripping detekt's LongParameterList - already hit that twice this session.
private data class AccountIdentity(
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
)

/**
 * Provisions a real Keycloak account for a newly-registered Patient or Organization. No forced
 * first-login password reset here (deliberately) - Keycloak's direct-grant flow (what this app's
 * login form uses) cannot fulfil any pending required action, so setting one just locks the
 * account out with an opaque "Account is not fully set up" error. The temp password is
 * immediately usable; a proper change-password UX is a follow-up, not this pass.
 */
class KeycloakAdminClient(
    private val adminServiceClient: ServiceToServiceClient,
    private val adminBaseUrl: String =
        System.getenv("KEYCLOAK_ADMIN_BASE_URL") ?: "http://localhost:8180/admin/realms/bima-care",
) {
    suspend fun provisionMemberAccount(patient: PatientSummary): String =
        provisionAccount(
            identity =
                AccountIdentity(
                    username = patient.phone,
                    // Patients/Organizations don't always have a real email - the realm's User
                    // Profile requires one anyway (or the account is flagged "not fully set up"
                    // and direct-grant login fails outright), so fall back to a placeholder
                    // that's never actually used to contact anyone.
                    email = patient.email ?: "${patient.phone}@members.bimacare.dev",
                    firstName = patient.firstName,
                    lastName = patient.lastName,
                ),
            attribute = AccountAttribute("patientId", patient.id.toString()),
            roleName = "Member",
        )

    suspend fun provisionInsurerAccount(organization: OrganizationSummary): String =
        provisionAccount(
            identity =
                AccountIdentity(
                    username = organization.phone,
                    email = organization.email ?: "${organization.phone}@members.bimacare.dev",
                    firstName = organization.name,
                    lastName = "",
                ),
            attribute = AccountAttribute("organizationId", organization.id.toString()),
            roleName = "Insurer",
        )

    private suspend fun provisionAccount(
        identity: AccountIdentity,
        attribute: AccountAttribute,
        roleName: String,
    ): String {
        val tempPassword = generateTempPassword()
        val createBody =
            buildJsonObject {
                put("username", identity.username)
                put("firstName", identity.firstName)
                put("lastName", identity.lastName)
                put("email", identity.email)
                put("emailVerified", true)
                put("enabled", true)
                put(
                    "attributes",
                    buildJsonObject {
                        put(attribute.key, buildJsonArray { add(JsonPrimitive(attribute.value)) })
                    },
                )
                put(
                    "credentials",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("type", "password")
                                put("value", tempPassword)
                                put("temporary", false)
                            },
                        )
                    },
                )
            }.toString()

        val createResponse = adminServiceClient.post("$adminBaseUrl/users", createBody)
        checkSuccess(createResponse) { "create Keycloak user for ${identity.username}" }
        val userId =
            createResponse.headers["Location"]?.substringAfterLast("/")
                ?: findUserIdByUsername(identity.username)

        assignRealmRole(userId, roleName)
        return tempPassword
    }

    private suspend fun findUserIdByUsername(username: String): String {
        val response = adminServiceClient.get("$adminBaseUrl/users?username=$username&exact=true")
        checkSuccess(response) { "look up Keycloak user by username $username" }
        return Json.parseToJsonElement(response.bodyAsText()).jsonArray
            .first().jsonObject.getValue("id").jsonPrimitive.content
    }

    private suspend fun assignRealmRole(
        userId: String,
        roleName: String,
    ) {
        val roleResponse = adminServiceClient.get("$adminBaseUrl/roles/$roleName")
        checkSuccess(roleResponse) { "fetch the $roleName role definition" }
        val response =
            adminServiceClient.post(
                "$adminBaseUrl/users/$userId/role-mappings/realm",
                "[${roleResponse.bodyAsText()}]",
            )
        checkSuccess(response) { "assign the $roleName role to user $userId" }
    }

    // ServiceToServiceClient/Ktor's default HttpClient doesn't throw on non-2xx responses, so
    // every admin API call here needs an explicit check - the bug this fixes: a 403 fetching the
    // Member role was silently wrapped into the next request's body and produced a confusing
    // downstream 400, instead of surfacing the real permission problem.
    private fun checkSuccess(
        response: HttpResponse,
        action: () -> String,
    ) {
        if (!response.status.isSuccess()) {
            error("Failed to ${action()}: HTTP ${response.status}")
        }
    }

    private fun generateTempPassword(): String =
        (1..TEMP_PASSWORD_LENGTH).map { PASSWORD_CHARS[Random.nextInt(PASSWORD_CHARS.size)] }.joinToString("")
}
