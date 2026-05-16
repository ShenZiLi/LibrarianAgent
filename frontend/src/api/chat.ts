import request from './request'
import type { SessionResponse, SessionWithMessages, MessageResponse, MessageRequest } from '@/types/chat'

export const createSession = (data?: { title?: string }) => {
  return request.post<any, SessionResponse>('/chat/sessions', data)
}

export const listSessions = () => {
  return request.get<any, SessionResponse[]>('/chat/sessions')
}

export const getSession = (sessionId: string) => {
  return request.get<any, SessionWithMessages>(`/chat/sessions/${sessionId}`)
}

export const sendMessage = (sessionId: string, data: MessageRequest) => {
  return request.post<any, MessageResponse>(`/chat/sessions/${sessionId}/message`, data)
}

export const streamMessage = (sessionId: string, data: MessageRequest): EventSource => {
  const params = new URLSearchParams()
  params.set('content', data.content)
  return new EventSource(`/api/v1/chat/sessions/${sessionId}/stream?${params.toString()}`)
}
