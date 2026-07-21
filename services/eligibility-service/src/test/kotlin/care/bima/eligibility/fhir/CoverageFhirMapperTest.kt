package care.bima.eligibility.fhir

import care.bima.eligibility.domain.Coverage
import care.bima.eligibility.domain.CoverageStatus
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CoverageFhirMapperTest {
    @Test
    fun mapsDomainCoverageToFhirCoverage() {
        val patientId = UUID.randomUUID()
        val insurerId = UUID.randomUUID()
        val coverage =
            Coverage(
                id = UUID.randomUUID(),
                patientId = patientId,
                insurerId = insurerId,
                status = CoverageStatus.ACTIVE,
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 12, 31),
                planTier = "gold",
            )

        val fhirCoverage = CoverageFhirMapper.toFhir(coverage)

        assertEquals(coverage.id.toString(), fhirCoverage.idElement.idPart)
        assertEquals("Patient/$patientId", fhirCoverage.beneficiary.reference)
        assertEquals("Organization/$insurerId", fhirCoverage.payorFirstRep.reference)
        assertEquals("gold", fhirCoverage.class_.first().value)
    }
}
