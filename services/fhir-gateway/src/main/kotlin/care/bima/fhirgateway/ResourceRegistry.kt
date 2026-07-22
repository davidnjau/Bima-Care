package care.bima.fhirgateway

data class FhirResourceMapping(
    val resourceType: String,
    val internalBaseUrl: String,
    val internalPluralPath: String,
)

fun resourceMappings(): List<FhirResourceMapping> =
    listOf(
        FhirResourceMapping(
            "Patient",
            System.getenv("PATIENT_SERVICE_URL") ?: "http://localhost:8086",
            "/patients",
        ),
        FhirResourceMapping(
            "Practitioner",
            System.getenv("PROVIDER_SERVICE_URL") ?: "http://localhost:8082",
            "/practitioners",
        ),
        FhirResourceMapping(
            "Organization",
            System.getenv("ORGANIZATION_SERVICE_URL") ?: "http://localhost:8083",
            "/organizations",
        ),
        FhirResourceMapping(
            "Coverage",
            System.getenv("ELIGIBILITY_SERVICE_URL") ?: "http://localhost:8084",
            "/coverages",
        ),
        FhirResourceMapping(
            "Encounter",
            System.getenv("ENCOUNTER_SERVICE_URL") ?: "http://localhost:8087",
            "/encounters",
        ),
        FhirResourceMapping(
            "Claim",
            System.getenv("CLAIMS_SERVICE_URL") ?: "http://localhost:8088",
            "/claims",
        ),
        FhirResourceMapping(
            "PaymentNotice",
            System.getenv("PAYMENTS_SERVICE_URL") ?: "http://localhost:8089",
            "/payments",
        ),
        FhirResourceMapping(
            "Consent",
            System.getenv("CONSENT_SERVICE_URL") ?: "http://localhost:8092",
            "/consents",
        ),
        FhirResourceMapping(
            "DocumentReference",
            System.getenv("DOCUMENT_SERVICE_URL") ?: "http://localhost:8093",
            "/documents",
        ),
    )

// Best-effort translation of common FHIR search params to each internal service's own query
// param names - internal services already support filtering (see e.g. ClaimRoutes.kt's
// status/patientId/organizationId params), this just bridges the naming gap for the handful
// of params that are actually useful to search on. Anything not listed here is dropped rather
// than forwarded verbatim, since forwarding e.g. FHIR's `_count`/`_sort` to an internal service
// that ignores them silently would look supported when it isn't.
private val SEARCH_PARAM_TRANSLATIONS =
    mapOf(
        "patient" to "patientId",
        "subject" to "patientId",
        "organization" to "organizationId",
        "status" to "status",
    )

fun translateSearchParams(incoming: Map<String, List<String>>): Map<String, String> =
    incoming.entries
        .mapNotNull { (key, values) ->
            val internalKey = SEARCH_PARAM_TRANSLATIONS[key] ?: return@mapNotNull null
            val value = values.firstOrNull() ?: return@mapNotNull null
            internalKey to value.substringAfter('/')
        }
        .toMap()

// Resource types that carry patient data and must be consent-gated before fhir-gateway returns
// them externally. PaymentNotice/Organization/Practitioner/Consent are deliberately excluded:
// PaymentNotice only references a Claim (no direct patient link, would need an extra hop to
// chase), and Organization/Practitioner/Consent aren't patient clinical/financial records.
val PATIENT_SCOPED_RESOURCE_TYPES = setOf("Patient", "Coverage", "Encounter", "Claim", "DocumentReference")

private val PATIENT_REFERENCE_FIELD =
    mapOf(
        "Coverage" to "beneficiary",
        "Encounter" to "subject",
        "Claim" to "patient",
        "DocumentReference" to "subject",
    )

fun patientReferenceField(resourceType: String): String? = PATIENT_REFERENCE_FIELD[resourceType]
