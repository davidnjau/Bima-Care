package care.bima.sync

import care.bima.shared.events.Topics

data class RegistrationMapping(
    val topic: String,
    val fhirResourceType: String,
    val internalBaseUrl: String,
    val internalPluralPath: String,
)

fun registrationMappings(): List<RegistrationMapping> =
    listOf(
        RegistrationMapping(
            topic = Topics.PATIENT_CREATED,
            fhirResourceType = "Patient",
            internalBaseUrl = System.getenv("PATIENT_SERVICE_URL") ?: "http://localhost:8086",
            internalPluralPath = "/patients",
        ),
        RegistrationMapping(
            topic = Topics.PRACTITIONER_CREATED,
            fhirResourceType = "Practitioner",
            internalBaseUrl = System.getenv("PROVIDER_SERVICE_URL") ?: "http://localhost:8082",
            internalPluralPath = "/practitioners",
        ),
        RegistrationMapping(
            topic = Topics.ORGANIZATION_CREATED,
            fhirResourceType = "Organization",
            internalBaseUrl = System.getenv("ORGANIZATION_SERVICE_URL") ?: "http://localhost:8083",
            internalPluralPath = "/organizations",
        ),
    )
