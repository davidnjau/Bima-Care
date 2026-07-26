package care.bima.eligibility.fhir

import care.bima.eligibility.domain.Policy
import care.bima.eligibility.domain.PolicyStatus
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus
import org.hl7.fhir.r4.model.InsurancePlan
import org.hl7.fhir.r4.model.Money
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import java.time.ZoneId
import java.util.Date

object PolicyFhirMapper {
    fun toFhir(policy: Policy): InsurancePlan =
        InsurancePlan().apply {
            id = policy.id.toString()
            name = policy.name
            ownedBy = Reference("Organization/${policy.insurerId}")
            administeredBy = Reference("Organization/${policy.insurerId}")
            addType(CodeableConcept().setText(policy.type.name))
            period =
                Period().apply {
                    start = Date.from(policy.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    policy.endDate?.let { end = Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant()) }
                }
            status =
                when (policy.status) {
                    PolicyStatus.ACTIVE -> PublicationStatus.ACTIVE
                    // FHIR's InsurancePlan.status (PublicationStatus) has no direct "suspended"
                    // value - draft|active|retired|unknown is the full set, so both a suspended
                    // and an expired policy map to "retired" here as the closest fit.
                    PolicyStatus.SUSPENDED, PolicyStatus.EXPIRED -> PublicationStatus.RETIRED
                }
            addPlan(
                InsurancePlan.InsurancePlanPlanComponent().apply {
                    addGeneralCost(
                        InsurancePlan.InsurancePlanPlanGeneralCostComponent().apply {
                            cost = Money().setValue(policy.premium)
                        },
                    )
                },
            )
        }
}
