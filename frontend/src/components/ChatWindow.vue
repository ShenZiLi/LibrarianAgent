<template>
  <div class="chat-window">
    <div class="messages-container" ref="messagesContainer">
      <div v-if="messages.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg viewBox="0 0 64 64" width="64" height="64" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="32" cy="32" r="28" opacity="0.3"/>
            <path d="M20 32h24M32 20v24"/>
            <circle cx="32" cy="32" r="8" opacity="0.5"/>
          </svg>
        </div>
        <h2 class="empty-title">开始新的对话</h2>
        <p class="empty-desc">向知识库提问，我会基于检索到的文档内容为您提供有据可查的答案</p>
      </div>

      <MessageItem
        v-for="(msg, index) in messages"
        :key="index"
        :message="msg"
      />

      <div v-if="isLoading" class="message-item assistant">
        <div class="message-avatar">
          <span class="avatar-icon ai-avatar">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
            </svg>
          </span>
        </div>
        <div class="message-body">
          <div class="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
    </div>

    <div class="input-area">
      <div class="input-wrapper">
        <textarea
          v-model="inputText"
          class="message-input"
          placeholder="输入您的问题..."
          @keydown.enter.exact.prevent="sendMessage"
          :disabled="isLoading"
          rows="1"
          ref="inputRef"
        ></textarea>
        <button
          class="send-button"
          @click="sendMessage"
          :disabled="isLoading || !inputText.trim()"
        >
          <el-icon><Promotion /></el-icon>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import MessageItem from './MessageItem.vue'
import { sendMessage as sendMsg } from '@/api/chat'
import { ElMessage } from 'element-plus'
import './ChatWindow.css'

interface Message {
  role: string
  content: string
  timestamp: string
  citations?: string[]
}

const props = defineProps<{
  sessionId: string | null
  messages: Message[]
}>()

const emit = defineEmits<{
  'message-sent': [message: Message]
}>()

const messagesContainer = ref<HTMLElement | null>(null)
const inputText = ref('')
const isLoading = ref(false)
const inputRef = ref<HTMLTextAreaElement | null>(null)

async function sendMessage() {
  if (!props.sessionId || !inputText.value.trim() || isLoading.value) return

  const content = inputText.value.trim()
  const userMessage: Message = {
    role: 'user',
    content,
    timestamp: new Date().toISOString(),
  }

  inputText.value = ''
  isLoading.value = true

  try {
    const response = await sendMsg(props.sessionId, { content })
    const assistantMessage: Message = {
      role: 'assistant',
      content: response.content,
      timestamp: response.timestamp,
      citations: response.citations || [],
    }
    emit('message-sent', assistantMessage)
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : '发送失败'
    ElMessage.error(msg)
  } finally {
    isLoading.value = false
  }

  await nextTick()
  scrollToBottom()
}

async function scrollToBottom() {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

watch(() => props.messages.length, () => {
  scrollToBottom()
})
</script>
