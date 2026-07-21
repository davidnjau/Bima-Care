package care.bima.patient.fhir

import care.bima.patient.domain.Gender
import care.bima.patient.domain.Patient
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PatientFhirMapperTest {
    @Test
    fun mapsDomainPatientToFhirPatient() {
        val patient =
            Patient(
                id = UUID.randomUUID(),
                nationalId = "12345678",
                firstName = "Asha",
                lastName = "Otieno",
                phone = "+254700000000",
                gender = Gender.FEMALE,
                dob = LocalDate.of(1990, 5, 1),
            )

        val fhirPatient = PatientFhirMapper.toFhir(patient)

        assertEquals(patient.id.toString(), fhirPatient.idElement.idPart)
        assertEquals("Otieno", fhirPatient.nameFirstRep.family)
        assertEquals("Asha", fhirPatient.nameFirstRep.givenAsSingleString)
        assertEquals("12345678", fhirPatient.identifierFirstRep.value)
        assertEquals(true, fhirPatient.active)
    }
}
