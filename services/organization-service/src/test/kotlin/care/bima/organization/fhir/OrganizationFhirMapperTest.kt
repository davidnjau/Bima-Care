package care.bima.organization.fhir

import care.bima.organization.domain.Organization
import care.bima.organization.domain.OrganizationType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class OrganizationFhirMapperTest {
    @Test
    fun mapsDomainOrganizationToFhirOrganization() {
        val organization =
            Organization(
                id = UUID.randomUUID(),
                registrationNumber = "REG-9001",
                name = "Nairobi General Hospital",
                type = OrganizationType.HOSPITAL,
                phone = "+254722222222",
                address = "Nairobi, Kenya",
            )

        val fhirOrganization = OrganizationFhirMapper.toFhir(organization)

        assertEquals(organization.id.toString(), fhirOrganization.idElement.idPart)
        assertEquals("Nairobi General Hospital", fhirOrganization.name)
        assertEquals("REG-9001", fhirOrganization.identifierFirstRep.value)
        assertEquals(true, fhirOrganization.active)
    }
}
