import apiClient from './client'

export interface Patient {
  id: string
  nationalId: string
  firstName: string
  lastName: string
  phone: string
  gender: string
  dob: string
  isActive: boolean
}

export interface CreatePatientRequest {
  nationalId: string
  firstName: string
  lastName: string
  phone: string
  gender: string
  dob: string
}

export async function listPatients(): Promise<Patient[]> {
  const response = await apiClient.get<Patient[]>('/patients')
  return response.data
}

export async function getPatient(id: string): Promise<Patient> {
  const response = await apiClient.get<Patient>(`/patients/${id}`)
  return response.data
}

export async function createPatient(request: CreatePatientRequest): Promise<Patient> {
  const response = await apiClient.post<Patient>('/patients', request)
  return response.data
}
