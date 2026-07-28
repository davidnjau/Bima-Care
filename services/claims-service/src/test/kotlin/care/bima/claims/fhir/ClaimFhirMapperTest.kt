package care.bima.claims.fhir

import care.bima.claims.domain.Claim
import care.bima.claims.domain.ClaimStatus
import care.bima.claims.domain.ClaimType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ClaimFhirMapperTest {
    @Test
    fun mapsDomainClaimToFhirClaim() {
        val patientId = UUID.randomUUID()
        val practitionerId = UUID.randomUUID()
        val coverageId = UUID.randomUUID()
        val claim =
            Claim(
                id = UUID.randomUUID(),
                patientId = patientId,
                encounterId = UUID.randomUUID(),
                coverageId = coverageId,
                practitionerId = practitionerId,
                organizationId = UUID.randomUUID(),
                serviceType = "Outpatient",
                diagnosisCode = "A09",
                treatmentDetails = "Rehydration therapy",
                requestedAmount = BigDecimal("8500.00"),
                approvedAmount = null,
                status = ClaimStatus.SUBMITTED,
                claimType = ClaimType.PROVIDER_SUBMITTED,
                dateOfService = null,
                claimFormDocumentId = null,
                itemizedReceiptDocumentId = null,
                etrDocumentId = null,
                submittedAt = LocalDateTime.of(2026, 3, 15, 10, 0),
                adjudicatedAt = null,
            )

        val fhirClaim = ClaimFhirMapper.toFhir(claim)

        assertEquals(claim.id.toString(), fhirClaim.idElement.idPart)
        assertEquals("Patient/$patientId", fhirClaim.patient.reference)
        assertEquals("Practitioner/$practitionerId", fhirClaim.provider.reference)
        assertEquals("Coverage/$coverageId", fhirClaim.insuranceFirstRep.coverage.reference)
        assertEquals(0, claim.requestedAmount.compareTo(fhirClaim.total.value))
    }

    @Test
    fun fallsBackToOrganizationProviderForReimbursementClaims() {
        val patientId = UUID.randomUUID()
        val organizationId = UUID.randomUUID()
        val coverageId = UUID.randomUUID()
        val claim =
            Claim(
                id = UUID.randomUUID(),
                patientId = patientId,
                encounterId = UUID.randomUUID(),
                coverageId = coverageId,
                practitionerId = null,
                organizationId = organizationId,
                serviceType = "Outpatient",
                diagnosisCode = "A09",
                treatmentDetails = "Paid cash, seeking reimbursement",
                requestedAmount = BigDecimal("4200.00"),
                approvedAmount = null,
                status = ClaimStatus.SUBMITTED,
                claimType = ClaimType.MEMBER_REIMBURSEMENT,
                dateOfService = LocalDate.of(2026, 3, 10),
                claimFormDocumentId = UUID.randomUUID(),
                itemizedReceiptDocumentId = UUID.randomUUID(),
                etrDocumentId = UUID.randomUUID(),
                submittedAt = LocalDateTime.of(2026, 3, 15, 10, 0),
                adjudicatedAt = null,
            )

        val fhirClaim = ClaimFhirMapper.toFhir(claim)

        assertEquals("Organization/$organizationId", fhirClaim.provider.reference)
        assertEquals("MEMBER_REIMBURSEMENT", fhirClaim.type.text)
    }
}
