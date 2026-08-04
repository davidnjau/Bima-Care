package care.bima.patient.fhir

import care.bima.patient.domain.Dependent
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson

object RelatedPersonFhirMapper {
    fun toFhir(dependent: Dependent): RelatedPerson =
        RelatedPerson().apply {
            id = dependent.id.toString()
            patient = Reference("Patient/${dependent.primaryPatientId}")
            addRelationship(CodeableConcept().setText(dependent.relationship.name))
            // The dependent is also their own full Patient record (so they can log in and raise
            // claims independently) - this identifier links back to it, since core FHIR
            // RelatedPerson has no dedicated field for "this related person is also Patient X".
            addIdentifier(
                Identifier().apply {
                    system = "urn:bima-care:patientId"
                    value = dependent.dependentPatientId.toString()
                },
            )
        }
}
