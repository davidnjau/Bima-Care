package care.bima.provider.db

import org.jetbrains.exposed.sql.Table

object PractitionersTable : Table("practitioners") {
    val id = uuid("practitioner_id")
    val licenseNumber = varchar("license_number", 64).uniqueIndex()
    val firstName = varchar("first_name", 128)
    val lastName = varchar("last_name", 128).index()
    val phone = varchar("phone", 32)
    val specialty = varchar("specialty", 128)
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)
}
