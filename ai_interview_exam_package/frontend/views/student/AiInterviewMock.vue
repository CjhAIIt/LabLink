<template>
  <div class="mock-interview">
    <div class="page-header">
      <button class="back-btn" @click="$router.push('/student/ai-interview')">
        <el-icon><ArrowLeft /></el-icon> 返回
      </button>
      <div>
        <h1>模拟面试</h1>
        <p class="subtitle">选择一个技术模块，开始自由练习。结果不会保存，可随时重来。</p>
      </div>
    </div>

    <!-- Module Selection -->
    <div class="section-label">选择面试模块</div>
    <div class="module-grid">
      <div
        v-for="mod in modules" :key="mod.id"
        class="module-card" :class="{ active: selectedModule === mod.id }"
        @click="selectedModule = mod.id"
      >
        <div class="module-icon" :style="{ background: mod.color }">{{ mod.icon }}</div>
        <div class="module-info">
          <h3>{{ mod.name }}</h3>
          <p>{{ mod.description }}</p>
        </div>
        <div class="check-mark" v-if="selectedModule === mod.id">
          <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
        </div>
      </div>
    </div>

    <!-- Start Section -->
    <div class="start-section" v-if="selectedModule">
      <div class="start-card">
        <div class="start-info">
          <h3>准备开始</h3>
          <p>模块：<strong>{{ selectedModuleName }}</strong> · AI 将根据该模块连续提问，每次一个问题，支持追问。</p>
        </div>
        <el-button type="primary" size="large" @click="startMock" :loading="starting">
          开始模拟面试
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useAiInterviewStore } from '@/stores/aiInterview'
import { getInterviewModules } from '@/api/aiInterview'
import { normalizeInterviewModules } from '@/utils/aiInterview'

const router = useRouter()
const store = useAiInterviewStore()
const selectedModule = ref(null)
const starting = ref(false)

// 默认模块（后端未就绪时的 fallback）
const defaultModules = [
  { id: 1, name: 'Java 基础', description: '面向对象、集合、多线程、JVM 等核心知识', icon: '☕', color: 'linear-gradient(135deg,#f97316,#ea580c)' },
  { id: 2, name: 'Spring Boot', description: 'IoC、AOP、自动配置、Web 开发等', icon: '🌱', color: 'linear-gradient(135deg,#22c55e,#16a34a)' },
  { id: 3, name: '前端基础', description: 'HTML/CSS/JS、DOM、事件、ES6+ 等', icon: '🎨', color: 'linear-gradient(135deg,#3b82f6,#2563eb)' },
  { id: 4, name: 'Vue / React', description: '组件化、响应式、路由、状态管理等', icon: '⚡', color: 'linear-gradient(135deg,#8b5cf6,#7c3aed)' },
  { id: 5, name: 'MySQL', description: '索引、事务、SQL 优化、锁机制等', icon: '🗄️', color: 'linear-gradient(135deg,#06b6d4,#0891b2)' },
  { id: 6, name: '数据结构与算法', description: '链表、树、排序、动态规划等', icon: '🧮', color: 'linear-gradient(135deg,#ec4899,#db2777)' },
  { id: 7, name: '计算机网络', description: 'TCP/IP、HTTP、DNS、网络安全等', icon: '🌐', color: 'linear-gradient(135deg,#14b8a6,#0d9488)' },
  { id: 8, name: '操作系统', description: '进程、内存管理、文件系统、调度等', icon: '💻', color: 'linear-gradient(135deg,#64748b,#475569)' },
  { id: 9, name: '项目经历表达', description: '项目介绍、技术选型、难点与解决方案', icon: '📋', color: 'linear-gradient(135deg,#f59e0b,#d97706)' },
]

const modules = ref(defaultModules)

const selectedModuleName = computed(() => {
  const m = modules.value.find(m => m.id === selectedModule.value)
  return m ? (m.name || m.moduleName || '') : ''
})

onMounted(async () => {
  try {
    const res = await getInterviewModules()
    modules.value = normalizeInterviewModules(res?.data, defaultModules)
  } catch { /* use defaults */ }
})

function startMock() {
  starting.value = true
  const mod = modules.value.find(m => m.id === selectedModule.value)
  const moduleName = mod?.name || mod?.moduleName || ''
  if (!mod || !moduleName) {
    starting.value = false
    return
  }
  store.reset()
  store.setMode('mock')
  store.setModule(mod.id, moduleName)
  router.push('/student/ai-interview/session')
}
</script>

<style scoped>
.mock-interview { max-width: 900px; margin: 0 auto; padding: 0 20px 40px; }
.page-header { display: flex; align-items: flex-start; gap: 16px; margin-bottom: 28px; }
.back-btn {
  display: inline-flex; align-items: center; gap: 4px; background: none; border: 1px solid #e2e8f0;
  border-radius: 10px; padding: 8px 14px; font-size: 13px; color: #64748b; cursor: pointer;
  transition: all .2s; margin-top: 2px;
}
.back-btn:hover { border-color: #0ea5e9; color: #0ea5e9; }
.page-header h1 { font-size: 22px; font-weight: 700; color: #0f172a; margin: 0 0 4px; }
.subtitle { font-size: 14px; color: #64748b; margin: 0; }

.section-label { font-size: 13px; font-weight: 600; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 14px; }

.module-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 28px; }
.module-card {
  position: relative; display: flex; align-items: center; gap: 14px; padding: 18px 16px;
  border-radius: 14px; border: 1.5px solid #f1f5f9; background: #fff; cursor: pointer;
  transition: all .22s cubic-bezier(.4,0,.2,1);
}
.module-card:hover { border-color: #cbd5e1; box-shadow: 0 4px 16px rgba(15,23,42,.06); }
.module-card.active { border-color: #0ea5e9; background: rgba(14,165,233,.03); box-shadow: 0 4px 20px rgba(14,165,233,.10); }
.module-icon {
  width: 42px; height: 42px; border-radius: 12px; display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}
.module-info h3 { font-size: 14px; font-weight: 600; color: #1e293b; margin: 0 0 3px; }
.module-info p { font-size: 12px; color: #94a3b8; margin: 0; line-height: 1.4; }
.check-mark {
  position: absolute; top: 10px; right: 10px; width: 22px; height: 22px;
  border-radius: 50%; background: #0ea5e9; color: #fff; display: flex; align-items: center; justify-content: center;
}
.check-mark svg { width: 14px; height: 14px; }

.start-section { margin-top: 8px; }
.start-card {
  display: flex; align-items: center; justify-content: space-between; gap: 20px;
  padding: 24px 28px; border-radius: 16px;
  background: linear-gradient(135deg, rgba(14,165,233,.06), rgba(16,185,129,.04));
  border: 1px solid rgba(14,165,233,.12);
}
.start-info h3 { font-size: 16px; font-weight: 600; color: #0f172a; margin: 0 0 6px; }
.start-info p { font-size: 13px; color: #64748b; margin: 0; }
.start-info strong { color: #0ea5e9; }

@media (max-width: 720px) { .module-grid { grid-template-columns: 1fr 1fr; } }
@media (max-width: 480px) { .module-grid { grid-template-columns: 1fr; } }
</style>
