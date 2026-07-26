import apiClient from './client'

export interface Coverage {
  id: string
  patientId: string
  insurerId: string
  status: string
  startDate: string
  endDate: string | null
  planTier: string
  policyId: string | null
}

export interface CreateCoverageRequest {
  patientId: string
  insurerId: string
  status: string
  startDate: string
  endDate?: string
  planTier: string
  policyId?: string
}

export async function listCoverages(): Promise<Coverage[]> {
  const response = await apiClient.get<Coverage[]>('/coverages')
  return response.data
}

export async function createCoverage(request: CreateCoverageRequest): Promise<Coverage> {
  const response = await apiClient.post<Coverage>('/coverages', request)
  return response.data
}
