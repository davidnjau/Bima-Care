package care.bima.patient.db

import care.bima.patient.domain.Gender
import care.bima.patient.domain.Patient
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PatientRepository {
    fun createSchema() =
        transaction {
            SchemaUtils.create(PatientsTable)
        }

    fun create(patient: Patient): Patient =
        transaction {
            PatientsTable.insert {
                it[id] = patient.id
                it[nationalId] = patient.nationalId
                it[firstName] = patient.firstName
                it[lastName] = patient.lastName
                it[phone] = patient.phone
                it[email] = patient.email
                it[gender] = patient.gender.name
                it[dob] = patient.dob
                it[isActive] = patient.isActive
            }
            patient
        }

    fun findById(id: UUID): Patient? =
        transaction {
            PatientsTable.selectAll().where { PatientsTable.id eq id }
                .map { it.toPatient() }
                .singleOrNull()
        }

    fun findByNationalId(nationalId: String): Patient? =
        transaction {
            PatientsTable.selectAll().where { PatientsTable.nationalId eq nationalId }
                .map { it.toPatient() }
                .singleOrNull()
        }

    fun findAll(): List<Patient> =
        transaction {
            PatientsTable.selectAll().map { it.toPatient() }
        }

    private fun ResultRow.toPatient() =
        Patient(
            id = this[PatientsTable.id],
            nationalId = this[PatientsTable.nationalId],
            firstName = this[PatientsTable.firstName],
            lastName = this[PatientsTable.lastName],
            phone = this[PatientsTable.phone],
            email = this[PatientsTable.email],
            gender = Gender.valueOf(this[PatientsTable.gender]),
            dob = this[PatientsTable.dob],
            isActive = this[PatientsTable.isActive],
        )
}
