<template>
  <div class="eval-view">
    <div class="view-header">
      <h1 class="view-title">评估面板</h1>
      <el-button type="primary" @click="handleRunEval" :loading="evalRunning">
        <el-icon><VideoPlay /></el-icon>
        运行评估
      </el-button>
    </div>

    <div class="eval-content">
      <div class="metrics-grid">
        <div class="metric-card">
          <div class="metric-label">忠实性</div>
          <div class="metric-value" :style="{ color: getScoreColor(results.faithfulness) }">
            {{ (results.faithfulness * 100).toFixed(1) }}%
          </div>
          <div class="metric-threshold">目标: ≥ 85%</div>
          <div class="metric-bar">
            <div
              class="metric-bar-fill"
              :style="{ width: `${results.faithfulness * 100}%`, backgroundColor: getScoreColor(results.faithfulness) }"
            ></div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-label">上下文精确度</div>
          <div class="metric-value" :style="{ color: getScoreColor(results.contextPrecision) }">
            {{ (results.contextPrecision * 100).toFixed(1) }}%
          </div>
          <div class="metric-threshold">目标: ≥ 70%</div>
          <div class="metric-bar">
            <div
              class="metric-bar-fill"
              :style="{ width: `${results.contextPrecision * 100}%`, backgroundColor: getScoreColor(results.contextPrecision) }"
            ></div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-label">答案准确率</div>
          <div class="metric-value" :style="{ color: getScoreColor(results.accuracy) }">
            {{ (results.accuracy * 100).toFixed(1) }}%
          </div>
          <div class="metric-threshold">目标: ≥ 80%</div>
          <div class="metric-bar">
            <div
              class="metric-bar-fill"
              :style="{ width: `${results.accuracy * 100}%`, backgroundColor: getScoreColor(results.accuracy) }"
            ></div>
          </div>
        </div>
      </div>

      <div class="cost-section">
        <h2 class="section-title">成本估算</h2>
        <div class="cost-controls">
          <div class="control-group">
            <label>top_k</label>
            <el-slider v-model="costParams.topK" :min="1" :max="15" show-input />
          </div>
          <div class="control-group">
            <label>temperature</label>
            <el-slider v-model="costParams.temperature" :min="0" :max="1" :step="0.1" show-input />
          </div>
          <div class="control-group">
            <label>reranker</label>
            <el-switch v-model="costParams.enableReranker" />
          </div>
          <el-button type="primary" @click="handleGetCost">查询</el-button>
        </div>

        <div v-if="costReport" class="cost-result">
          <div class="cost-summary">
            <div class="cost-item">
              <span class="cost-label">每1000次调用预估成本</span>
              <span class="cost-value">{{ costReport.estimatedCostPer1000Calls.toFixed(4) }}</span>
            </div>
          </div>
          <div class="sensitivity-table" v-if="Object.keys(costReport.sensitivityAnalysis).length > 0">
            <h3>敏感性分析</h3>
            <el-table :data="sensitivityData" stripe>
              <el-table-column prop="param" label="参数" />
              <el-table-column prop="impact" label="影响系数" />
            </el-table>
          </div>
        </div>
      </div>

      <div class="log-section">
        <h2 class="section-title">示例日志</h2>
        <div class="log-entries">
          <div class="log-entry">
            <span class="log-level info">INFO</span>
            <span class="log-time">2026-05-16 10:30:00</span>
            <span class="log-msg">Chat completed - retrieval_time_ms=245, generation_time_ms=3200, total_time_ms=3445, retrieved_docs=5, avg_similarity=0.82</span>
          </div>
          <div class="log-entry">
            <span class="log-level warn">WARN</span>
            <span class="log-time">2026-05-16 10:28:12</span>
            <span class="log-msg">Low similarity score (0.45) for query "公司年假政策" - returning fallback response</span>
          </div>
          <div class="log-entry">
            <span class="log-level info">INFO</span>
            <span class="log-time">2026-05-16 10:25:33</span>
            <span class="log-msg">Document ingestion completed: employee_handbook.pdf (23 chunks)</span>
          </div>
          <div class="log-entry">
            <span class="log-level debug">DEBUG</span>
            <span class="log-time">2026-05-16 10:25:30</span>
            <span class="log-msg">Embedding batch processed: 23 chunks, 0.8s</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { runEvaluation, getResults, getCostEstimate } from '@/api/eval'
import type { EvalResult, CostReport } from '@/types/eval'
import { ElMessage } from 'element-plus'
import './EvalView.css'

const results = ref<EvalResult>({
  faithfulness: 0,
  contextPrecision: 0,
  accuracy: 0,
  metrics: {},
  completedAt: null,
})

const evalRunning = ref(false)
const costReport = ref<CostReport | null>(null)

const costParams = ref({
  topK: 5,
  temperature: 0.7,
  enableReranker: false,
})

const sensitivityData = computed(() => {
  if (!costReport.value) return []
  return Object.entries(costReport.value.sensitivityAnalysis).map(([param, impact]) => ({
    param,
    impact: impact.toFixed(4),
  }))
})

async function handleRunEval() {
  evalRunning.value = true
  try {
    await runEvaluation()
    const res = await getResults()
    results.value = res
    ElMessage.success('评估完成')
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : '评估失败'
    ElMessage.error(msg)
  } finally {
    evalRunning.value = false
  }
}

async function handleGetCost() {
  try {
    costReport.value = await getCostEstimate(
      costParams.value.topK,
      costParams.value.enableReranker,
      costParams.value.temperature
    )
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : '查询失败'
    ElMessage.error(msg)
  }
}

function getScoreColor(score: number): string {
  if (score >= 0.85) return '#6aab7c'
  if (score >= 0.7) return '#d4a76a'
  return '#d47a6a'
}

onMounted(async () => {
  try {
    const res = await getResults()
    results.value = res
  } catch {
    // ignore
  }
})
</script>
