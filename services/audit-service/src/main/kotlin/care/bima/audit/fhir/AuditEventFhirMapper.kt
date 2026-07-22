package care.bima.audit.fhir

import care.bima.audit.db.AuditRecord
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Reference
import java.time.Instant
import java.util.Date
import org.hl7.fhir.r4.model.AuditEvent as FhirAuditEvent

// AuditEvent.type/subtype have real FHIR value sets (DICOM audit message types) that don't map
// cleanly onto this platform's own Kafka topic names - out of scope for MVP, so `type` just
// carries the raw topic name under a platform-local code system instead of a spec value set.
object AuditEventFhirMapper {
    fun toFhir(record: AuditRecord): FhirAuditEvent =
        FhirAuditEvent().apply {
            id = record.id.toString()
            type = Coding().setSystem("http://bima.care/audit-event-type").setCode(record.eventType)
            recorded = Date.from(Instant.parse(record.occurredAt))
            addEntity().what = Reference(record.resourceId)
        }
}
