package care.bima.patient.db

import care.bima.patient.domain.Dependent
import care.bima.patient.domain.RelationshipType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class DependentRepository {
    fun createSchema() =
        transaction {
            SchemaUtils.create(DependentsTable)
        }

    fun create(dependent: Dependent): Dependent =
        transaction {
            DependentsTable.insert {
                it[id] = dependent.id
                it[primaryPatientId] = dependent.primaryPatientId
                it[dependentPatientId] = dependent.dependentPatientId
                it[relationship] = dependent.relationship.name
            }
            dependent
        }

    fun findById(id: UUID): Dependent? =
        transaction {
            DependentsTable.selectAll().where { DependentsTable.id eq id }
                .map { it.toDependent() }
                .singleOrNull()
        }

    fun findByPrimaryPatientId(primaryPatientId: UUID): List<Dependent> =
        transaction {
            DependentsTable.selectAll().where { DependentsTable.primaryPatientId eq primaryPatientId }
                .map { it.toDependent() }
        }

    fun findLink(
        primaryPatientId: UUID,
        dependentPatientId: UUID,
    ): Dependent? =
        transaction {
            DependentsTable.selectAll().where {
                (DependentsTable.primaryPatientId eq primaryPatientId) and
                    (DependentsTable.dependentPatientId eq dependentPatientId)
            }.map { it.toDependent() }.singleOrNull()
        }

    private fun ResultRow.toDependent() =
        Dependent(
            id = this[DependentsTable.id],
            primaryPatientId = this[DependentsTable.primaryPatientId],
            dependentPatientId = this[DependentsTable.dependentPatientId],
            relationship = RelationshipType.valueOf(this[DependentsTable.relationship]),
        )
}
