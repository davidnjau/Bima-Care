package care.bima.encounter.fhir

import care.bima.encounter.domain.Encounter
import care.bima.encounter.domain.EncounterStatus
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import java.time.ZoneId
import java.util.Date
import org.hl7.fhir.r4.model.Encounter as FhirEncounter
import org.hl7.fhir.r4.model.Encounter.EncounterStatus as FhirEncounterStatus

object EncounterFhirMapper {
    fun toFhir(encounter: Encounter): FhirEncounter =
        FhirEncounter().apply {
            id = encounter.id.toString()
            subject = Reference("Patient/${encounter.patientId}")
            serviceProvider = Reference("Organization/${encounter.organizationId}")
            addParticipant().individual = Reference("Practitioner/${encounter.practitionerId}")
            status =
                when (encounter.status) {
                    EncounterStatus.IN_PROGRESS -> FhirEncounterStatus.INPROGRESS
                    EncounterStatus.FINISHED -> FhirEncounterStatus.FINISHED
                    EncounterStatus.CANCELLED -> FhirEncounterStatus.CANCELLED
                }
            period =
                Period().apply {
                    start = Date.from(encounter.startedAt.atZone(ZoneId.systemDefault()).toInstant())
                    encounter.endedAt?.let { end = Date.from(it.atZone(ZoneId.systemDefault()).toInstant()) }
                }
        }
}
