package care.bima.eligibility.fhir

import care.bima.eligibility.domain.Coverage
import care.bima.eligibility.domain.CoverageStatus
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import java.time.ZoneId
import java.util.Date
import org.hl7.fhir.r4.model.Coverage as FhirCoverage
import org.hl7.fhir.r4.model.Coverage.CoverageStatus as FhirCoverageStatus

object CoverageFhirMapper {
    fun toFhir(coverage: Coverage): FhirCoverage =
        FhirCoverage().apply {
            id = coverage.id.toString()
            beneficiary = Reference("Patient/${coverage.patientId}")
            payor = listOf(Reference("Organization/${coverage.insurerId}"))
            status =
                when (coverage.status) {
                    CoverageStatus.ACTIVE -> FhirCoverageStatus.ACTIVE
                    CoverageStatus.CANCELLED -> FhirCoverageStatus.CANCELLED
                    CoverageStatus.DRAFT -> FhirCoverageStatus.DRAFT
                    CoverageStatus.ENTERED_IN_ERROR -> FhirCoverageStatus.ENTEREDINERROR
                }
            period =
                Period().apply {
                    start = Date.from(coverage.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    coverage.endDate?.let { end = Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant()) }
                }
            addClass_().apply {
                type = CodeableConcept().setText("plan-tier")
                value = coverage.planTier
            }
        }
}
