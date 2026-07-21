import apiClient from './client'

export interface Identity {
  subject: string
  roles: string[]
}

export async function getMe(): Promise<Identity> {
  const response = await apiClient.get<Identity>('/me')
  return response.data
}
