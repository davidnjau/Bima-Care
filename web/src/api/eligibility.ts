import apiClient from './client'
import type { Coverage } from './coverages'

export interface EligibilityResult {
  eligible: boolean
  coverage: Coverage | null
}

export async function verifyEligibility(patientId: string): Promise<EligibilityResult> {
  const response = await apiClient.get<EligibilityResult>(`/coverages/verify/${patientId}`)
  return response.data
}
