package care.bima.document.fhir

import care.bima.document.domain.Document
import care.bima.document.domain.DocumentStatus
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.Reference
import java.time.ZoneId
import java.util.Date
import org.hl7.fhir.r4.model.DocumentReference as FhirDocumentReference
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus as FhirDocumentReferenceStatus

object DocumentReferenceFhirMapper {
    fun toFhir(document: Document): FhirDocumentReference =
        FhirDocumentReference().apply {
            id = document.id.toString()
            subject = Reference("Patient/${document.patientId}")
            status =
                when (document.status) {
                    DocumentStatus.CURRENT -> FhirDocumentReferenceStatus.CURRENT
                    DocumentStatus.SUPERSEDED -> FhirDocumentReferenceStatus.SUPERSEDED
                    DocumentStatus.ENTERED_IN_ERROR -> FhirDocumentReferenceStatus.ENTEREDINERROR
                }
            date = Date.from(document.createdAt.atZone(ZoneId.systemDefault()).toInstant())
            addContent().apply {
                attachment =
                    Attachment().apply {
                        contentType = document.contentType
                        title = document.title
                    }
            }
        }
}
