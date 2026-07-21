package care.bima.provider.fhir

import care.bima.provider.domain.Practitioner
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PractitionerFhirMapperTest {
    @Test
    fun mapsDomainPractitionerToFhirPractitioner() {
        val practitioner =
            Practitioner(
                id = UUID.randomUUID(),
                licenseNumber = "MP-12345",
                firstName = "John",
                lastName = "Mwangi",
                phone = "+254711111111",
                specialty = "General Practice",
            )

        val fhirPractitioner = PractitionerFhirMapper.toFhir(practitioner)

        assertEquals(practitioner.id.toString(), fhirPractitioner.idElement.idPart)
        assertEquals("Mwangi", fhirPractitioner.nameFirstRep.family)
        assertEquals("MP-12345", fhirPractitioner.identifierFirstRep.value)
        assertEquals(true, fhirPractitioner.active)
    }
}
