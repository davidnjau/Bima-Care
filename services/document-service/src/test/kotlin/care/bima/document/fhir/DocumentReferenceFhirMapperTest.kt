package care.bima.document.fhir

import care.bima.document.domain.Document
import care.bima.document.domain.DocumentStatus
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentReferenceFhirMapperTest {
    @Test
    fun mapsDomainDocumentToFhirDocumentReference() {
        val patientId = UUID.randomUUID()
        val document =
            Document(
                id = UUID.randomUUID(),
                patientId = patientId,
                contentType = "application/pdf",
                title = "Lab report - full blood count",
                category = "lab-report",
                storageKey = "$patientId/some-key",
                status = DocumentStatus.CURRENT,
                createdAt = LocalDateTime.of(2026, 3, 15, 10, 0),
            )

        val fhirDocumentReference = DocumentReferenceFhirMapper.toFhir(document)

        assertEquals(document.id.toString(), fhirDocumentReference.idElement.idPart)
        assertEquals("Patient/$patientId", fhirDocumentReference.subject.reference)
        assertEquals("application/pdf", fhirDocumentReference.contentFirstRep.attachment.contentType)
        assertEquals("Lab report - full blood count", fhirDocumentReference.contentFirstRep.attachment.title)
    }
}
