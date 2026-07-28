import type { Claim } from '../api/claims'
import { formatDate } from './formatDate'

export function claimFields(claim: Claim, patientLabel: string, organizationLabel: string) {
  return [
    { label: 'Claim ID', value: claim.id },
    {
      label: 'Claim type',
      value: claim.claimType === 'MEMBER_REIMBURSEMENT' ? 'Member reimbursement' : 'Provider-submitted',
    },
    { label: 'Member', value: patientLabel },
    { label: 'Provider', value: organizationLabel },
    { label: 'Service type', value: claim.serviceType },
    { label: 'Diagnosis code', value: claim.diagnosisCode },
    { label: 'Treatment details', value: claim.treatmentDetails },
    { label: 'Date of service', value: claim.dateOfService && (formatDate(claim.dateOfService) ?? claim.dateOfService) },
    { label: 'Requested amount', value: `Ksh ${Number(claim.requestedAmount).toLocaleString()}` },
    {
      label: 'Approved amount',
      value: claim.approvedAmount ? `Ksh ${Number(claim.approvedAmount).toLocaleString()}` : null,
    },
    { label: 'Status', value: claim.status },
    { label: 'Submitted at', value: formatDate(claim.submittedAt) ?? claim.submittedAt },
    { label: 'Adjudicated at', value: formatDate(claim.adjudicatedAt) ?? claim.adjudicatedAt },
  ]
}

// Only reimbursement claims have attached evidence - provider-submitted claims have none.
export function claimDocuments(claim: Claim): { label: string; documentId: string }[] {
  return [
    claim.claimFormDocumentId && { label: 'claim form', documentId: claim.claimFormDocumentId },
    claim.itemizedReceiptDocumentId && { label: 'itemised receipt', documentId: claim.itemizedReceiptDocumentId },
    claim.etrDocumentId && { label: 'ETR', documentId: claim.etrDocumentId },
  ].filter((doc): doc is { label: string; documentId: string } => !!doc)
}
