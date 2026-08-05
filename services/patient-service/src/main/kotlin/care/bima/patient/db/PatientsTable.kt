package care.bima.patient.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object PatientsTable : Table("patients") {
    val id = uuid("patient_id")
    val nationalId = varchar("national_id", 64).uniqueIndex()
    val firstName = varchar("first_name", 128)
    val lastName = varchar("last_name", 128).index()
    val phone = varchar("phone", 32)
    val email = varchar("email", 255).nullable()
    val gender = varchar("gender", 16)
    val dob = date("dob")
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)
}
