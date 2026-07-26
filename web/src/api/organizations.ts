import apiClient from './client'

export interface Organization {
  id: string
  registrationNumber: string
  name: string
  type: string
  phone: string
  address: string
  isActive: boolean
}

export interface CreateOrganizationRequest {
  registrationNumber: string
  name: string
  type: string
  phone: string
  address: string
}

export async function listOrganizations(): Promise<Organization[]> {
  const response = await apiClient.get<Organization[]>('/organizations')
  return response.data
}

export async function createOrganization(
  request: CreateOrganizationRequest,
): Promise<Organization> {
  const response = await apiClient.post<Organization>('/organizations', request)
  return response.data
}

export async function getOrganizationFhir(id: string): Promise<unknown> {
  const response = await apiClient.get(`/organizations/${id}/fhir`)
  return response.data
}
