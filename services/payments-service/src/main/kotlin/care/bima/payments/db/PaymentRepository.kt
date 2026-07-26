package care.bima.payments.db

import care.bima.payments.domain.Payment
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PaymentRepository {
    fun createSchema() =
        transaction {
            SchemaUtils.create(PaymentsTable)
        }

    fun create(payment: Payment): Payment =
        transaction {
            PaymentsTable.insert {
                it[id] = payment.id
                it[claimId] = payment.claimId
                it[patientId] = payment.patientId
                it[amount] = payment.amount
                it[releasedAt] = payment.releasedAt
            }
            payment
        }

    fun findById(id: UUID): Payment? =
        transaction {
            PaymentsTable.selectAll().where { PaymentsTable.id eq id }
                .map { it.toPayment() }
                .singleOrNull()
        }

    fun findByClaimId(claimId: UUID): Payment? =
        transaction {
            PaymentsTable.selectAll().where { PaymentsTable.claimId eq claimId }
                .map { it.toPayment() }
                .singleOrNull()
        }

    fun findAll(patientId: UUID?): List<Payment> =
        transaction {
            val query = PaymentsTable.selectAll()
            patientId?.let { query.andWhere { PaymentsTable.patientId eq it } }
            query.map { it.toPayment() }
        }

    private fun ResultRow.toPayment() =
        Payment(
            id = this[PaymentsTable.id],
            claimId = this[PaymentsTable.claimId],
            patientId = this[PaymentsTable.patientId],
            amount = this[PaymentsTable.amount],
            releasedAt = this[PaymentsTable.releasedAt],
        )
}
