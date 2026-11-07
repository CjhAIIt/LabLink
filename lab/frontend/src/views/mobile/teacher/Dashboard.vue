<template>
  <div class="m-page">
    <section class="hero">
      <div class="hero-text">
        <p class="hero-eyebrow">教师端</p>
        <h1 class="hero-title">你好，{{ userStore.realName || '老师' }}</h1>
        <p class="hero-sub">发起实验室创建申请，并查看审批进度。</p>
      </div>
      <button class="hero-refresh" type="button" :disabled="loading" @click="refresh">
        <el-icon :size="18"><Refresh /></el-icon>
      </button>
    </section>

    <section class="grid">
      <div class="stat-card">
        <p class="stat-label">创建申请</p>
        <strong class="stat-value">{{ createApplyTotal ?? '--' }}</strong>
      </div>
      <div class="stat-card">
        <p class="stat-label">我的学院</p>
        <strong class="stat-value">{{ userStore.userInfo?.collegeName || userStore.userInfo?.college || '--' }}</strong>
      </div>
    </section>

    <section class="quick">
      <button class="quick-card" type="button" @click="router.push('/m/teacher/create-applies')">
        <div class="quick-icon">
          <el-icon :size="22"><Edit /></el-icon>
        </div>
        <div class="quick-body">
          <strong>实验室创建申请</strong>
          <span>发起申请 / 查看进度</span>
        </div>
        <el-icon :size="18" class="quick-arrow"><ArrowRight /></el-icon>
      </button>
      <button class="quick-card" type="button" @click="router.push('/m/teacher/notices')">
        <div class="quick-icon notices">
          <el-icon :size="22"><Bell /></el-icon>
        </div>
        <div class="quick-body">
          <strong>公告中心</strong>
          <span>最新通知与提醒</span>
        </div>
        <el-icon :size="18" class="quick-arrow"><ArrowRight /></el-icon>
      </button>
    </section>
  </div>
</template>

<script setup>
import { ArrowRight, Bell, Edit, Refresh } from '@element-plus/icons-vue'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLabCreateApplyPage } from '@/api/labCreateApplies'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const createApplyTotal = ref(null)

const refresh = async () => {
  loading.value = true
  try {
    const res = await getLabCreateApplyPage({ pageNum: 1, pageSize: 1 })
    const page = res.data || {}
    createApplyTotal.value = page.total ?? page.totalCount ?? page.count ?? 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refresh()
})
</script>

<style scoped>
.m-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero {
  position: relative;
  border-radius: 18px;
  padding: 18px 16px;
  background: linear-gradient(135deg, #0f172a 0%, #7c3aed 100%);
  color: #ffffff;
}

.hero-text {
  max-width: 280px;
}

.hero-eyebrow {
  font-size: 12px;
  opacity: 0.85;
  margin-bottom: 6px;
}

.hero-title {
  font-size: 20px;
  line-height: 1.2;
  margin-bottom: 8px;
}

.hero-sub {
  font-size: 13px;
  opacity: 0.85;
  line-height: 1.5;
}

.hero-refresh {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  border-radius: 16px;
  padding: 14px 14px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.stat-label {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 16px;
  color: #0f172a;
}

.quick {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.92);
  text-align: left;
}

.quick-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: linear-gradient(135deg, #7c3aed 0%, #a78bfa 100%);
}

.quick-icon.notices {
  background: linear-gradient(135deg, #0f766e 0%, #5eead4 100%);
}

.quick-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.quick-body strong {
  color: #0f172a;
  font-size: 14px;
}

.quick-body span {
  color: #64748b;
  font-size: 12px;
}

.quick-arrow {
  color: #94a3b8;
}
</style>
