import request from './request'
import type { EvalResult, CostReport } from '@/types/eval'

export const runEvaluation = (config?: { topK?: number; enableReranker?: boolean; temperature?: number }) => {
  return request.post('/eval/run', config)
}

export const getResults = () => {
  return request.get<any, EvalResult>('/eval/results')
}

export const getCostEstimate = (topK = 5, enableReranker = false, temperature = 0.7) => {
  return request.get<any, CostReport>('/eval/cost-estimate', {
    params: { topK, enableReranker, temperature },
  })
}
