package care.bima.consent.fhir

import care.bima.consent.domain.Consent
import care.bima.consent.domain.ConsentStatus
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ConsentFhirMapperTest {
    @Test
    fun mapsDomainConsentToFhirConsent() {
        val patientId = UUID.randomUUID()
        val granteeId = UUID.randomUUID()
        val consent =
            Consent(
                id = UUID.randomUUID(),
                patientId = patientId,
                granteeId = granteeId,
                scope = "read:Claim",
                status = ConsentStatus.ACTIVE,
                createdAt = LocalDateTime.of(2026, 1, 1, 9, 0),
            )

        val fhirConsent = ConsentFhirMapper.toFhir(consent)

        assertEquals(consent.id.toString(), fhirConsent.idElement.idPart)
        assertEquals("Patient/$patientId", fhirConsent.patient.reference)
        assertEquals("read:Claim", fhirConsent.scope.text)
        assertEquals(granteeId.toString(), fhirConsent.provision.actorFirstRep.reference.reference)
    }
}
