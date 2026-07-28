package care.bima.claims.fhir

import care.bima.claims.domain.Claim
import care.bima.claims.domain.ClaimStatus
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Money
import org.hl7.fhir.r4.model.Reference
import java.time.ZoneId
import java.util.Date
import org.hl7.fhir.r4.model.Claim as FhirClaim
import org.hl7.fhir.r4.model.Claim.ClaimStatus as FhirClaimStatus

object ClaimFhirMapper {
    fun toFhir(claim: Claim): FhirClaim =
        FhirClaim().apply {
            id = claim.id.toString()
            patient = Reference("Patient/${claim.patientId}")
            // MEMBER_REIMBURSEMENT claims have no linked Practitioner - fall back to the
            // Organization the member named as the responsible party.
            provider =
                if (claim.practitionerId != null) {
                    Reference("Practitioner/${claim.practitionerId}")
                } else {
                    Reference("Organization/${claim.organizationId}")
                }
            type = CodeableConcept().setText(claim.claimType.name)
            insurance.add(
                FhirClaim.InsuranceComponent().apply {
                    coverage = Reference("Coverage/${claim.coverageId}")
                    sequence = 1
                    focal = true
                },
            )
            created = Date.from(claim.submittedAt.atZone(ZoneId.systemDefault()).toInstant())
            status =
                when (claim.status) {
                    ClaimStatus.REJECTED -> FhirClaimStatus.CANCELLED
                    else -> FhirClaimStatus.ACTIVE
                }
            total = Money().setValue(claim.requestedAmount)
        }
}
