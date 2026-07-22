package care.bima.audit.fhir

import care.bima.audit.db.AuditRecord
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class AuditEventFhirMapperTest {
    @Test
    fun mapsAuditRecordToFhirAuditEventAndProvenance() {
        val resourceId = UUID.randomUUID().toString()
        val record =
            AuditRecord(
                id = UUID.randomUUID(),
                eventType = "claim.adjudicated",
                resourceId = resourceId,
                version = 1,
                occurredAt = "2026-03-15T10:00:00Z",
                payload = """{"status":"APPROVED"}""",
            )

        val auditEvent = AuditEventFhirMapper.toFhir(record)
        assertEquals(record.id.toString(), auditEvent.idElement.idPart)
        assertEquals("claim.adjudicated", auditEvent.type.code)
        assertEquals(resourceId, auditEvent.entityFirstRep.what.reference)

        val provenance = ProvenanceFhirMapper.toFhir(record)
        assertEquals(record.id.toString(), provenance.idElement.idPart)
        assertEquals(resourceId, provenance.targetFirstRep.reference)
        assertEquals("claim.adjudicated", provenance.activity.text)
    }
}
