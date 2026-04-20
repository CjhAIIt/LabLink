<template>
  <div class="lab-students-page">
    <div class="bg-grid"></div>
    <div class="ambient-glow"></div>

    <header class="hero play-anim">
      <div class="hero-content">
        <span class="hero-tag anim-title">{{ lab ? lab.labName : '实验室' }}</span>
        <h1 class="anim-middle">优秀毕业生风采</h1>
        <div class="hero-actions anim-bottom">
          <router-link class="ghost-btn" to="/outstanding-students">
            <el-icon><Back /></el-icon> 返回列表
          </router-link>
        </div>
      </div>
    </header>

    <main class="main-content">
      <div v-if="students.length > 0" class="students-grid">
        <div 
          v-for="(student, index) in students" 
          :key="student.id" 
          class="student-card reveal-item"
          :style="{ transitionDelay: `${index * 0.1}s` }"
        >
          <div class="student-header">
            <el-avatar :size="80" class="student-avatar" :src="student.avatarUrl">
              {{ student.name ? student.name.charAt(0) : '' }}
            </el-avatar>
            <div class="student-info">
              <h2>{{ student.name }}</h2>
              <p class="student-major">{{ student.major }}</p>
              <div class="job-tag">
                <el-icon><Suitcase /></el-icon>
                <span>{{ student.company }} · {{ student.position }}</span>
              </div>
            </div>
          </div>
          <div class="student-body">
            <div class="text-content">{{ student.description }}</div>
          </div>
        </div>
      </div>
      <div v-else class="empty-state reveal-item visible">
        <el-empty description="暂无优秀毕业生数据" />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Back, Suitcase } from '@element-plus/icons-vue'
import { getLabById } from '@/api/lab'
import { getGraduateList } from '@/api/graduate'

const route = useRoute()
const labId = route.params.labId
const lab = ref(null)
const students = ref([])

onMounted(async () => {
  if (labId) {
    // 获取实验室信息
    const labRes = await getLabById(labId)
    if (labRes.code === 200) {
      lab.value = labRes.data
    }

    // 获取毕业生列表
    const gradRes = await getGraduateList({
      pageNum: 1,
      pageSize: 100,
      labId: labId
    })
    if (gradRes.code === 200) {
      students.value = gradRes.data.records
      setTimeout(() => {
        const items = document.querySelectorAll('.reveal-item');
        items.forEach(el => el.classList.add('visible'));
      }, 100);
    }
  }
})
</script>

<style scoped>
/* ================== 1. 变量定义 (适配 Light Theme) ================== */
.lab-students-page {
  --bg: #f8fafc;
  --panel: #ffffff;
  --line: #e2e8f0;
  --text: #1e293b;
  --muted: #64748b;
  --brand: #0ea5e9;
  --shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.1);
  --radius: 20px;
  --max: 1280px;

  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background-color: var(--bg);
  color: var(--text);
  min-height: 100vh;
}

/* ================== 2. 背景与组件 ================== */
.bg-grid {
  position: fixed; inset: 0; pointer-events: none; opacity: 0.6; z-index: 0;
  background-image: linear-gradient(#e2e8f0 1px, transparent 1px), linear-gradient(90deg, #e2e8f0 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: linear-gradient(to bottom, rgba(0,0,0,1), rgba(0,0,0,0));
}
.ambient-glow {
  position: fixed; inset: 0; pointer-events: none; z-index: 0;
  background: radial-gradient(circle at 20% 40%, rgba(14, 165, 233, 0.1) 0%, transparent 45%),
              radial-gradient(circle at 80% 60%, rgba(59, 130, 246, 0.1) 0%, transparent 45%);
}

.hero {
  padding: 80px 24px;
  text-align: center;
  position: relative;
  z-index: 1;
}

.hero-content {
  max-width: 800px;
  margin: 0 auto;
}

.hero-tag {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.1);
  color: var(--brand);
  font-weight: 600;
  margin-bottom: 24px;
}

.hero h1 {
  font-size: 48px;
  font-weight: 800;
  margin: 0 0 16px;
  color: #1e293b;
  letter-spacing: -1px;
}

.ghost-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 999px;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;
  border: 1px solid var(--line);
  color: var(--text);
  background: white;
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
}
.ghost-btn:hover {
  border-color: var(--brand);
  color: var(--brand);
  transform: translateY(-2px);
}

.main-content {
  padding: 40px 24px 80px;
  max-width: 1000px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.students-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 32px;
}

.student-card {
  background: var(--panel);
  border-radius: var(--radius);
  padding: 40px;
  box-shadow: var(--shadow);
  border: 1px solid var(--line);
  transition: transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1), box-shadow 0.3s;
}

.student-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 40px -10px rgba(14, 165, 233, 0.15);
  border-color: var(--brand);
}

.student-header {
  display: flex;
  align-items: flex-start;
  gap: 32px;
  margin-bottom: 32px;
  border-bottom: 1px solid var(--line);
  padding-bottom: 32px;
}

.student-avatar {
  background-color: var(--brand);
  color: #fff;
  font-size: 32px;
  flex-shrink: 0;
  border: 4px solid white;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.student-info h2 {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 8px;
  color: var(--text);
}

.student-major {
  color: var(--muted);
  margin: 0 0 16px;
  font-size: 16px;
}

.job-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(14, 165, 233, 0.1);
  color: var(--brand);
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
}

.text-content {
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
  font-size: 16px;
}

.empty-state {
  padding: 40px;
  text-align: center;
}

/* Animations */
.anim-bottom, .anim-middle, .anim-title { opacity: 0; will-change: transform, opacity; }
@keyframes fadeUp { from { opacity: 0; transform: translateY(40px); } to { opacity: 1; transform: translateY(0); } }

.play-anim .anim-bottom { animation: fadeUp 0.8s cubic-bezier(0.2, 0.8, 0.2, 1) 0.1s forwards; }
.play-anim .anim-middle { animation: fadeUp 0.8s cubic-bezier(0.2, 0.8, 0.2, 1) 0.3s forwards; }
.play-anim .anim-title  { animation: fadeUp 0.8s cubic-bezier(0.2, 0.8, 0.2, 1) 0.5s forwards; }

.reveal-item {
  opacity: 0; transform: translateY(50px);
  transition: opacity 0.8s cubic-bezier(0.2, 0.8, 0.2, 1), transform 0.8s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.reveal-item.visible { opacity: 1; transform: translateY(0); }
</style>
