<template>
  <div class="home-view">
    <div class="chat-header" v-if="chatStore.currentSessionId">
      <h1 class="session-title">{{ currentTitle }}</h1>
    </div>

    <ChatWindow
      :session-id="chatStore.currentSessionId"
      :messages="messages"
      @message-sent="handleMessageSent"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import ChatWindow from '@/components/ChatWindow.vue'
import { getSession } from '@/api/chat'

const chatStore = useChatStore()
const route = useRoute()
const messages = ref<Array<{ role: string; content: string; timestamp: string; citations?: string[] }>>([])
const currentTitle = ref('')

async function loadSession(sessionId: string) {
  chatStore.setCurrentSession(sessionId)
  try {
    const session = await getSession(sessionId)
    messages.value = session.messages.map(msg => ({
      role: msg.role,
      content: msg.content,
      timestamp: msg.timestamp,
      citations: msg.citations,
    }))
    currentTitle.value = session.title
  } catch {
    messages.value = []
    currentTitle.value = ''
  }
}

function handleMessageSent(message: { role: string; content: string; timestamp: string; citations?: string[] }) {
  messages.value.push(message)
}

watch(() => route.query.id, (id) => {
  if (typeof id === 'string' && id) {
    loadSession(id)
  }
})

onMounted(() => {
  const id = route.query.id as string
  if (id) {
    loadSession(id)
  }
})
</script>
