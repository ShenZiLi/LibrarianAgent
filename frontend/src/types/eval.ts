export interface DocumentStats {
  totalDocuments: number
  completedDocuments: number
  processingDocuments: number
  failedDocuments: number
  totalChunks: number
}

export interface QueryLog {
  query: string
  retrievedDocs: number
  avgSimilarity: number
  retrievalTimeMs: number
  generationTimeMs: number
  timestamp: string
}

export interface RetrievalMetrics {
  avgSimilarity: number
  avgRetrievalTimeMs: number
  avgGenerationTimeMs: number
  totalQueries: number
}

export interface DashboardResponse {
  documentStats: DocumentStats
  recentQueries: QueryLog[]
  retrievalMetrics: RetrievalMetrics
}
