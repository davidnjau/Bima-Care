import apiClient from './client'

export interface Patient {
  id: string
  nationalId: string
  firstName: string
  lastName: string
  phone: string
  email: string | null
  gender: string
  dob: string
  isActive: boolean
}

export interface CreatePatientRequest {
  nationalId: string
  firstName: string
  lastName: string
  phone: string
  // Optional - lets provisioning email the temp password instead of only logging it.
  email?: string
  gender: string
  dob: string
  // Client-generated id (see Workstream C offline queue) so a retried sync of the same
  // request is a safe no-op on patient-service rather than a duplicate or a conflict.
  id?: string
}

export async function listPatients(): Promise<Patient[]> {
  const response = await apiClient.get<Patient[]>('/patients')
  return response.data
}

export async function getPatient(id: string): Promise<Patient> {
  const response = await apiClient.get<Patient>(`/patients/${id}`)
  return response.data
}

export async function getPatientFhir(id: string): Promise<unknown> {
  const response = await apiClient.get(`/patients/${id}/fhir`)
  return response.data
}

export async function createPatient(request: CreatePatientRequest): Promise<Patient> {
  const response = await apiClient.post<Patient>('/patients', request)
  return response.data
}
