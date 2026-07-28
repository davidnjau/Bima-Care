import apiClient from './client'

export interface Policy {
  id: string
  insurerId: string
  policyNumber: string
  name: string
  type: string
  premium: string
  startDate: string
  endDate: string | null
  status: string
}

// No policyNumber - the server generates it.
export interface CreatePolicyRequest {
  name: string
  type: string
  premium: string
  startDate: string
  endDate?: string
}

export async function listPolicies(): Promise<Policy[]> {
  const response = await apiClient.get<Policy[]>('/policies')
  return response.data
}

export async function createPolicy(request: CreatePolicyRequest): Promise<Policy> {
  const response = await apiClient.post<Policy>('/policies', request)
  return response.data
}

export async function updatePolicyStatus(id: string, status: string): Promise<Policy> {
  const response = await apiClient.post<Policy>(`/policies/${id}/status`, { status })
  return response.data
}

export async function getPolicyFhir(id: string): Promise<unknown> {
  const response = await apiClient.get(`/policies/${id}/fhir`)
  return response.data
}
