export interface EvalResult {
  faithfulness: number
  contextPrecision: number
  accuracy: number
  metrics: Record<string, unknown>
  completedAt: string | null
}

export interface CostReport {
  totalInputTokens: number
  totalOutputTokens: number
  estimatedCostPer1000Calls: number
  sensitivityAnalysis: Record<string, number>
}
