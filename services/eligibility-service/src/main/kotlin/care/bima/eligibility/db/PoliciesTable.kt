package care.bima.eligibility.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

private const val PREMIUM_PRECISION = 12
private const val PREMIUM_SCALE = 2

object PoliciesTable : Table("policies") {
    val id = uuid("policy_id")
    val insurerId = uuid("insurer_id").index()
    val policyNumber = varchar("policy_number", 64).uniqueIndex()
    val name = varchar("name", 255)
    val type = varchar("type", 32)
    val premium = decimal("premium", PREMIUM_PRECISION, PREMIUM_SCALE)
    val startDate = date("start_date")
    val endDate = date("end_date").nullable()
    val status = varchar("status", 32)

    override val primaryKey = PrimaryKey(id)
}
