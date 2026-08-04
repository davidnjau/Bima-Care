package care.bima.patient.fhir

import care.bima.patient.domain.Dependent
import care.bima.patient.domain.RelationshipType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class RelatedPersonFhirMapperTest {
    @Test
    fun mapsDomainDependentToFhirRelatedPerson() {
        val primaryPatientId = UUID.randomUUID()
        val dependentPatientId = UUID.randomUUID()
        val dependent =
            Dependent(
                id = UUID.randomUUID(),
                primaryPatientId = primaryPatientId,
                dependentPatientId = dependentPatientId,
                relationship = RelationshipType.CHILD,
            )

        val fhirRelatedPerson = RelatedPersonFhirMapper.toFhir(dependent)

        assertEquals(dependent.id.toString(), fhirRelatedPerson.idElement.idPart)
        assertEquals("Patient/$primaryPatientId", fhirRelatedPerson.patient.reference)
        assertEquals("CHILD", fhirRelatedPerson.relationshipFirstRep.text)
        assertEquals(dependentPatientId.toString(), fhirRelatedPerson.identifierFirstRep.value)
    }
}
