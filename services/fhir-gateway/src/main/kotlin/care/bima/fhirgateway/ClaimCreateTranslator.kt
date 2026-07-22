package care.bima.fhirgateway

import care.bima.shared.fhir.FhirContextProvider
import care.bima.shared.service.ValidationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.hl7.fhir.r4.model.Claim

// FHIR's Claim resource has no single canonical "free text treatment details" field for our
// simplified MVP shape - Claim.supportingInfo (designed to carry arbitrary contextual info)
// is the closest fit, so an external submitter is expected to put treatment details there
// as a valueString.
object ClaimCreateTranslator {
    fun toSubmitClaimRequestJson(fhirJson: String): String {
        val claim = FhirContextProvider.decodeFromJson(fhirJson, Claim::class.java)

        val patientId =
            claim.patient?.reference?.substringAfter("Patient/")
                ?: throw ValidationException("Claim.patient.reference is required")
        val serviceType =
            claim.item.firstOrNull()?.productOrService?.text
                ?: throw ValidationException("Claim.item[0].productOrService.text is required")
        val diagnosisCode =
            claim.diagnosis.firstOrNull()?.diagnosisCodeableConcept?.text
                ?: throw ValidationException("Claim.diagnosis[0].diagnosisCodeableConcept.text is required")
        val treatmentDetails =
            claim.supportingInfo.firstOrNull()?.takeIf { it.hasValueStringType() }?.valueStringType?.value
                ?: throw ValidationException(
                    "Claim.supportingInfo[0].valueString is required (used as treatment details)",
                )
        val amount =
            claim.total?.value
                ?: throw ValidationException("Claim.total.value is required")

        return buildJsonObject {
            put("patientId", patientId)
            put("serviceType", serviceType)
            put("diagnosisCode", diagnosisCode)
            put("treatmentDetails", treatmentDetails)
            put("amount", amount.toPlainString())
        }.toString()
    }
}
