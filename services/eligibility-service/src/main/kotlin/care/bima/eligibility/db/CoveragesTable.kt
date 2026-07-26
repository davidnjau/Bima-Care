package care.bima.eligibility.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object CoveragesTable : Table("coverages") {
    val id = uuid("coverage_id")
    val patientId = uuid("patient_id").index()
    val insurerId = uuid("insurer_id").index()
    val status = varchar("status", 32)
    val startDate = date("start_date")
    val endDate = date("end_date").nullable()
    val planTier = varchar("plan_tier", 64)
    val policyId = uuid("policy_id").nullable()

    override val primaryKey = PrimaryKey(id)
}
