import apiClient from './client'

export interface Coverage {
  id: string
  patientId: string
  insurerId: string
  status: string
  startDate: string
  endDate: string | null
  planTier: string
}

export async function listCoverages(): Promise<Coverage[]> {
  const response = await apiClient.get<Coverage[]>('/coverages')
  return response.data
}
