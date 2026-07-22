package care.bima.payments.fhir

import care.bima.payments.domain.Payment
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Money
import org.hl7.fhir.r4.model.PaymentNotice
import org.hl7.fhir.r4.model.Reference
import java.time.ZoneId
import java.util.Date

object PaymentFhirMapper {
    fun toFhir(payment: Payment): PaymentNotice =
        PaymentNotice().apply {
            id = payment.id.toString()
            request = Reference("Claim/${payment.claimId}")
            amount = Money().setValue(payment.amount)
            paymentStatus = CodeableConcept().setText("paid")
            created = Date.from(payment.releasedAt.atZone(ZoneId.systemDefault()).toInstant())
        }
}
