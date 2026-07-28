package care.bima.claims.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

private const val AMOUNT_PRECISION = 12
private const val AMOUNT_SCALE = 2

object ClaimsTable : Table("claims") {
    val id = uuid("claim_id")
    val patientId = uuid("patient_id").index()
    val encounterId = uuid("encounter_id")
    val coverageId = uuid("coverage_id")
    val practitionerId = uuid("practitioner_id").nullable()
    val organizationId = uuid("organization_id").index()
    val serviceType = varchar("service_type", 64)
    val diagnosisCode = varchar("diagnosis_code", 32)
    val treatmentDetails = varchar("treatment_details", 1024)
    val requestedAmount = decimal("requested_amount", AMOUNT_PRECISION, AMOUNT_SCALE)
    val approvedAmount = decimal("approved_amount", AMOUNT_PRECISION, AMOUNT_SCALE).nullable()
    val status = varchar("status", 32).index()
    val claimType = varchar("claim_type", 32).default("PROVIDER_SUBMITTED")
    val dateOfService = date("date_of_service").nullable()
    val claimFormDocumentId = uuid("claim_form_document_id").nullable()
    val itemizedReceiptDocumentId = uuid("itemized_receipt_document_id").nullable()
    val etrDocumentId = uuid("etr_document_id").nullable()
    val submittedAt = datetime("submitted_at")
    val adjudicatedAt = datetime("adjudicated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
