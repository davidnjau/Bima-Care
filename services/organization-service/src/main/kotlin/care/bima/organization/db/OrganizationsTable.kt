package care.bima.organization.db

import org.jetbrains.exposed.sql.Table

object OrganizationsTable : Table("organizations") {
    val id = uuid("organization_id")
    val registrationNumber = varchar("registration_number", 64).uniqueIndex()
    val name = varchar("name", 256).index()
    val type = varchar("type", 32)
    val phone = varchar("phone", 32)
    val address = varchar("address", 512)
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)
}
