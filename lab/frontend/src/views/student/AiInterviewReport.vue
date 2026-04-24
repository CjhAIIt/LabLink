<template>
  <div class="interview-report">
    <div class="report-header">
      <div class="report-header-bg"></div>
      <div class="report-header-content">
        <span class="mode-tag" :class="store.isFormal ? 'formal' : 'mock'">{{ store.isFormal ? '正式面试' : '模拟练习' }}</span>
        <h1>面试评估报告</h1>
        <p>{{ store.moduleName }} · {{ store.questionCount }} 道题目</p>
      </div>
    </div>

    <div class="report-body" v-if="report">
      <!-- Score Ring -->
      <div class="score-section">
        <div class="score-ring" :class="scoreLevel">
          <svg viewBox="0 0 120 120">
            <circle cx="60" cy="60" r="52" fill="none" stroke="#f1f5f9" stroke-width="8"/>
            <circle cx="60" cy="60" r="52" fill="none" :stroke="scoreColor" stroke-width="8"
              stroke-linecap="round" :stroke-dasharray="dashArray" stroke-dashoffset="0"
              transform="rotate(-90 60 60)" style="transition: stroke-dasharray .8s ease"/>
          </svg>
          <div class="score-value">
            <span class="score-num">{{ report.score }}</span>
            <span class="score-label">综合评分</span>
          </div>
        </div>
        <div class="score-desc">
          <span class="level-badge" :class="scoreLevel">{{ levelText }}</span>
        </div>
      </div>

      <!-- Tags -->
      <div class="report-section" v-if="report.tags?.length">
        <h3>能力标签</h3>
        <div class="tag-list">
          <span class="ability-tag" v-for="(tag, i) in report.tags" :key="i">{{ tag }}</span>
        </div>
      </div>

      <!-- Summary -->
      <div class="report-section" v-if="report.summary">
        <h3>综合评价</h3>
        <div class="summary-card">{{ report.summary }}</div>
      </div>

      <!-- Strengths & Weaknesses -->
      <div class="two-col" v-if="report.strengths || report.weaknesses">
        <div class="report-section" v-if="report.strengths">
          <h3><span class="dot green"></span>优势</h3>
          <div class="detail-card green-card">{{ report.strengths }}</div>
        </div>
        <div class="report-section" v-if="report.weaknesses">
          <h3><span class="dot red"></span>待改进</h3>
          <div class="detail-card red-card">{{ report.weaknesses }}</div>
        </div>
      </div>

      <!-- Suggestions -->
      <div class="report-section" v-if="report.suggestions">
        <h3>学习建议</h3>
        <div class="detail-card blue-card">{{ report.suggestions }}</div>
      </div>
    </div>

    <!-- Disclaimer -->
    <div class="disclaimer">
      <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"/></svg>
      <span>AI 评分仅供参考，不代表最终录取结果。</span>
    </div>

    <div class="report-actions">
      <el-button @click="router.push(homePath)">返回首页</el-button>
      <el-button type="primary" @click="retry" v-if="!store.isFormal">再来一次</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAiInterviewStore } from '@/stores/aiInterview'
import { resolveSurfacePathByRoute } from '@/utils/portal'

const route = useRoute()
const router = useRouter()
const store = useAiInterviewStore()
const report = computed(() => store.report)
const homePath = computed(() => resolveSurfacePathByRoute(route.path, '/student/ai-interview'))
const sessionPath = computed(() => resolveSurfacePathByRoute(route.path, '/student/ai-interview/session'))

const scoreColor = computed(() => {
  const s = report.value?.score || 0
  if (s >= 80) return '#10b981'
  if (s >= 60) return '#f59e0b'
  return '#ef4444'
})
const scoreLevel = computed(() => {
  const s = report.value?.score || 0
  if (s >= 80) return 'high'
  if (s >= 60) return 'mid'
  return 'low'
})
const levelText = computed(() => {
  const s = report.value?.score || 0
  if (s >= 90) return '优秀'
  if (s >= 80) return '良好'
  if (s >= 60) return '合格'
  return '需加强'
})
const dashArray = computed(() => {
  const circumference = 2 * Math.PI * 52
  const pct = (report.value?.score || 0) / 100
  return `${circumference * pct} ${circumference * (1 - pct)}`
})

function retry() {
  const moduleId = store.moduleId
  const moduleName = store.moduleName
  store.reset()
  store.setMode('mock')
  store.setModule(moduleId, moduleName)
  router.push(sessionPath.value)
}
</script>

<style scoped>
.interview-report { max-width: 720px; margin: 0 auto; padding: 0 20px 40px; }

.report-header { position: relative; border-radius: 20px; overflow: hidden; margin-bottom: 32px; background: linear-gradient(135deg, #0f172a, #1e293b); }
.report-header-bg { position: absolute; inset: 0; background: radial-gradient(ellipse at 60% 30%, rgba(14,165,233,.22) 0%, transparent 60%), radial-gradient(ellipse at 30% 80%, rgba(16,185,129,.12) 0%, transparent 50%); }
.report-header-content { position: relative; padding: 40px 36px 36px; text-align: center; }
.mode-tag { display: inline-block; font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 20px; margin-bottom: 12px; }
.mode-tag.mock { background: rgba(16,185,129,.15); color: #34d399; }
.mode-tag.formal { background: rgba(245,158,11,.15); color: #fbbf24; }
.report-header h1 { font-size: 24px; font-weight: 700; color: #f1f5f9; margin: 0 0 8px; }
.report-header p { font-size: 14px; color: #94a3b8; margin: 0; }

.score-section { text-align: center; margin-bottom: 32px; }
.score-ring { position: relative; width: 120px; height: 120px; margin: 0 auto 12px; }
.score-ring svg { width: 100%; height: 100%; }
.score-value { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.score-num { font-size: 32px; font-weight: 800; color: #0f172a; }
.score-label { font-size: 11px; color: #94a3b8; }
.level-badge { font-size: 13px; font-weight: 600; padding: 4px 14px; border-radius: 20px; }
.level-badge.high { background: rgba(16,185,129,.1); color: #10b981; }
.level-badge.mid { background: rgba(245,158,11,.1); color: #d97706; }
.level-badge.low { background: rgba(239,68,68,.1); color: #ef4444; }

.report-section { margin-bottom: 24px; }
.report-section h3 { font-size: 15px; font-weight: 600; color: #1e293b; margin: 0 0 12px; display: flex; align-items: center; gap: 8px; }
.dot { width: 8px; height: 8px; border-radius: 50%; }
.dot.green { background: #10b981; }
.dot.red { background: #ef4444; }

.tag-list { display: flex; flex-wrap: wrap; gap: 8px; }
.ability-tag { font-size: 13px; padding: 5px 14px; border-radius: 20px; background: rgba(14,165,233,.08); color: #0369a1; border: 1px solid rgba(14,165,233,.12); }

.summary-card { padding: 18px 20px; border-radius: 14px; background: #f8fafc; border: 1px solid #f1f5f9; font-size: 14px; color: #475569; line-height: 1.7; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px; }
.detail-card { padding: 16px 18px; border-radius: 14px; font-size: 13px; color: #475569; line-height: 1.65; }
.green-card { background: rgba(16,185,129,.05); border: 1px solid rgba(16,185,129,.12); }
.red-card { background: rgba(239,68,68,.04); border: 1px solid rgba(239,68,68,.10); }
.blue-card { background: rgba(14,165,233,.05); border: 1px solid rgba(14,165,233,.10); }

.disclaimer { display: flex; align-items: center; gap: 6px; padding: 10px 14px; border-radius: 10px; background: rgba(245,158,11,.06); margin-bottom: 20px; }
.disclaimer svg { color: #f59e0b; flex-shrink: 0; }
.disclaimer span { font-size: 12px; color: #92400e; }

.report-actions { display: flex; justify-content: center; gap: 12px; }

@media (max-width: 600px) { .two-col { grid-template-columns: 1fr; } }
</style>
