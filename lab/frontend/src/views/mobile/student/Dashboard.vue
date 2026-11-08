<template>
  <div class="m-page">
    <section class="hero hero--top">
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

