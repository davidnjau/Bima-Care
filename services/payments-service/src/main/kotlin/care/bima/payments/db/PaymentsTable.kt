package care.bima.payments.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

private const val AMOUNT_PRECISION = 12
private const val AMOUNT_SCALE = 2

object PaymentsTable : Table("payments") {
    val id = uuid("payment_id")
    val claimId = uuid("claim_id").uniqueIndex()
    val patientId = uuid("patient_id").index()
    val amount = decimal("amount", AMOUNT_PRECISION, AMOUNT_SCALE)
    val releasedAt = datetime("released_at")

    override val primaryKey = PrimaryKey(id)
}
