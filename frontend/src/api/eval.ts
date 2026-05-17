import request from './request'
import type { DashboardResponse } from '@/types/eval'

export const getDashboard = () => {
  return request.get<any, DashboardResponse>('/eval/dashboard')
}
