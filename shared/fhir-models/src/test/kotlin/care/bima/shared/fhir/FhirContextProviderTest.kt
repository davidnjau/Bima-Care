package care.bima.shared.fhir

import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import kotlin.test.Test
import kotlin.test.assertEquals

class FhirContextProviderTest {
    @Test
    fun roundTripsPatientThroughJson() {
        val patient =
            Patient().apply {
                addName(HumanName().addGiven("Asha").setFamily("Otieno"))
            }

        val json = FhirContextProvider.encodeToJson(patient)
        val decoded = FhirContextProvider.decodeFromJson(json, Patient::class.java)

        assertEquals("Otieno", decoded.nameFirstRep.family)
        assertEquals("Asha", decoded.nameFirstRep.givenAsSingleString)
    }
}
