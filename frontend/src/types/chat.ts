export interface SessionResponse {
  sessionId: string
  title: string
  messageCount: number
  createdAt: string
  lastActiveAt: string
}

export interface MessageResponse {
  role: string
  content: string
  timestamp: string
  citations: string[]
}

export interface SessionWithMessages {
  sessionId: string
  title: string
  messages: MessageResponse[]
  createdAt: string
}

export interface CreateSessionRequest {
  title?: string
}

export interface MessageRequest {
  content: string
}
