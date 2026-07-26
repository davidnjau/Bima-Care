package care.bima.eligibility.db

import care.bima.eligibility.domain.Coverage
import care.bima.eligibility.domain.CoverageStatus
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.UUID

class CoverageRepository {
    fun createSchema() =
        transaction {
            SchemaUtils.createMissingTablesAndColumns(CoveragesTable)
        }

    fun create(coverage: Coverage): Coverage =
        transaction {
            CoveragesTable.insert {
                it[id] = coverage.id
                it[patientId] = coverage.patientId
                it[insurerId] = coverage.insurerId
                it[status] = coverage.status.name
                it[startDate] = coverage.startDate
                it[endDate] = coverage.endDate
                it[planTier] = coverage.planTier
                it[policyId] = coverage.policyId
            }
            coverage
        }

    fun findById(id: UUID): Coverage? =
        transaction {
            CoveragesTable.selectAll().where { CoveragesTable.id eq id }
                .map { it.toCoverage() }
                .singleOrNull()
        }

    fun findActiveCoverage(
        patientId: UUID,
        onDate: LocalDate,
    ): Coverage? =
        transaction {
            CoveragesTable.selectAll().where {
                (CoveragesTable.patientId eq patientId) and
                    (CoveragesTable.status eq CoverageStatus.ACTIVE.name) and
                    (CoveragesTable.startDate lessEq onDate) and
                    ((CoveragesTable.endDate greaterEq onDate) or CoveragesTable.endDate.isNull())
            }.map { it.toCoverage() }.firstOrNull()
        }

    fun findAll(): List<Coverage> =
        transaction {
            CoveragesTable.selectAll().map { it.toCoverage() }
        }

    private fun ResultRow.toCoverage() =
        Coverage(
            id = this[CoveragesTable.id],
            patientId = this[CoveragesTable.patientId],
            insurerId = this[CoveragesTable.insurerId],
            status = CoverageStatus.valueOf(this[CoveragesTable.status]),
            startDate = this[CoveragesTable.startDate],
            endDate = this[CoveragesTable.endDate],
            planTier = this[CoveragesTable.planTier],
            policyId = this[CoveragesTable.policyId],
        )
}
