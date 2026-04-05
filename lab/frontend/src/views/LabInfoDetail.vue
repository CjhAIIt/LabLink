<template>
  <div class="lab-info-detail-page">
    <div class="bg-grid"></div>
    <div class="ambient-glow"></div>

    <div v-if="lab" class="lab-content">
      <header class="hero play-anim">
        <div class="hero-content">
          <span class="hero-tag anim-title">实验室详情</span>
          <h1 class="anim-middle">{{ lab.labName }}</h1>
          <div class="hero-actions anim-bottom">
            <router-link class="ghost-btn" to="/intro">
              <el-icon><Back /></el-icon> 返回列表
            </router-link>
          </div>
        </div>
      </header>

      <main class="main-content">
        <div class="info-grid">
          <!-- 基本信息卡片 -->
          <div class="info-card reveal-item" style="transition-delay: 0.1s">
            <div class="card-header">
              <el-icon><InfoFilled /></el-icon>
              <h3>基本信息</h3>
            </div>
            <div class="info-list">
              <div class="info-item">
                <span class="label">成立时间</span>
                <span class="value">{{ lab.foundingDate || '暂无' }}</span>
              </div>
              <div class="info-item">
                <span class="label">指导老师</span>
                <span class="value">{{ lab.advisors || '暂无' }}</span>
              </div>
              <div class="info-item">
                <span class="label">现任管理员</span>
                <span class="value">{{ lab.currentAdmins || '暂无' }}</span>
              </div>
            </div>
          </div>

          <!-- 获得奖项卡片 -->
          <div class="info-card reveal-item" style="transition-delay: 0.2s">
            <div class="card-header">
              <el-icon><Trophy /></el-icon>
              <h3>获得奖项</h3>
            </div>
            <div class="text-content">{{ lab.awards || '暂无' }}</div>
          </div>

          <!-- 基础信息卡片 -->
          <div class="info-card reveal-item" style="transition-delay: 0.3s">
            <div class="card-header">
              <el-icon><DataBoard /></el-icon>
              <h3>基础信息</h3>
            </div>
            <div class="text-content">{{ lab.basicInfo || '暂无' }}</div>
          </div>

          <!-- 实验室介绍卡片 -->
          <div class="info-card full-width reveal-item" style="transition-delay: 0.4s">
            <div class="card-header">
              <el-icon><Document /></el-icon>
              <h3>实验室介绍</h3>
            </div>
            <div class="text-content">{{ lab.labDesc || '暂无' }}</div>
          </div>
        </div>

        <el-card shadow="never" class="panel-card reveal-item" style="transition-delay: 0.05s">
          <template #header>
            <div class="panel-header">
              <span>优秀学长</span>
              <el-tag type="warning" effect="plain">{{ graduates.length }} 位</el-tag>
            </div>
          </template>
          <div v-if="graduates.length" class="graduate-grid">
            <div v-for="item in graduates" :key="item.id" class="graduate-card">
              <div class="graduate-head">
                <el-avatar :size="46" :src="item.avatarUrl">{{ (item.name || 'A').charAt(0) }}</el-avatar>
                <div>
                  <div class="graduate-name">{{ item.name }}</div>
                  <div class="graduate-meta">
                    <span>{{ item.major || '-' }}</span>
                    <span v-if="item.graduationYear">· {{ item.graduationYear }}</span>
                  </div>
                </div>
              </div>
              <div class="graduate-body">
                <div class="graduate-line"><span class="label">去向</span><span>{{ item.company || '-' }} {{ item.position || '' }}</span></div>
                <div class="graduate-desc">{{ item.description || '暂无介绍' }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无优秀毕业生数据" />
        </el-card>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Back, InfoFilled, Trophy, DataBoard, Document } from '@element-plus/icons-vue'
import { getLabById } from '@/api/lab'
import { getGraduateList } from '@/api/graduate'

const route = useRoute()
const lab = ref(null)
const graduates = ref([])

onMounted(async () => {
  const labId = route.params.id
  if (labId) {
    const [labRes, graduateRes] = await Promise.all([
      getLabById(labId),
      getGraduateList({ pageNum: 1, pageSize: 12, labId })
    ])
    if (labRes.code === 200) {
      lab.value = labRes.data
    }
    if (graduateRes.code === 200) {
      graduates.value = graduateRes.data.records || []
    }
    setTimeout(() => {
      const items = document.querySelectorAll('.reveal-item')
      items.forEach((el) => el.classList.add('visible'))
    }, 100)
  }
})
</script>

<style scoped>
/* ================== 1. 变量定义 (适配 Light Theme) ================== */
.lab-info-detail-page {
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
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.panel-card {
  margin-bottom: 32px;
  border-radius: var(--radius);
  border: 1px solid var(--line);
  box-shadow: var(--shadow);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.graduate-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.graduate-card {
  border: 1px solid var(--line);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  padding: 18px;
  display: grid;
  gap: 12px;
}

.graduate-head {
  display: flex;
  gap: 12px;
  align-items: center;
}

.graduate-name {
  font-weight: 800;
  color: var(--text);
}

.graduate-meta {
  margin-top: 4px;
  color: var(--muted);
  font-size: 13px;
  font-weight: 600;
}

.graduate-body {
  display: grid;
  gap: 10px;
  color: #334155;
}

.graduate-line {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
}

.graduate-line .label {
  color: var(--muted);
  font-weight: 700;
}

.graduate-desc {
  color: #334155;
  line-height: 1.7;
  font-size: 13px;
  white-space: pre-wrap;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 32px;
}

.info-card {
  background: var(--panel);
  border-radius: var(--radius);
  padding: 32px;
  box-shadow: var(--shadow);
  border: 1px solid var(--line);
  transition: transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1), box-shadow 0.3s;
}

.info-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 40px -10px rgba(14, 165, 233, 0.15);
  border-color: var(--brand);
}

.info-card.full-width {
  grid-column: 1 / -1;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  color: var(--brand);
  font-size: 20px;
}

.card-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.info-item:last-child {
  border-bottom: none;
}

.label {
  color: var(--muted);
  font-size: 14px;
}

.value {
  font-weight: 600;
  color: var(--text);
}

.text-content {
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
  font-size: 16px;
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
