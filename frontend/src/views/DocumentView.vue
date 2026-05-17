<template>
  <div class="document-view">
    <div class="view-header">
      <h1 class="view-title">文档管理</h1>
      <div class="header-actions">
        <el-select
          v-model="filterStatus"
          placeholder="全部状态"
          clearable
          size="default"
          class="status-filter"
          @change="handleFilterChange"
        >
          <el-option label="处理中" value="processing" />
          <el-option label="已完成" value="completed" />
          <el-option label="失败" value="failed" />
        </el-select>
        <el-button type="primary" @click="showUpload = true" class="upload-trigger">
          <el-icon><Upload /></el-icon>
          上传文档
        </el-button>
      </div>
    </div>

    <div class="document-list">
      <div v-if="documents.length === 0 && !loading" class="empty-docs">
        <div class="empty-icon">
          <svg viewBox="0 0 64 64" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="12" y="8" width="40" height="48" rx="4" opacity="0.3"/>
            <line x1="20" y1="20" x2="44" y2="20" opacity="0.5"/>
            <line x1="20" y1="28" x2="36" y2="28" opacity="0.5"/>
            <line x1="20" y1="36" x2="40" y2="36" opacity="0.5"/>
          </svg>
        </div>
        <p class="empty-text">暂无文档，点击上方按钮上传文档</p>
      </div>

      <div
        v-for="doc in documents"
        :key="doc.documentId"
        class="document-card"
      >
        <div class="doc-icon">
          <el-icon v-if="doc.fileType?.includes('pdf')"><Document /></el-icon>
          <el-icon v-else><Document /></el-icon>
        </div>
        <div class="doc-info">
          <div class="doc-name">{{ doc.fileName }}</div>
          <div class="doc-meta">
            <span class="doc-size">{{ formatSize(doc.fileSize) }}</span>
            <span class="doc-status" :class="doc.status">{{ getStatusLabel(doc.status) }}</span>
            <span v-if="doc.chunkCount > 0" class="doc-chunks">{{ doc.chunkCount }} 个分块</span>
            <span class="doc-date">{{ formatDate(doc.createdAt) }}</span>
          </div>
          <div v-if="doc.errorMessage" class="doc-error">
            <el-icon><WarningFilled /></el-icon>
            {{ doc.errorMessage }}
          </div>
        </div>
        <div class="doc-actions">
          <el-button
            v-if="doc.status === 'failed'"
            type="warning"
            link
            size="small"
            @click="handleRetry(doc.documentId)"
            class="retry-btn"
          >
            <el-icon><RefreshRight /></el-icon>
          </el-button>
          <el-button
            type="danger"
            link
            size="small"
            @click="handleDelete(doc.documentId)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <div v-if="total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        background
        @current-change="fetchDocuments"
      />
    </div>

    <el-dialog
      v-model="showUpload"
      title="上传文档"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-upload
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="5"
        accept=".pdf,.md,.txt"
        multiple
      >
        <div class="upload-area">
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">拖拽文件到此处，或 <em>点击上传</em></div>
          <div class="upload-hint">支持 PDF、Markdown、TXT 格式</div>
        </div>
      </el-upload>

      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :disabled="selectedFiles.length === 0">
          开始上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { listDocuments, uploadDocument, deleteDocument, retryDocument } from '@/api/document'
import type { DocumentResponse } from '@/types/document'
import { ElMessage } from 'element-plus'
import './DocumentView.css'

const documents = ref<DocumentResponse[]>([])
const showUpload = ref(false)
const selectedFiles = ref<File[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref('')
const loading = ref(false)

let pollTimer: ReturnType<typeof setInterval> | null = null

async function fetchDocuments() {
  loading.value = true
  try {
    const res = await listDocuments(currentPage.value, pageSize.value, filterStatus.value || undefined)
    documents.value = res.records
    total.value = res.total
  } catch {
    ElMessage.error('获取文档列表失败')
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  currentPage.value = 1
  fetchDocuments()
}

function handleFileChange(file: any) {
  selectedFiles.value.push(file.raw)
}

async function handleUpload() {
  for (const file of selectedFiles.value) {
    try {
      await uploadDocument(file)
      ElMessage.success(`${file.name} 上传成功`)
    } catch (error: unknown) {
      const msg = error instanceof Error ? error.message : '上传失败'
      ElMessage.error(msg)
    }
  }
  showUpload.value = false
  selectedFiles.value = []
  await fetchDocuments()
}

async function handleDelete(documentId: string) {
  try {
    await deleteDocument(documentId)
    ElMessage.success('文档已删除')
    await fetchDocuments()
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : '删除失败'
    ElMessage.error(msg)
  }
}

async function handleRetry(documentId: string) {
  try {
    await retryDocument(documentId)
    ElMessage.success('文档重试已提交')
    await fetchDocuments()
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : '重试失败'
    ElMessage.error(msg)
  }
}

function hasProcessingDocs(): boolean {
  return documents.value.some(doc => doc.status === 'processing')
}

function startPolling() {
  pollTimer = setInterval(() => {
    if (hasProcessingDocs()) {
      fetchDocuments()
    }
  }, 5000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    processing: '处理中',
    completed: '已完成',
    failed: '失败',
  }
  return map[status] || status
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  fetchDocuments()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>
