import apiClient from './client'

export interface Document {
  id: string
  patientId: string
  contentType: string
  title: string
  category: string
  status: string
  createdAt: string
}

export interface UploadDocumentRequest {
  patientId: string
  title: string
  category: string
  file: File
}

export async function uploadDocument(request: UploadDocumentRequest): Promise<Document> {
  const form = new FormData()
  form.append('patientId', request.patientId)
  form.append('title', request.title)
  form.append('category', request.category)
  form.append('file', request.file)
  const response = await apiClient.post<Document>('/documents', form)
  return response.data
}

// Fetches the file's bytes (with the auth header a plain <a href> to the gateway can't carry)
// and triggers a browser download/open via a temporary object URL.
export async function downloadDocument(id: string, filename: string): Promise<void> {
  const response = await apiClient.get(`/documents/${id}/content`, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data as Blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.target = '_blank'
  link.click()
  URL.revokeObjectURL(url)
}
