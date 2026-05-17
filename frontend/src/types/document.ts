export interface DocumentResponse {
  documentId: string
  fileName: string
  fileType: string
  fileSize: number
  status: string
  chunkCount: number
  errorMessage: string | null
  createdAt: string
  updatedAt: string | null
}

export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
