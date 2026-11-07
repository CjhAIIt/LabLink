<template>
  <div class="m-page">
    <section class="hero">
      <div class="hero-text">
        <p class="hero-eyebrow">学生端</p>
        <h1 class="hero-title">你好，{{ userStore.realName || '同学' }}</h1>
        <p class="hero-sub">快速查看实验室动态、公告与我的申请。</p>
      </div>
      <button class="hero-refresh" type="button" :disabled="loading" @click="refresh">
        <el-icon :size="18"><Refresh /></el-icon>
      </button>
    </section>

    <section class="grid">
      <div class="stat-card">
        <p class="stat-label">实验室总数</p>
        <strong class="stat-value">{{ stats.labCount ?? '--' }}</strong>
      </div>
      <div class="stat-card">
        <p class="stat-label">学院数量</p>
        <strong class="stat-value">{{ stats.collegeCount ?? '--' }}</strong>
      </div>
      <div class="stat-card">
        <p class="stat-label">我的申请</p>
        <strong class="stat-value">{{ myApplyTotal ?? '--' }}</strong>
      </div>
      <div class="stat-card">
        <p class="stat-label">我的实验室</p>
        <strong class="stat-value">{{ userStore.userInfo?.labId ? `#${userStore.userInfo.labId}` : '未加入' }}</strong>
      </div>
    </section>

    <section class="quick">
      <button class="quick-card" type="button" @click="router.push('/m/student/labs')">
        <div class="quick-icon labs">
          <el-icon :size="22"><OfficeBuilding /></el-icon>
        </div>
        <div class="quick-body">
          <strong>实验室总览</strong>
          <span>浏览与申请加入</span>
        </div>
        <el-icon :size="18" class="quick-arrow"><ArrowRight /></el-icon>
      </button>
      <button class="quick-card" type="button" @click="router.push('/m/student/applications')">
        <div class="quick-icon apps">
          <el-icon :size="22"><Document /></el-icon>
        </div>
        <div class="quick-body">
          <strong>我的申请</strong>
          <span>查看审核进度</span>
        </div>
        <el-icon :size="18" class="quick-arrow"><ArrowRight /></el-icon>
      </button>
      <button class="quick-card" type="button" @click="router.push('/m/student/notices')">
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

    <section class="panel">
      <header class="panel-header">
        <h2>最新公告</h2>
        <button class="panel-link" type="button" @click="router.push('/m/student/notices')">更多</button>
      </header>
      <div v-if="latestNotices.length" class="notice-list">
        <button
          v-for="item in latestNotices"
          :key="item.id"
          class="notice-item"
          type="button"
          @click="openNotice(item)"
        >
          <div class="notice-main">
            <strong class="notice-title">{{ item.title || '公告' }}</strong>
            <span class="notice-sub">{{ formatTime(item.createTime || item.createdAt) }}</span>
          </div>
          <el-icon :size="18" class="notice-arrow"><ArrowRight /></el-icon>
        </button>
      </div>
      <el-empty v-else description="暂无公告" :image-size="70" />
    </section>

    <el-drawer v-model="drawerVisible" :with-header="false" size="92%">
      <div class="drawer">
        <div class="drawer-head">
          <strong class="drawer-title">{{ activeNotice?.title || '公告详情' }}</strong>
          <button class="drawer-close" type="button" @click="drawerVisible = false">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </div>
        <p class="drawer-meta">{{ formatTime(activeNotice?.createTime || activeNotice?.createdAt) }}</p>
        <div class="drawer-body">{{ activeNotice?.content || '暂无内容' }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ArrowRight, Bell, Close, Document, OfficeBuilding, Refresh } from '@element-plus/icons-vue'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLabStats } from '@/api/lab'
import { getMyLabApplyPage } from '@/api/labApplies'
import { getLatestNotices } from '@/api/notices'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const stats = ref({})
const myApplyTotal = ref(null)
const latestNotices = ref([])

const drawerVisible = ref(false)
const activeNotice = ref(null)

const formatTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const openNotice = (notice) => {
  activeNotice.value = notice
  drawerVisible.value = true
}

const refresh = async () => {
  loading.value = true
  try {
    const [statsRes, noticeRes, applyRes] = await Promise.all([
      getLabStats(),
      getLatestNotices({ size: 5 }),
      getMyLabApplyPage({ pageNum: 1, pageSize: 1 })
    ])
    stats.value = statsRes.data || {}
    latestNotices.value = noticeRes.data || []
    const page = applyRes.data || {}
    myApplyTotal.value = page.total ?? page.totalCount ?? page.count ?? 0
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
  background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 100%);
  color: #ffffff;
  overflow: hidden;
}

.hero-text {
  max-width: 260px;
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
  font-size: 18px;
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
}

.quick-icon.labs {
  background: linear-gradient(135deg, #2563eb 0%, #60a5fa 100%);
}

.quick-icon.apps {
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

.panel {
  border-radius: 18px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.panel-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}

.panel-header h2 {
  font-size: 15px;
  color: #0f172a;
}

.panel-link {
  border: 0;
  background: transparent;
  color: #2563eb;
  font-weight: 600;
  font-size: 13px;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: #ffffff;
  text-align: left;
}

.notice-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.notice-title {
  color: #0f172a;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notice-sub {
  color: #64748b;
  font-size: 12px;
}

.notice-arrow {
  color: #94a3b8;
}

.drawer {
  padding: 14px 14px calc(14px + env(safe-area-inset-bottom)) 14px;
}

.drawer-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.drawer-title {
  font-size: 16px;
  color: #0f172a;
}

.drawer-close {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.drawer-meta {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 10px;
}

.drawer-body {
  color: #334155;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
}
</style>
