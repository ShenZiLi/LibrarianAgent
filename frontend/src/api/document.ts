import request from './request'
import type { DocumentResponse } from '@/types/document'

export const uploadDocument = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const listDocuments = () => {
  return request.get<any, DocumentResponse[]>('/documents')
}

export const getDocument = (documentId: string) => {
  return request.get<any, DocumentResponse>(`/documents/${documentId}`)
}

export const deleteDocument = (documentId: string) => {
  return request.delete(`/documents/${documentId}`)
}
