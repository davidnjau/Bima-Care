package care.bima.claims.db

import care.bima.claims.domain.Claim
import care.bima.claims.domain.ClaimStatus
import care.bima.claims.domain.ClaimType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class ClaimRepository {
    fun createSchema() =
        transaction {
            SchemaUtils.create(ClaimsTable)
        }

    fun create(claim: Claim): Claim =
        transaction {
            ClaimsTable.insert {
                it[id] = claim.id
                it[patientId] = claim.patientId
                it[encounterId] = claim.encounterId
                it[coverageId] = claim.coverageId
                it[practitionerId] = claim.practitionerId
                it[organizationId] = claim.organizationId
                it[serviceType] = claim.serviceType
                it[diagnosisCode] = claim.diagnosisCode
                it[treatmentDetails] = claim.treatmentDetails
                it[requestedAmount] = claim.requestedAmount
                it[approvedAmount] = claim.approvedAmount
                it[status] = claim.status.name
                it[claimType] = claim.claimType.name
                it[dateOfService] = claim.dateOfService
                it[claimFormDocumentId] = claim.claimFormDocumentId
                it[itemizedReceiptDocumentId] = claim.itemizedReceiptDocumentId
                it[etrDocumentId] = claim.etrDocumentId
                it[submittedAt] = claim.submittedAt
                it[adjudicatedAt] = claim.adjudicatedAt
            }
            claim
        }

    fun findById(id: UUID): Claim? =
        transaction {
            ClaimsTable.selectAll().where { ClaimsTable.id eq id }
                .map { it.toClaim() }
                .singleOrNull()
        }

    fun findAll(
        status: ClaimStatus?,
        patientId: UUID?,
        organizationId: UUID?,
    ): List<Claim> =
        transaction {
            val query = ClaimsTable.selectAll()
            status?.let { query.andWhere { ClaimsTable.status eq it.name } }
            patientId?.let { query.andWhere { ClaimsTable.patientId eq it } }
            organizationId?.let { query.andWhere { ClaimsTable.organizationId eq it } }
            query.map { it.toClaim() }
        }

    fun adjudicate(
        id: UUID,
        status: ClaimStatus,
        approvedAmount: BigDecimal?,
        adjudicatedAt: LocalDateTime,
    ): Claim? =
        transaction {
            ClaimsTable.update({ ClaimsTable.id eq id }) {
                it[ClaimsTable.status] = status.name
                it[ClaimsTable.approvedAmount] = approvedAmount
                it[ClaimsTable.adjudicatedAt] = adjudicatedAt
            }
            ClaimsTable.selectAll().where { ClaimsTable.id eq id }.map { it.toClaim() }.singleOrNull()
        }

    private fun ResultRow.toClaim() =
        Claim(
            id = this[ClaimsTable.id],
            patientId = this[ClaimsTable.patientId],
            encounterId = this[ClaimsTable.encounterId],
            coverageId = this[ClaimsTable.coverageId],
            practitionerId = this[ClaimsTable.practitionerId],
            organizationId = this[ClaimsTable.organizationId],
            serviceType = this[ClaimsTable.serviceType],
            diagnosisCode = this[ClaimsTable.diagnosisCode],
            treatmentDetails = this[ClaimsTable.treatmentDetails],
            requestedAmount = this[ClaimsTable.requestedAmount],
            approvedAmount = this[ClaimsTable.approvedAmount],
            status = ClaimStatus.valueOf(this[ClaimsTable.status]),
            claimType = ClaimType.valueOf(this[ClaimsTable.claimType]),
            dateOfService = this[ClaimsTable.dateOfService],
            claimFormDocumentId = this[ClaimsTable.claimFormDocumentId],
            itemizedReceiptDocumentId = this[ClaimsTable.itemizedReceiptDocumentId],
            etrDocumentId = this[ClaimsTable.etrDocumentId],
            submittedAt = this[ClaimsTable.submittedAt],
            adjudicatedAt = this[ClaimsTable.adjudicatedAt],
        )
}
