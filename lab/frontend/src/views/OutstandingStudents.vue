<template>
  <div class="student-showcase-page">
    <div class="bg-grid"></div>
    <div class="ambient-glow"></div>

    <header class="hero play-anim">
      <div class="hero-content">
        <span class="hero-tag anim-title">精英风采</span>
        <h1 class="anim-middle">优秀毕业生殿堂</h1>
        <p class="anim-bottom">探索从各个实验室走出的杰出学子，他们的故事将激励你不断前行。</p>
        <div class="hero-actions anim-bottom">
          <router-link class="ghost-btn" to="/intro">
            <el-icon><Back /></el-icon> 返回介绍页
          </router-link>
        </div>
      </div>
    </header>

    <main class="main-content">
      <div class="labs-grid">
        <router-link
          v-for="(lab, index) in labs"
          :key="lab.id"
          :to="`/outstanding-students/${lab.id}`"
          class="lab-card-link reveal-item"
          :style="{ transitionDelay: `${index * 0.1}s` }"
        >
          <div class="lab-card">
            <div class="card-icon">
              <el-icon><Trophy /></el-icon>
            </div>
            <h3>{{ lab.labName }}</h3>
            <p>{{ lab.labDesc }}</p>
            <div class="lab-footer">
              <span>查看毕业生</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </router-link>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowRight, Back, Trophy } from '@element-plus/icons-vue'
import { getAllLabs } from '@/api/lab'

const labs = ref([])

onMounted(async () => {
  const res = await getAllLabs()
  if (res.code === 200) {
    labs.value = res.data.records
    // Trigger animations after data load
    setTimeout(() => {
      const items = document.querySelectorAll('.reveal-item');
      items.forEach(el => el.classList.add('visible'));
    }, 100);
  }
})
</script>

<style scoped>
/* ================== 1. 变量定义 (适配 Light Theme) ================== */
.student-showcase-page {
  --bg: #f8fafc;
  --panel: #ffffff;
  --line: #e2e8f0;
  --text: #1e293b;
  --muted: #64748b;
  --brand: #0ea5e9;
  --brand2: #f59e0b; /* Gold for Trophy */
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
  background: radial-gradient(circle at 20% 40%, rgba(245, 158, 11, 0.1) 0%, transparent 45%),
              radial-gradient(circle at 80% 60%, rgba(14, 165, 233, 0.1) 0%, transparent 45%);
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
  background: rgba(245, 158, 11, 0.1);
  color: var(--brand2);
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

.hero p {
  font-size: 18px;
  color: var(--muted);
  max-width: 600px;
  margin: 0 auto 32px;
  line-height: 1.6;
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
  max-width: 1280px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.labs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 32px;
}

.lab-card-link {
  text-decoration: none;
  display: block;
}

.lab-card {
  background: var(--panel);
  border-radius: var(--radius);
  padding: 32px;
  box-shadow: var(--shadow);
  border: 1px solid var(--line);
  height: 100%;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1), box-shadow 0.3s;
}

.lab-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px -10px rgba(14, 165, 233, 0.15);
  border-color: var(--brand2);
}

.card-icon {
  width: 56px; height: 56px; border-radius: 16px; margin-bottom: 24px; 
  display: flex; align-items: center; justify-content: center; font-size: 28px; 
  background: rgba(245, 158, 11, 0.1); color: var(--brand2);
}

.lab-card h3 {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 12px;
  color: var(--text);
}

.lab-card p {
  color: var(--muted);
  line-height: 1.7;
  flex-grow: 1;
  font-size: 15px;
  margin-bottom: 24px;
}

.lab-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--brand2);
  font-weight: 600;
  font-size: 14px;
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
