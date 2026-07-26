package care.bima.encounter.fhir

import care.bima.encounter.domain.Encounter
import care.bima.encounter.domain.EncounterStatus
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EncounterFhirMapperTest {
    @Test
    fun mapsDomainEncounterToFhirEncounter() {
        val patientId = UUID.randomUUID()
        val practitionerId = UUID.randomUUID()
        val organizationId = UUID.randomUUID()
        val encounter =
            Encounter(
                id = UUID.randomUUID(),
                patientId = patientId,
                practitionerId = practitionerId,
                organizationId = organizationId,
                status = EncounterStatus.IN_PROGRESS,
                startedAt = LocalDateTime.of(2026, 1, 1, 9, 0),
                endedAt = null,
            )

        val fhirEncounter = EncounterFhirMapper.toFhir(encounter)

        assertEquals(encounter.id.toString(), fhirEncounter.idElement.idPart)
        assertEquals("Patient/$patientId", fhirEncounter.subject.reference)
        assertEquals("Organization/$organizationId", fhirEncounter.serviceProvider.reference)
        assertEquals("Practitioner/$practitionerId", fhirEncounter.participantFirstRep.individual.reference)
    }
}
