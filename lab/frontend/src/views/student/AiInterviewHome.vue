<template>
  <div class="ai-interview-home">
    <!-- Hero Section -->
    <div class="hero-section">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="hero-icon">
          <svg viewBox="0 0 48 48" fill="none"><circle cx="24" cy="24" r="22" stroke="currentColor" stroke-width="2" opacity=".18"/><circle cx="24" cy="24" r="14" stroke="currentColor" stroke-width="2.5"/><path d="M20 20v8l7-4z" fill="currentColor"/></svg>
        </div>
        <h1 class="hero-title">AI 智能面试</h1>
        <p class="hero-desc">通过 AI 驱动的多轮技术面试，帮助你检验知识掌握程度、发现薄弱环节，提前适应真实面试节奏。</p>
      </div>
    </div>

    <!-- Mode Cards -->
    <div class="mode-cards">
      <!-- 模拟面试 -->
      <div class="mode-card mock-card" @click="goMock">
        <div class="card-badge">自由练习</div>
        <div class="card-icon-wrap mock-icon">
          <svg viewBox="0 0 40 40" fill="none"><rect x="4" y="8" width="32" height="24" rx="4" stroke="currentColor" stroke-width="2"/><path d="M16 17v6l6-3z" fill="currentColor"/><circle cx="32" cy="12" r="4" fill="#10b981" opacity=".6"/></svg>
        </div>
        <h2>模拟面试</h2>
        <p>选择任意技术模块，自由练习，不限次数。面试结果仅在当前页面展示，不会保存记录。</p>
        <ul class="card-features">
          <li><span class="dot green"></span>不限次数</li>
          <li><span class="dot green"></span>自由选择模块</li>
          <li><span class="dot green"></span>即时评分反馈</li>
          <li><span class="dot gray"></span>结果不入库</li>
        </ul>
        <div class="card-action">
          <span>开始练习</span>
          <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path d="M10.293 3.293a1 1 0 011.414 0l6 6a1 1 0 010 1.414l-6 6a1 1 0 01-1.414-1.414L14.586 11H3a1 1 0 110-2h11.586l-4.293-4.293a1 1 0 010-1.414z"/></svg>
        </div>
      </div>

      <!-- 正式面试 -->
      <div class="mode-card formal-card" @click="goFormal">
        <div class="card-badge formal-badge">正式考核</div>
        <div class="card-icon-wrap formal-icon">
          <svg viewBox="0 0 40 40" fill="none"><rect x="6" y="4" width="28" height="32" rx="3" stroke="currentColor" stroke-width="2"/><path d="M13 14h14M13 20h14M13 26h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><circle cx="32" cy="8" r="5" fill="#f59e0b" opacity=".6"/><path d="M30.5 8l1 1 2-2" stroke="#fff" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </div>
        <h2>正式 AI 面试</h2>
        <p>实验室招新 / 内部考核专用通道。每人仅有 <strong>2 次</strong> 机会，结果将保存供管理员参考。</p>
        <ul class="card-features">
          <li><span class="dot amber"></span>限 2 次机会</li>
          <li><span class="dot amber"></span>结果永久保存</li>
          <li><span class="dot amber"></span>管理员可查看</li>
          <li><span class="dot gray"></span>仅供参考，非最终结果</li>
        </ul>
        <div class="card-action formal-action">
          <span>进入正式面试</span>
          <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path d="M10.293 3.293a1 1 0 011.414 0l6 6a1 1 0 010 1.414l-6 6a1 1 0 01-1.414-1.414L14.586 11H3a1 1 0 110-2h11.586l-4.293-4.293a1 1 0 010-1.414z"/></svg>
        </div>
      </div>
    </div>

    <!-- Disclaimer -->
    <div class="disclaimer">
      <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"/></svg>
      <span>AI 面试结果仅供参考，不代表最终录取结果。管理员将结合线下面试、项目经历等综合判断。</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useRoute } from 'vue-router'
import { resolveSurfacePathByRoute } from '@/utils/portal'

const route = useRoute()
const router = useRouter()
const mockPath = computed(() => resolveSurfacePathByRoute(route.path, '/student/ai-interview/mock'))
const formalPath = computed(() => resolveSurfacePathByRoute(route.path, '/student/ai-interview/formal'))

const goMock = () => router.push(mockPath.value)
const goFormal = () => router.push(formalPath.value)
</script>

<style scoped>
.ai-interview-home { max-width: 960px; margin: 0 auto; padding: 0 20px 40px; }

/* Hero */
.hero-section {
  position: relative; border-radius: 20px; overflow: hidden; margin-bottom: 32px;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}
.hero-bg {
  position: absolute; inset: 0;
  background: radial-gradient(ellipse at 70% 20%, rgba(14,165,233,.25) 0%, transparent 60%),
              radial-gradient(ellipse at 20% 80%, rgba(16,185,129,.15) 0%, transparent 50%);
}
.hero-content { position: relative; padding: 48px 40px 44px; text-align: center; }
.hero-icon { width: 56px; height: 56px; margin: 0 auto 18px; color: #38bdf8; }
.hero-title { font-size: 28px; font-weight: 700; color: #f1f5f9; margin: 0 0 12px; letter-spacing: .5px; }
.hero-desc { font-size: 15px; color: #94a3b8; line-height: 1.7; max-width: 560px; margin: 0 auto; }

/* Mode Cards */
.mode-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-bottom: 24px; }
.mode-card {
  position: relative; border-radius: 18px; padding: 32px 28px 28px; cursor: pointer;
  background: #fff; border: 1.5px solid rgba(148,163,184,.14);
  box-shadow: 0 4px 24px rgba(15,23,42,.05);
  transition: all .28s cubic-bezier(.4,0,.2,1);
}
.mode-card:hover { transform: translateY(-4px); box-shadow: 0 12px 40px rgba(15,23,42,.10); }
.mock-card:hover { border-color: rgba(16,185,129,.35); }
.formal-card:hover { border-color: rgba(245,158,11,.35); }

.card-badge {
  display: inline-block; font-size: 11px; font-weight: 600; padding: 3px 10px;
  border-radius: 20px; background: rgba(16,185,129,.1); color: #10b981; margin-bottom: 18px;
}
.formal-badge { background: rgba(245,158,11,.1); color: #d97706; }
.card-icon-wrap { width: 52px; height: 52px; margin-bottom: 16px; color: #64748b; }
.mock-icon { color: #10b981; }
.formal-icon { color: #f59e0b; }
.mode-card h2 { font-size: 20px; font-weight: 700; color: #0f172a; margin: 0 0 10px; }
.mode-card p { font-size: 14px; color: #64748b; line-height: 1.65; margin: 0 0 18px; }
.mode-card p strong { color: #ef4444; }
.card-features { list-style: none; padding: 0; margin: 0 0 22px; display: flex; flex-direction: column; gap: 8px; }
.card-features li { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #475569; }
.dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.dot.green { background: #10b981; }
.dot.amber { background: #f59e0b; }
.dot.gray { background: #cbd5e1; }
.card-action {
  display: inline-flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600;
  color: #10b981; transition: gap .2s;
}
.formal-action { color: #d97706; }
.mode-card:hover .card-action { gap: 10px; }

/* Disclaimer */
.disclaimer {
  display: flex; align-items: flex-start; gap: 8px; padding: 14px 18px;
  border-radius: 12px; background: rgba(245,158,11,.06); border: 1px solid rgba(245,158,11,.15);
}
.disclaimer svg { color: #f59e0b; flex-shrink: 0; margin-top: 1px; }
.disclaimer span { font-size: 13px; color: #92400e; line-height: 1.6; }

@media (max-width: 720px) {
  .ai-interview-home {
    max-width: none;
    padding: 0;
    display: grid;
    gap: 16px;
  }

  .hero-section {
    margin-bottom: 0;
    border-radius: 26px;
    background: linear-gradient(135deg, #15324b, #176b9a 54%, #19a7b8);
    box-shadow: 0 22px 48px rgba(23, 107, 154, 0.22);
  }

  .mode-cards { grid-template-columns: 1fr; }
  .hero-content { padding: 34px 22px 30px; }
  .hero-title { font-size: 28px; }

  .mode-cards {
    gap: 13px;
    margin-bottom: 0;
  }

  .mode-card {
    border-radius: 24px;
    padding: 20px;
    box-shadow: 0 16px 38px rgba(23, 32, 51, 0.08);
  }

  .mode-card:hover {
    transform: none;
  }

  .card-action {
    min-height: 44px;
    justify-content: center;
    border-radius: 16px;
    background: rgba(16, 185, 129, 0.1);
  }

  .formal-action {
    background: rgba(245, 158, 11, 0.12);
  }

  .disclaimer {
    border-radius: 20px;
  }
}
</style>
