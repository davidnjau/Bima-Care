package care.bima.organization.fhir

import care.bima.organization.domain.Organization
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Organization as FhirOrganization

object OrganizationFhirMapper {
    fun toFhir(organization: Organization): FhirOrganization =
        FhirOrganization().apply {
            id = organization.id.toString()
            addIdentifier(Identifier().setValue(organization.registrationNumber))
            name = organization.name
            addType(CodeableConcept().setText(organization.type.name))
            addTelecom(ContactPoint().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(organization.phone))
            addAddress(Address().setText(organization.address))
            active = organization.isActive
        }
}
