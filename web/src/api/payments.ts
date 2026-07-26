import apiClient from './client'

export interface Payment {
  id: string
  claimId: string
  patientId: string
  amount: string
  releasedAt: string
}

export async function listPayments(patientId?: string): Promise<Payment[]> {
  const response = await apiClient.get<Payment[]>('/payments', { params: patientId ? { patientId } : {} })
  return response.data
}
