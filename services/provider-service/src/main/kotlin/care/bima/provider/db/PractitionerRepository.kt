package care.bima.provider.db

import care.bima.provider.domain.Practitioner
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PractitionerRepository {
    fun createSchema() =
        transaction {
            SchemaUtils.create(PractitionersTable)
        }

    fun create(practitioner: Practitioner): Practitioner =
        transaction {
            PractitionersTable.insert {
                it[id] = practitioner.id
                it[licenseNumber] = practitioner.licenseNumber
                it[firstName] = practitioner.firstName
                it[lastName] = practitioner.lastName
                it[phone] = practitioner.phone
                it[specialty] = practitioner.specialty
                it[isActive] = practitioner.isActive
            }
            practitioner
        }

    fun findById(id: UUID): Practitioner? =
        transaction {
            PractitionersTable.selectAll().where { PractitionersTable.id eq id }
                .map { it.toPractitioner() }
                .singleOrNull()
        }

    fun findAll(): List<Practitioner> =
        transaction {
            PractitionersTable.selectAll().map { it.toPractitioner() }
        }

    private fun ResultRow.toPractitioner() =
        Practitioner(
            id = this[PractitionersTable.id],
            licenseNumber = this[PractitionersTable.licenseNumber],
            firstName = this[PractitionersTable.firstName],
            lastName = this[PractitionersTable.lastName],
            phone = this[PractitionersTable.phone],
            specialty = this[PractitionersTable.specialty],
            isActive = this[PractitionersTable.isActive],
        )
}
