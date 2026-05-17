<template>
  <div class="eval-view">
    <div class="view-header">
      <h1 class="view-title">RAG 监控面板</h1>
      <el-button @click="fetchDashboard" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="eval-content">
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon docs">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard.documentStats?.totalDocuments ?? 0 }}</div>
            <div class="stat-label">文档总数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon completed">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard.documentStats?.completedDocuments ?? 0 }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon processing">
            <el-icon><Loading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard.documentStats?.processingDocuments ?? 0 }}</div>
            <div class="stat-label">处理中</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon failed">
            <el-icon><CircleClose /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard.documentStats?.failedDocuments ?? 0 }}</div>
            <div class="stat-label">失败</div>
          </div>
        </div>
      </div>

      <div class="metrics-grid">
        <div class="metric-card">
          <div class="metric-label">平均相似度</div>
          <div class="metric-value" :style="{ color: getScoreColor(dashboard.retrievalMetrics?.avgSimilarity ?? 0) }">
            {{ ((dashboard.retrievalMetrics?.avgSimilarity ?? 0) * 100).toFixed(1) }}%
          </div>
          <div class="metric-bar">
            <div class="metric-bar-fill"
              :style="{ width: `${(dashboard.retrievalMetrics?.avgSimilarity ?? 0) * 100}%`, backgroundColor: getScoreColor(dashboard.retrievalMetrics?.avgSimilarity ?? 0) }">
            </div>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-label">平均检索耗时</div>
          <div class="metric-value">{{ dashboard.retrievalMetrics?.avgRetrievalTimeMs ?? 0 }}ms</div>
          <div class="metric-sub">检索阶段</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">平均生成耗时</div>
          <div class="metric-value">{{ dashboard.retrievalMetrics?.avgGenerationTimeMs ?? 0 }}ms</div>
          <div class="metric-sub">LLM 生成阶段</div>
        </div>
      </div>

      <div class="log-section">
        <div class="section-header">
          <h2 class="section-title">最近查询日志</h2>
          <span class="query-count">共 {{ dashboard.retrievalMetrics?.totalQueries ?? 0 }} 次查询</span>
        </div>
        <div v-if="dashboard.recentQueries?.length" class="log-entries">
          <div v-for="(log, index) in dashboard.recentQueries" :key="index" class="log-entry">
            <span class="log-similarity" :class="getSimilarityClass(log.avgSimilarity)">
              {{ (log.avgSimilarity * 100).toFixed(0) }}%
            </span>
            <span class="log-query">{{ log.query }}</span>
            <span class="log-meta">{{ log.retrievedDocs }} 条结果 · 检索 {{ log.retrievalTimeMs }}ms · 生成 {{ log.generationTimeMs }}ms</span>
            <span class="log-time">{{ formatTime(log.timestamp) }}</span>
          </div>
        </div>
        <div v-else class="empty-logs">
          暂无查询记录，开始对话后将在此显示
        </div>
      </div>

      <div class="chunks-section">
        <div class="section-header">
          <h2 class="section-title">向量分块统计</h2>
        </div>
        <div class="chunks-info">
          <span class="chunks-value">{{ dashboard.documentStats?.totalChunks ?? 0 }}</span>
          <span class="chunks-label">个向量分块已入库</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboard } from '@/api/eval'
import type { DashboardResponse } from '@/types/eval'
import { ElMessage } from 'element-plus'
import './EvalView.css'

const loading = ref(false)
const dashboard = ref<Partial<DashboardResponse>>({
  documentStats: {
    totalDocuments: 0,
    completedDocuments: 0,
    processingDocuments: 0,
    failedDocuments: 0,
    totalChunks: 0,
  },
  recentQueries: [],
  retrievalMetrics: {
    avgSimilarity: 0,
    avgRetrievalTimeMs: 0,
    avgGenerationTimeMs: 0,
    totalQueries: 0,
  },
})

async function fetchDashboard() {
  loading.value = true
  try {
    dashboard.value = await getDashboard()
  } catch {
    ElMessage.error('获取监控数据失败')
  } finally {
    loading.value = false
  }
}

function getScoreColor(score: number): string {
  if (score >= 0.85) return '#6aab7c'
  if (score >= 0.7) return '#d4a76a'
  if (score >= 0.3) return '#c4956a'
  return '#d47a6a'
}

function getSimilarityClass(score: number): string {
  if (score >= 0.7) return 'high'
  if (score >= 0.3) return 'medium'
  return 'low'
}

function formatTime(timestamp: string): string {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

onMounted(fetchDashboard)
</script>
