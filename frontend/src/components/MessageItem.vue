<template>
  <div class="message-item" :class="message.role">
    <div class="message-avatar">
      <span v-if="message.role === 'user'" class="avatar-icon user-avatar">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
      </span>
      <span v-else class="avatar-icon ai-avatar">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
        </svg>
      </span>
    </div>
    <div class="message-body">
      <div class="message-content" v-html="renderedContent"></div>
      <div class="message-citations" v-if="message.citations && message.citations.length > 0">
        <div class="citation-label">
          <el-icon><Link /></el-icon>
          引用来源
        </div>
        <div class="citation-list">
          <span
            v-for="(citation, idx) in message.citations"
            :key="idx"
            class="citation-tag"
          >
            {{ citation }}
          </span>
        </div>
      </div>
      <div class="message-time">
        {{ formatTime(message.timestamp) }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import './MessageItem.css'

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
})

interface Message {
  role: string
  content: string
  timestamp: string
  citations?: string[]
}

const props = defineProps<{
  message: Message
}>()

const renderedContent = computed(() => {
  return md.render(props.message.content)
})

function formatTime(timestamp: string): string {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>
