import apiClient from './client'

export interface Dependent {
  id: string
  primaryPatientId: string
  dependentPatientId: string
  relationship: string
}

export interface CreateDependentRequest {
  dependentPatientId: string
  relationship: string
}

export async function listDependents(primaryPatientId: string): Promise<Dependent[]> {
  const response = await apiClient.get<Dependent[]>(`/patients/${primaryPatientId}/dependents`)
  return response.data
}

export async function createDependent(
  primaryPatientId: string,
  request: CreateDependentRequest,
): Promise<Dependent> {
  const response = await apiClient.post<Dependent>(`/patients/${primaryPatientId}/dependents`, request)
  return response.data
}
