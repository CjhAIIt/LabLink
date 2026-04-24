<template>
  <div class="formal-interview">
    <div class="page-header">
      <button class="back-btn" @click="router.push(homePath)">
        <el-icon><ArrowLeft /></el-icon> 返回
      </button>
      <div>
        <h1>正式 AI 面试</h1>
        <p class="subtitle">实验室招新 / 内部考核专用。请认真对待每一次机会。</p>
      </div>
    </div>

    <!-- Status Banner -->
    <div class="status-banner" :class="{ 'no-chance': chances <= 0 }">
      <div class="chance-display">
        <div class="chance-number">{{ chances }}</div>
        <div class="chance-label">剩余次数</div>
      </div>
      <div class="chance-divider"></div>
      <div class="chance-info">
        <p v-if="chances > 0">你还有 <strong>{{ chances }}</strong> 次正式面试机会。开始后将自动扣减 1 次。</p>
        <p v-else>你的正式面试次数已用完。如有特殊情况，请联系管理员。</p>
        <span class="chance-tip">AI 面试结果仅供参考，不代表最终录取结果。</span>
      </div>
    </div>

    <!-- Module Selection (same as mock but with formal styling) -->
    <template v-if="chances > 0">
      <div class="section-label">选择面试模块</div>
      <div class="module-grid">
        <div v-for="mod in modules" :key="mod.id"
          class="module-card" :class="{ active: selectedModule === mod.id }"
          @click="selectedModule = mod.id">
          <div class="module-icon" :style="{ background: mod.color }">{{ mod.icon }}</div>
          <div class="module-info">
            <h3>{{ mod.name }}</h3>
            <p>{{ mod.description }}</p>
          </div>
          <div class="check-mark" v-if="selectedModule === mod.id">✓</div>
        </div>
      </div>

      <!-- Confirm Dialog -->
      <div class="start-section" v-if="selectedModule">
        <div class="start-card formal-start">
          <div class="start-info">
            <h3>确认开始正式面试</h3>
            <p>模块：<strong>{{ selectedModuleName }}</strong> · 开始后将消耗 1 次机会，请确认准备就绪。</p>
          </div>
          <el-button type="warning" size="large" @click="confirmStart" :loading="starting">
            确认开始
          </el-button>
        </div>
      </div>
    </template>

    <!-- History Records -->
    <div class="section-label" style="margin-top: 32px;">我的正式面试记录</div>
    <div class="records-list" v-if="records.length">
      <div class="record-item" v-for="r in records" :key="r.id">
        <div class="record-left">
          <span class="record-attempt">#{{ r.attemptNo }}</span>
          <div>
            <div class="record-module">{{ r.moduleName }}</div>
            <div class="record-time">{{ r.startTime }}</div>
          </div>
        </div>
        <div class="record-right">
          <span class="record-score" :class="scoreClass(r.score)">{{ r.score }}分</span>
          <el-tag :type="r.status === '已完成' ? 'success' : 'info'" size="small">{{ r.status }}</el-tag>
        </div>
      </div>
    </div>
    <div class="empty-records" v-else>暂无正式面试记录</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useAiInterviewStore } from '@/stores/aiInterview'
import { getFormalChances, getInterviewModules, getMyFormalRecords } from '@/api/aiInterview'
import { normalizeInterviewModules } from '@/utils/aiInterview'
import { resolveSurfacePathByRoute } from '@/utils/portal'

const route = useRoute()
const router = useRouter()
const store = useAiInterviewStore()
const chances = ref(2)
const selectedModule = ref(null)
const starting = ref(false)
const records = ref([])
const homePath = computed(() => resolveSurfacePathByRoute(route.path, '/student/ai-interview'))
const sessionPath = computed(() => resolveSurfacePathByRoute(route.path, '/student/ai-interview/session'))

const defaultModules = [
  { id: 1, name: 'Java 基础', description: '面向对象、集合、多线程、JVM', icon: '☕', color: 'linear-gradient(135deg,#f97316,#ea580c)' },
  { id: 2, name: 'Spring Boot', description: 'IoC、AOP、自动配置、Web 开发', icon: '🌱', color: 'linear-gradient(135deg,#22c55e,#16a34a)' },
  { id: 3, name: '前端基础', description: 'HTML/CSS/JS、DOM、ES6+', icon: '🎨', color: 'linear-gradient(135deg,#3b82f6,#2563eb)' },
  { id: 4, name: 'Vue / React', description: '组件化、响应式、状态管理', icon: '⚡', color: 'linear-gradient(135deg,#8b5cf6,#7c3aed)' },
  { id: 5, name: 'MySQL', description: '索引、事务、SQL 优化', icon: '🗄️', color: 'linear-gradient(135deg,#06b6d4,#0891b2)' },
  { id: 6, name: '数据结构与算法', description: '链表、树、排序、DP', icon: '🧮', color: 'linear-gradient(135deg,#ec4899,#db2777)' },
  { id: 7, name: '计算机网络', description: 'TCP/IP、HTTP、DNS', icon: '🌐', color: 'linear-gradient(135deg,#14b8a6,#0d9488)' },
  { id: 8, name: '操作系统', description: '进程、内存、文件系统', icon: '💻', color: 'linear-gradient(135deg,#64748b,#475569)' },
  { id: 9, name: '项目经历表达', description: '项目介绍、难点与方案', icon: '📋', color: 'linear-gradient(135deg,#f59e0b,#d97706)' },
]
const modules = ref(defaultModules)

const selectedModuleName = computed(() => {
  const mod = modules.value.find(m => m.id === selectedModule.value)
  return mod?.name || mod?.moduleName || ''
})
const scoreClass = (s) => s >= 80 ? 'high' : s >= 60 ? 'mid' : 'low'

onMounted(async () => {
  try { const r = await getFormalChances(); chances.value = r?.data?.remaining ?? 2 } catch {}
  try { const r = await getInterviewModules(); modules.value = normalizeInterviewModules(r?.data, defaultModules) } catch {}
  try { const r = await getMyFormalRecords(); if (r?.data?.length) records.value = r.data } catch {}
})

async function confirmStart() {
  try {
    await ElMessageBox.confirm('正式面试开始后将消耗 1 次机会，确定开始？', '确认', { type: 'warning', confirmButtonText: '开始', cancelButtonText: '取消' })
    starting.value = true
    const mod = modules.value.find(m => m.id === selectedModule.value)
    const moduleName = mod?.name || mod?.moduleName || ''
    if (!mod || !moduleName) {
      starting.value = false
      return
    }
    store.reset()
    store.setMode('formal')
    store.setModule(mod.id, moduleName)
    router.push(sessionPath.value)
  } catch { /* cancelled */ }
}
</script>

<style scoped>
.formal-interview { max-width: 900px; margin: 0 auto; padding: 0 20px 40px; }
.page-header { display: flex; align-items: flex-start; gap: 16px; margin-bottom: 24px; }
.back-btn { display: inline-flex; align-items: center; gap: 4px; background: none; border: 1px solid #e2e8f0; border-radius: 10px; padding: 8px 14px; font-size: 13px; color: #64748b; cursor: pointer; transition: all .2s; margin-top: 2px; }
.back-btn:hover { border-color: #f59e0b; color: #d97706; }
.page-header h1 { font-size: 22px; font-weight: 700; color: #0f172a; margin: 0 0 4px; }
.subtitle { font-size: 14px; color: #64748b; margin: 0; }

.status-banner { display: flex; align-items: center; gap: 24px; padding: 24px 28px; border-radius: 16px; background: linear-gradient(135deg, rgba(245,158,11,.08), rgba(251,191,36,.04)); border: 1px solid rgba(245,158,11,.18); margin-bottom: 28px; }
.status-banner.no-chance { background: linear-gradient(135deg, rgba(239,68,68,.06), rgba(239,68,68,.02)); border-color: rgba(239,68,68,.18); }
.chance-display { text-align: center; min-width: 64px; }
.chance-number { font-size: 36px; font-weight: 800; color: #d97706; line-height: 1; }
.no-chance .chance-number { color: #ef4444; }
.chance-label { font-size: 12px; color: #92400e; margin-top: 4px; }
.chance-divider { width: 1px; height: 48px; background: rgba(245,158,11,.2); }
.chance-info p { font-size: 14px; color: #44403c; margin: 0 0 4px; }
.chance-info strong { color: #d97706; }
.chance-tip { font-size: 12px; color: #a8a29e; }

.section-label { font-size: 13px; font-weight: 600; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 14px; }
.module-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 28px; }
.module-card { position: relative; display: flex; align-items: center; gap: 14px; padding: 18px 16px; border-radius: 14px; border: 1.5px solid #f1f5f9; background: #fff; cursor: pointer; transition: all .22s; }
.module-card:hover { border-color: #cbd5e1; box-shadow: 0 4px 16px rgba(15,23,42,.06); }
.module-card.active { border-color: #f59e0b; background: rgba(245,158,11,.03); box-shadow: 0 4px 20px rgba(245,158,11,.10); }
.module-icon { width: 42px; height: 42px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.module-info h3 { font-size: 14px; font-weight: 600; color: #1e293b; margin: 0 0 3px; }
.module-info p { font-size: 12px; color: #94a3b8; margin: 0; }
.check-mark { position: absolute; top: 10px; right: 10px; width: 22px; height: 22px; border-radius: 50%; background: #f59e0b; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; }

.start-card { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 24px 28px; border-radius: 16px; background: linear-gradient(135deg, rgba(245,158,11,.08), rgba(234,88,12,.04)); border: 1px solid rgba(245,158,11,.18); }
.start-info h3 { font-size: 16px; font-weight: 600; color: #0f172a; margin: 0 0 6px; }
.start-info p { font-size: 13px; color: #64748b; margin: 0; }
.start-info strong { color: #d97706; }

.records-list { display: flex; flex-direction: column; gap: 10px; }
.record-item { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-radius: 12px; background: #fff; border: 1px solid #f1f5f9; }
.record-left { display: flex; align-items: center; gap: 14px; }
.record-attempt { font-size: 13px; font-weight: 700; color: #94a3b8; background: #f8fafc; padding: 4px 10px; border-radius: 8px; }
.record-module { font-size: 14px; font-weight: 600; color: #1e293b; }
.record-time { font-size: 12px; color: #94a3b8; }
.record-right { display: flex; align-items: center; gap: 12px; }
.record-score { font-size: 18px; font-weight: 700; }
.record-score.high { color: #10b981; }
.record-score.mid { color: #f59e0b; }
.record-score.low { color: #ef4444; }
.empty-records { text-align: center; padding: 40px; color: #94a3b8; font-size: 14px; background: #fafbfc; border-radius: 12px; }

@media (max-width: 720px) { .module-grid { grid-template-columns: 1fr 1fr; } .status-banner { flex-direction: column; text-align: center; } .chance-divider { width: 80%; height: 1px; } }
</style>
