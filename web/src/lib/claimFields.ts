import type { Claim } from '../api/claims'
import { formatDate } from './formatDate'

export function claimFields(claim: Claim, patientLabel: string, organizationLabel: string) {
  return [
    { label: 'Claim ID', value: claim.id },
    { label: 'Member', value: patientLabel },
    { label: 'Provider', value: organizationLabel },
    { label: 'Service type', value: claim.serviceType },
    { label: 'Diagnosis code', value: claim.diagnosisCode },
    { label: 'Treatment details', value: claim.treatmentDetails },
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
