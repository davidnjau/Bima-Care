package care.bima.organization.db

import care.bima.organization.domain.Organization
import care.bima.organization.domain.OrganizationType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class OrganizationRepository {
    fun createSchema() =
        transaction {
            SchemaUtils.create(OrganizationsTable)
        }

    fun create(organization: Organization): Organization =
        transaction {
            OrganizationsTable.insert {
                it[id] = organization.id
                it[registrationNumber] = organization.registrationNumber
                it[name] = organization.name
                it[type] = organization.type.name
                it[phone] = organization.phone
                it[address] = organization.address
                it[isActive] = organization.isActive
            }
            organization
        }

    fun findById(id: UUID): Organization? =
        transaction {
            OrganizationsTable.selectAll().where { OrganizationsTable.id eq id }
                .map { it.toOrganization() }
                .singleOrNull()
        }

    fun findAll(): List<Organization> =
        transaction {
            OrganizationsTable.selectAll().map { it.toOrganization() }
        }

    private fun ResultRow.toOrganization() =
        Organization(
            id = this[OrganizationsTable.id],
            registrationNumber = this[OrganizationsTable.registrationNumber],
            name = this[OrganizationsTable.name],
            type = OrganizationType.valueOf(this[OrganizationsTable.type]),
            phone = this[OrganizationsTable.phone],
            address = this[OrganizationsTable.address],
            isActive = this[OrganizationsTable.isActive],
        )
}
