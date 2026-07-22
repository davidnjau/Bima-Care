package care.bima.consent.fhir

import care.bima.consent.domain.Consent
import care.bima.consent.domain.ConsentStatus
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Reference
import java.time.ZoneId
import java.util.Date
import org.hl7.fhir.r4.model.Consent as FhirConsent
import org.hl7.fhir.r4.model.Consent.ConsentState as FhirConsentState

object ConsentFhirMapper {
    fun toFhir(consent: Consent): FhirConsent =
        FhirConsent().apply {
            id = consent.id.toString()
            patient = Reference("Patient/${consent.patientId}")
            status =
                when (consent.status) {
                    ConsentStatus.ACTIVE -> FhirConsentState.ACTIVE
                    ConsentStatus.REVOKED -> FhirConsentState.INACTIVE
                }
            scope = CodeableConcept().setText(consent.scope)
            dateTime = Date.from(consent.createdAt.atZone(ZoneId.systemDefault()).toInstant())
            // The grantee could be a Practitioner, Organization, or ExternalPartner - we don't
            // track which, so this is a bare, untyped reference rather than "ResourceType/id".
            provision.addActor().reference = Reference(consent.granteeId.toString())
        }
}
