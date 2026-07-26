package care.bima.payments.domain

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class Payment(
    val id: UUID,
    val claimId: UUID,
    val patientId: UUID,
    val amount: BigDecimal,
    val releasedAt: LocalDateTime,
)
