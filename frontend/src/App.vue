<template>
  <div class="app-layout">
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header">
        <div class="logo" @click="isCollapsed = !isCollapsed">
          <span class="logo-icon">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
              <line x1="8" y1="7" x2="16" y2="7"/>
              <line x1="8" y1="11" x2="14" y2="11"/>
            </svg>
          </span>
          <span class="logo-text" v-show="!isCollapsed">Librarian</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/" class="nav-btn new-chat" @click="handleNewChat">
          <el-icon><Plus /></el-icon>
          <span v-show="!isCollapsed">新对话</span>
        </router-link>

        <div class="nav-divider" v-show="!isCollapsed"></div>

        <div class="nav-section-title" v-show="!isCollapsed">历史会话</div>
        <router-link
          v-for="session in chatStore.sessions"
          :key="session.sessionId"
          :to="{ path: '/', query: { id: session.sessionId } }"
          class="nav-item"
          :class="{ active: chatStore.currentSessionId === session.sessionId }"
          @click="chatStore.setCurrentSession(session.sessionId)"
        >
          <el-icon><ChatLineRound /></el-icon>
          <span class="nav-label" v-show="!isCollapsed">{{ session.title }}</span>
        </router-link>

        <div class="nav-divider" v-show="!isCollapsed"></div>

        <router-link to="/documents" class="nav-item" active-class="active">
          <el-icon><Files /></el-icon>
          <span v-show="!isCollapsed">文档管理</span>
        </router-link>

        <router-link to="/eval" class="nav-item" active-class="active">
          <el-icon><DataAnalysis /></el-icon>
          <span v-show="!isCollapsed">评估面板</span>
        </router-link>
      </nav>
    </aside>

    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useChatStore } from '@/stores/chat'
import { createSession } from '@/api/chat'
import { useRouter } from 'vue-router'

const chatStore = useChatStore()
const isCollapsed = ref(false)
const router = useRouter()

async function handleNewChat() {
  const session = await createSession({ title: '新对话' })
  chatStore.addSession(session)
  chatStore.setCurrentSession(session.sessionId)
  router.push({ path: '/', query: { id: session.sessionId } })
}

onMounted(() => {
  chatStore.fetchSessions()
})
</script>
