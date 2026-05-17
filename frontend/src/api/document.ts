import request from './request'
import type { DocumentResponse, PageResponse } from '@/types/document'

export const uploadDocument = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const listDocuments = (page: number = 1, size: number = 10, status?: string) => {
  const params: Record<string, any> = { page, size }
  if (status) {
    params.status = status
  }
  return request.get<any, PageResponse<DocumentResponse>>('/documents', { params })
}

export const getDocument = (documentId: string) => {
  return request.get<any, DocumentResponse>(`/documents/${documentId}`)
}

export const deleteDocument = (documentId: string) => {
  return request.delete(`/documents/${documentId}`)
}

export const retryDocument = (documentId: string) => {
  return request.post(`/documents/${documentId}/retry`)
}
