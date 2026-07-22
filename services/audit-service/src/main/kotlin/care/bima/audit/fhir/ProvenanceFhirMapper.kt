package care.bima.audit.fhir

import care.bima.audit.db.AuditRecord
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Reference
import java.time.Instant
import java.util.Date
import org.hl7.fhir.r4.model.Provenance as FhirProvenance

object ProvenanceFhirMapper {
    fun toFhir(record: AuditRecord): FhirProvenance =
        FhirProvenance().apply {
            id = record.id.toString()
            recorded = Date.from(Instant.parse(record.occurredAt))
            addTarget(Reference(record.resourceId))
            activity = CodeableConcept().setText(record.eventType)
        }
}
