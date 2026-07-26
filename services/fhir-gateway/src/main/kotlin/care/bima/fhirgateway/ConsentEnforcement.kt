package care.bima.fhirgateway

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private fun ApplicationCall.granteeId(): String? = principal<JWTPrincipal>()?.payload?.subject

// Extracts the patientId a FHIR resource is about, so it can be checked against consent-service.
// Fails closed (returns null, which callers treat as "deny") if a patient-scoped resource type
// can't be resolved to a patient - matches the zero-trust posture in BIMA CARE.md §10 rather
// than silently letting ungated data through.
private fun extractPatientId(
    resourceType: String,
    fhirJson: String,
): String? {
    val resource = Json.parseToJsonElement(fhirJson).jsonObject
    return if (resourceType == "Patient") {
        resource["id"]?.jsonPrimitive?.content
    } else {
        val reference =
            patientReferenceField(resourceType)
                ?.let { resource[it]?.jsonObject?.get("reference")?.jsonPrimitive?.content }
        reference?.substringAfter("Patient/")
    }
}

suspend fun isConsented(
    fhirJson: String,
    resourceType: String,
    call: ApplicationCall,
    consentGateClient: ConsentGateClient,
    authHeader: String?,
): Boolean {
    val granteeId = call.granteeId()
    val patientId = granteeId?.let { extractPatientId(resourceType, fhirJson) }
    return granteeId != null && patientId != null &&
        consentGateClient.hasActiveConsent(granteeId, patientId, authHeader)
}

suspend fun filterByConsent(
    resources: List<String>,
    resourceType: String,
    call: ApplicationCall,
    consentGateClient: ConsentGateClient,
    authHeader: String?,
): List<String> {
    if (resourceType !in PATIENT_SCOPED_RESOURCE_TYPES) return resources
    return resources.filter { isConsented(it, resourceType, call, consentGateClient, authHeader) }
}
