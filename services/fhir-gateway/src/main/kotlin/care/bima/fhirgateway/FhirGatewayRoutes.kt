package care.bima.fhirgateway

import care.bima.shared.service.ErrorResponse
import care.bima.shared.service.NotFoundException
import care.bima.shared.service.ValidationException
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.toMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val WRITABLE_RESOURCE_TYPE = "Claim"

class FhirGatewayDependencies(
    val client: HttpClient,
    val consentGateClient: ConsentGateClient,
    val mappings: Map<String, FhirResourceMapping> = resourceMappings().associateBy { it.resourceType },
)

fun Routing.fhirGatewayRoutes(
    client: HttpClient,
    consentGateClient: ConsentGateClient,
) {
    val deps = FhirGatewayDependencies(client, consentGateClient)

    authenticate("keycloak") {
        route("/{resourceType}") {
            get { searchResources(call, deps) }
            get("/{id}") { getResource(call, deps) }
            post { createResource(call, deps) }
        }
    }
}

private suspend fun searchResources(
    call: ApplicationCall,
    deps: FhirGatewayDependencies,
) {
    val mapping = mappingFor(call.parameters["resourceType"], deps.mappings)
    val authHeader = call.request.headers["Authorization"]
    val ids = fetchResourceIds(deps.client, mapping, call.request.queryParameters.toMap(), authHeader)
    val resources = ids.map { id -> fetchFhirResource(deps.client, mapping, id, authHeader) }
    val visible = filterByConsent(resources, mapping.resourceType, call, deps.consentGateClient, authHeader)
    call.respondText(BundleBuilder.searchset(visible), ContentType.Application.Json)
}

private suspend fun getResource(
    call: ApplicationCall,
    deps: FhirGatewayDependencies,
) {
    val mapping = mappingFor(call.parameters["resourceType"], deps.mappings)
    val id = call.parameters["id"] ?: throw ValidationException("Missing resource id")
    val authHeader = call.request.headers["Authorization"]
    val response =
        deps.client.get("${mapping.internalBaseUrl}${mapping.internalPluralPath}/$id/fhir") {
            authHeader?.let { header("Authorization", it) }
        }
    if (response.status == HttpStatusCode.NotFound) {
        throw NotFoundException("${mapping.resourceType} $id not found")
    }
    val fhirJson = response.bodyAsText()
    if (mapping.resourceType in PATIENT_SCOPED_RESOURCE_TYPES) {
        val allowed = isConsented(fhirJson, mapping.resourceType, call, deps.consentGateClient, authHeader)
        if (!allowed) {
            call.respond(
                HttpStatusCode.Forbidden,
                ErrorResponse("No active consent covers this ${mapping.resourceType} for this requester"),
            )
            return
        }
    }
    call.respondText(fhirJson, ContentType.Application.Json, response.status)
}

private suspend fun createResource(
    call: ApplicationCall,
    deps: FhirGatewayDependencies,
) {
    val resourceType = call.parameters["resourceType"]
    if (resourceType != WRITABLE_RESOURCE_TYPE) {
        throw ValidationException(
            "Creating $resourceType externally isn't supported yet - only $WRITABLE_RESOURCE_TYPE is",
        )
    }
    val mapping = deps.mappings.getValue(WRITABLE_RESOURCE_TYPE)
    val internalRequestJson = ClaimCreateTranslator.toSubmitClaimRequestJson(call.receiveText())
    val authHeader = call.request.headers["Authorization"]

    val createResponse =
        deps.client.post("${mapping.internalBaseUrl}${mapping.internalPluralPath}") {
            authHeader?.let { header("Authorization", it) }
            contentType(ContentType.Application.Json)
            setBody(internalRequestJson)
        }
    if (createResponse.status != HttpStatusCode.Created) {
        call.respondText(createResponse.bodyAsText(), ContentType.Application.Json, createResponse.status)
        return
    }

    val createdId =
        Json.parseToJsonElement(createResponse.bodyAsText()).jsonObject.getValue("id").jsonPrimitive.content
    val fhirResponse =
        deps.client.get("${mapping.internalBaseUrl}${mapping.internalPluralPath}/$createdId/fhir") {
            authHeader?.let { header("Authorization", it) }
        }
    call.respondText(fhirResponse.bodyAsText(), ContentType.Application.Json, HttpStatusCode.Created)
}

private fun mappingFor(
    resourceType: String?,
    mappings: Map<String, FhirResourceMapping>,
): FhirResourceMapping = mappings[resourceType] ?: throw NotFoundException("Unknown FHIR resource type: $resourceType")

private suspend fun fetchResourceIds(
    client: HttpClient,
    mapping: FhirResourceMapping,
    queryParams: Map<String, List<String>>,
    authHeader: String?,
): List<String> {
    val translated = translateSearchParams(queryParams)
    val query = translated.entries.joinToString("&") { (k, v) -> "$k=$v" }
    val url = "${mapping.internalBaseUrl}${mapping.internalPluralPath}" + if (query.isNotEmpty()) "?$query" else ""
    val response = client.get(url) { authHeader?.let { header("Authorization", it) } }
    return Json.parseToJsonElement(response.bodyAsText()).jsonArray
        .mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
}

private suspend fun fetchFhirResource(
    client: HttpClient,
    mapping: FhirResourceMapping,
    id: String,
    authHeader: String?,
): String {
    val response: HttpResponse =
        client.get("${mapping.internalBaseUrl}${mapping.internalPluralPath}/$id/fhir") {
            authHeader?.let { header("Authorization", it) }
        }
    return response.bodyAsText()
}
