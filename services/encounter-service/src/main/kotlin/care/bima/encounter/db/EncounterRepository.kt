package care.bima.encounter.db

import care.bima.encounter.domain.Encounter
import care.bima.encounter.domain.EncounterStatus
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class EncounterRepository {
    fun createSchema() =
        transaction {
            SchemaUtils.create(EncountersTable)
        }

    fun create(encounter: Encounter): Encounter =
        transaction {
            EncountersTable.insert {
                it[id] = encounter.id
                it[patientId] = encounter.patientId
                it[practitionerId] = encounter.practitionerId
                it[organizationId] = encounter.organizationId
                it[status] = encounter.status.name
                it[startedAt] = encounter.startedAt
                it[endedAt] = encounter.endedAt
            }
            encounter
        }

    fun findById(id: UUID): Encounter? =
        transaction {
            EncountersTable.selectAll().where { EncountersTable.id eq id }
                .map { it.toEncounter() }
                .singleOrNull()
        }

    fun findByPatientId(patientId: UUID): List<Encounter> =
        transaction {
            EncountersTable.selectAll().where { EncountersTable.patientId eq patientId }
                .map { it.toEncounter() }
        }

    fun findAll(): List<Encounter> =
        transaction {
            EncountersTable.selectAll().map { it.toEncounter() }
        }

    private fun ResultRow.toEncounter() =
        Encounter(
            id = this[EncountersTable.id],
            patientId = this[EncountersTable.patientId],
            practitionerId = this[EncountersTable.practitionerId],
            organizationId = this[EncountersTable.organizationId],
            status = EncounterStatus.valueOf(this[EncountersTable.status]),
            startedAt = this[EncountersTable.startedAt],
            endedAt = this[EncountersTable.endedAt],
        )
}
