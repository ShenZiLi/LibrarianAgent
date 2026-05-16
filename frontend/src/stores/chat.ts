import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { SessionResponse } from '@/types/chat'
import { listSessions } from '@/api/chat'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<SessionResponse[]>([])
  const currentSessionId = ref<string | null>(null)

  async function fetchSessions() {
    sessions.value = await listSessions()
  }

  function setCurrentSession(id: string) {
    currentSessionId.value = id
  }

  function addSession(session: SessionResponse) {
    sessions.value.unshift(session)
  }

  return {
    sessions,
    currentSessionId,
    fetchSessions,
    setCurrentSession,
    addSession,
  }
})
