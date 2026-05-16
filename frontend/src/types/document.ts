export interface DocumentResponse {
  documentId: string
  fileName: string
  fileType: string
  fileSize: number
  status: string
  chunkCount: number
  createdAt: string
  processedAt: string | null
}
