package care.bima.provider.fhir

import care.bima.provider.domain.Practitioner
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Practitioner as FhirPractitioner

object PractitionerFhirMapper {
    fun toFhir(practitioner: Practitioner): FhirPractitioner =
        FhirPractitioner().apply {
            id = practitioner.id.toString()
            addIdentifier(Identifier().setValue(practitioner.licenseNumber))
            addName(HumanName().addGiven(practitioner.firstName).setFamily(practitioner.lastName))
            addTelecom(ContactPoint().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(practitioner.phone))
            active = practitioner.isActive
        }
}
