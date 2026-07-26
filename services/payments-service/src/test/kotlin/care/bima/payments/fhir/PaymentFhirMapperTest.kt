package care.bima.payments.fhir

import care.bima.payments.domain.Payment
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentFhirMapperTest {
    @Test
    fun mapsDomainPaymentToFhirPaymentNotice() {
        val claimId = UUID.randomUUID()
        val payment =
            Payment(
                id = UUID.randomUUID(),
                claimId = claimId,
                patientId = UUID.randomUUID(),
                amount = BigDecimal("8500.00"),
                releasedAt = LocalDateTime.of(2026, 3, 20, 9, 0),
            )

        val fhirPaymentNotice = PaymentFhirMapper.toFhir(payment)

        assertEquals(payment.id.toString(), fhirPaymentNotice.idElement.idPart)
        assertEquals("Claim/$claimId", fhirPaymentNotice.request.reference)
        assertEquals(0, payment.amount.compareTo(fhirPaymentNotice.amount.value))
    }
}
