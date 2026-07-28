package care.bima.claims.api

import care.bima.shared.service.ValidationException
import java.time.LocalDate
import java.util.UUID

private const val MAX_DAYS_SINCE_SERVICE = 30L

internal fun parseDateOfService(raw: String): LocalDate {
    val dateOfService =
        runCatching { LocalDate.parse(raw) }.getOrElse { throw ValidationException("Invalid dateOfService: $raw") }
    val today = LocalDate.now()
    if (dateOfService.isAfter(today)) {
        throw ValidationException("dateOfService cannot be in the future")
    }
    if (dateOfService.isBefore(today.minusDays(MAX_DAYS_SINCE_SERVICE))) {
        throw ValidationException("Claims must be submitted within $MAX_DAYS_SINCE_SERVICE days of service")
    }
    return dateOfService
}

internal suspend fun verifyReimbursementDocuments(
    deps: ClaimRouteDependencies,
    patientId: UUID,
    request: SubmitReimbursementClaimRequest,
): Triple<UUID, UUID, UUID> {
    val claimFormDocumentId = parseId(request.claimFormDocumentId, "claimFormDocumentId")
    val itemizedReceiptDocumentId = parseId(request.itemizedReceiptDocumentId, "itemizedReceiptDocumentId")
    val etrDocumentId = parseId(request.etrDocumentId, "etrDocumentId")
    mapOf(
        "claimFormDocumentId" to claimFormDocumentId,
        "itemizedReceiptDocumentId" to itemizedReceiptDocumentId,
        "etrDocumentId" to etrDocumentId,
    ).forEach { (field, documentId) ->
        if (!deps.documentClient.belongsToPatient(documentId, patientId)) {
            throw ValidationException("$field does not reference a document uploaded by this member")
        }
    }
    return Triple(claimFormDocumentId, itemizedReceiptDocumentId, etrDocumentId)
}
