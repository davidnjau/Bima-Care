package care.bima.patient.db

import org.jetbrains.exposed.sql.Table

object DependentsTable : Table("dependents") {
    val id = uuid("dependent_id")
    val primaryPatientId = uuid("primary_patient_id").index()
    val dependentPatientId = uuid("dependent_patient_id").index()
    val relationship = varchar("relationship", 32)

    init {
        uniqueIndex(primaryPatientId, dependentPatientId)
    }

    override val primaryKey = PrimaryKey(id)
}
