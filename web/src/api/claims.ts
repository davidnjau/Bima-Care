import apiClient from './client'

export interface Claim {
  id: string
  patientId: string
  encounterId: string
  coverageId: string
  practitionerId: string | null
  organizationId: string
  serviceType: string
  diagnosisCode: string
  treatmentDetails: string
  requestedAmount: string
  approvedAmount: string | null
  status: string
  // PROVIDER_SUBMITTED or MEMBER_REIMBURSEMENT
  claimType: string
  dateOfService: string | null
  claimFormDocumentId: string | null
  itemizedReceiptDocumentId: string | null
  etrDocumentId: string | null
  submittedAt: string
  adjudicatedAt: string | null
}

export interface SubmitClaimRequest {
  patientId: string
  serviceType: string
  diagnosisCode: string
  treatmentDetails: string
  amount: string
}

// No patientId - the gateway resolves the submitting member's own patientId from their JWT.
export interface SubmitReimbursementClaimRequest {
  organizationId: string
  serviceType: string
  diagnosisCode: string
  treatmentDetails: string
  amount: string
  dateOfService: string
  claimFormDocumentId: string
  itemizedReceiptDocumentId: string
  etrDocumentId: string
}

export interface AdjudicateClaimRequest {
  decision: string
  approvedAmount?: string
}

export interface ListClaimsParams {
  status?: string
  patientId?: string
  organizationId?: string
}

export async function submitClaim(request: SubmitClaimRequest): Promise<Claim> {
  const response = await apiClient.post<Claim>('/claims', request)
  return response.data
}

export async function submitReimbursementClaim(request: SubmitReimbursementClaimRequest): Promise<Claim> {
  const response = await apiClient.post<Claim>('/claims/reimbursement', request)
  return response.data
}

export async function listClaims(params: ListClaimsParams = {}): Promise<Claim[]> {
  const response = await apiClient.get<Claim[]>('/claims', { params })
  return response.data
}

export async function adjudicateClaim(id: string, request: AdjudicateClaimRequest): Promise<Claim> {
  const response = await apiClient.post<Claim>(`/claims/${id}/adjudicate`, request)
  return response.data
}

export async function getClaimFhir(id: string): Promise<unknown> {
  const response = await apiClient.get(`/claims/${id}/fhir`)
  return response.data
}
