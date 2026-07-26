package care.bima.eligibility.db

import care.bima.eligibility.domain.Policy
import care.bima.eligibility.domain.PolicyStatus
import care.bima.eligibility.domain.PolicyType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class PolicyRepository {
    fun createSchema() =
        transaction {
            SchemaUtils.create(PoliciesTable)
        }

    fun create(policy: Policy): Policy =
        transaction {
            PoliciesTable.insert {
                it[id] = policy.id
                it[insurerId] = policy.insurerId
                it[policyNumber] = policy.policyNumber
                it[name] = policy.name
                it[type] = policy.type.name
                it[premium] = policy.premium
                it[startDate] = policy.startDate
                it[endDate] = policy.endDate
                it[status] = policy.status.name
            }
            policy
        }

    fun findById(id: UUID): Policy? =
        transaction {
            PoliciesTable.selectAll().where { PoliciesTable.id eq id }
                .map { it.toPolicy() }
                .singleOrNull()
        }

    fun findAll(insurerId: UUID?): List<Policy> =
        transaction {
            val query = PoliciesTable.selectAll()
            insurerId?.let { query.andWhere { PoliciesTable.insurerId eq it } }
            query.map { it.toPolicy() }
        }

    fun updateStatus(
        id: UUID,
        status: PolicyStatus,
    ): Policy? =
        transaction {
            PoliciesTable.update({ PoliciesTable.id eq id }) {
                it[PoliciesTable.status] = status.name
            }
            PoliciesTable.selectAll().where { PoliciesTable.id eq id }.map { it.toPolicy() }.singleOrNull()
        }

    private fun ResultRow.toPolicy() =
        Policy(
            id = this[PoliciesTable.id],
            insurerId = this[PoliciesTable.insurerId],
            policyNumber = this[PoliciesTable.policyNumber],
            name = this[PoliciesTable.name],
            type = PolicyType.valueOf(this[PoliciesTable.type]),
            premium = this[PoliciesTable.premium],
            startDate = this[PoliciesTable.startDate],
            endDate = this[PoliciesTable.endDate],
            status = PolicyStatus.valueOf(this[PoliciesTable.status]),
        )
}
