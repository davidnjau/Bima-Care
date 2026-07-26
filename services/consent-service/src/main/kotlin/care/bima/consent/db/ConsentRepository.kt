package care.bima.consent.db

import care.bima.consent.domain.Consent
import care.bima.consent.domain.ConsentStatus
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class ConsentRepository {
    fun createSchema() =
        transaction {
            SchemaUtils.create(ConsentsTable)
        }

    fun create(consent: Consent): Consent =
        transaction {
            ConsentsTable.insert {
                it[id] = consent.id
                it[patientId] = consent.patientId
                it[granteeId] = consent.granteeId
                it[scope] = consent.scope
                it[status] = consent.status.name
                it[createdAt] = consent.createdAt
            }
            consent
        }

    fun findById(id: UUID): Consent? =
        transaction {
            ConsentsTable.selectAll().where { ConsentsTable.id eq id }
                .map { it.toConsent() }
                .singleOrNull()
        }

    fun findByPatientId(patientId: UUID): List<Consent> =
        transaction {
            ConsentsTable.selectAll().where { ConsentsTable.patientId eq patientId }
                .map { it.toConsent() }
        }

    fun findActiveConsent(
        patientId: UUID,
        granteeId: UUID,
    ): Consent? =
        transaction {
            ConsentsTable.selectAll().where {
                (ConsentsTable.patientId eq patientId) and
                    (ConsentsTable.granteeId eq granteeId) and
                    (ConsentsTable.status eq ConsentStatus.ACTIVE.name)
            }.map { it.toConsent() }.firstOrNull()
        }

    fun revoke(id: UUID): Consent? =
        transaction {
            ConsentsTable.update({ ConsentsTable.id eq id }) {
                it[status] = ConsentStatus.REVOKED.name
            }
            ConsentsTable.selectAll().where { ConsentsTable.id eq id }.map { it.toConsent() }.singleOrNull()
        }

    private fun ResultRow.toConsent() =
        Consent(
            id = this[ConsentsTable.id],
            patientId = this[ConsentsTable.patientId],
            granteeId = this[ConsentsTable.granteeId],
            scope = this[ConsentsTable.scope],
            status = ConsentStatus.valueOf(this[ConsentsTable.status]),
            createdAt = this[ConsentsTable.createdAt],
        )
}
