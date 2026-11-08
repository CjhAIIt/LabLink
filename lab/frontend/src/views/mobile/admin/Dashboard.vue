<template>
  <div class="m-page">
    <section class="hero hero--top">
      <div class="hero-text">
        <p class="hero-eyebrow">管理端</p>
        <h1 class="hero-title">你好，{{ userStore.realName || '管理员' }}</h1>
        <p class="hero-sub">{{ scopeLabel }}治理概览与待办事项。</p>
      </div>
      <button class="hero-refresh" type="button" :disabled="loading" @click="refresh">
        <el-icon :size="18"><Refresh /></el-icon>
      </button>
    </section>

    <section class="grid">
      <div v-for="card in metricCards" :key="card.label" class="stat-card">
        <p class="stat-label">{{ card.label }}</p>
        <strong class="stat-value">{{ card.value }}</strong>
        <p class="stat-tip">{{ card.tip }}</p>
      </div>
    </section>

    <section v-if="pendingApprovals.length" class="panel">
      <header class="panel-header">
        <span>待办审批</span>
        <span class="panel-count">{{ overview.pendingApprovalTotal ?? 0 }} 项</span>
      </header>
      <div class="panel-body">
        <button
          v-for="item in pendingApprovals"
          :key="item.label"
          class="pending-item"
          type="button"
          @click="goRoute(item.route)"
        >
          <div class="pending-main">
            <span class="pending-label">{{ item.label }}</span>
            <strong class="pending-value">{{ item.value }}</strong>
          </div>
          <p class="pending-desc">{{ item.description }}</p>
          <el-icon class="pending-arrow"><ArrowRight /></el-icon>
        </button>
      </div>
    </section>

    <section class="panel">
      <header class="panel-header">
        <span>最新公告</span>
        <span class="panel-count">{{ notices.length }} 条</span>
      </header>
      <div v-if="notices.length" class="notice-list">
        <button v-for="item in notices" :key="item.id" class="notice-item" type="button" @click="openNotice(item)">
          <div class="notice-main">
            <strong class="notice-title">{{ item.title || '公告' }}</strong>
            <span class="notice-sub">{{ item.content || '暂无内容' }}</span>
          </div>
          <el-icon class="notice-arrow"><ArrowRight /></el-icon>
        </button>
      </div>
      <el-empty v-else-if="!loading" description="暂无公告" :image-size="70" />
    </section>

    <el-drawer v-model="drawerVisible" :with-header="false" size="92%">
      <div class="drawer">
        <div class="drawer-head">
          <strong class="drawer-title">{{ activeNotice?.title || '公告详情' }}</strong>
          <button class="drawer-close" type="button" @click="drawerVisible = false">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </div>
        <p class="drawer-meta">{{ formatTime(activeNotice?.publishTime || activeNotice?.createTime) }}</p>
        <div class="drawer-body">{{ activeNotice?.content || '暂无内容' }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ArrowRight, Close, Refresh } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLatestNotices } from '@/api/notices'
import { getOverviewStatistics } from '@/api/statistics'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const overview = ref({})
const notices = ref([])

const drawerVisible = ref(false)
const activeNotice = ref(null)

const scopeLabel = computed(() => {
  if (overview.value.scopeName) {
    return overview.value.scopeName
  }
  if (overview.value.scopeType === 'college') {
    return '当前学院'
  }
  if (overview.value.scopeType === 'lab') {
    return '当前实验室'
  }
  return '全校'
})

const metricCards = computed(() => [
  { label: '实验室总数', value: overview.value.labCount ?? 0, tip: '纳入平台的实验室规模' },
  { label: '正式成员', value: overview.value.formalMemberCount ?? 0, tip: '当前有效成员总量' },
  { label: '待处理事项', value: overview.value.pendingApprovalTotal ?? 0, tip: '审批链上的待办总数' },
  { label: '开放招新', value: overview.value.openPlanCount ?? 0, tip: '正在开放的招新计划' }
])

const pendingApprovals = computed(() => overview.value.pendingApprovals || [])

const formatTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const goRoute = (route) => {
  if (!route) return
  router.push(route.replace(/^\/admin/, '/m/admin'))
}

const openNotice = (notice) => {
  activeNotice.value = notice
  drawerVisible.value = true
}

const refresh = async () => {
  loading.value = true
  try {
    const [statsRes, noticeRes] = await Promise.all([getOverviewStatistics(), getLatestNotices({ limit: 5 })])
    overview.value = statsRes.data || {}
    notices.value = noticeRes.data || []
  } finally {
    loading.value = false
  }
}

onMounted(refresh)
</script>

<style scoped>
.panel-count {
  font-size: var(--ll-text-xs);
  font-weight: 500;
  color: var(--ll-text-tertiary);
}

.pending-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px var(--ll-space-3);
  width: 100%;
  text-align: left;
  padding: var(--ll-space-3) 0;
  border: none;
  border-bottom: 1px solid var(--ll-border-subtle);
  background: transparent;
}

.pending-item:last-child {
  border-bottom: none;
}

.pending-main {
  display: flex;
  align-items: baseline;
  gap: var(--ll-space-2);
}

.pending-label {
  font-size: var(--ll-text-sm);
  font-weight: 500;
  color: var(--ll-text-primary);
}

.pending-value {
  font-size: var(--ll-text-lg);
  font-weight: 600;
  color: var(--ll-text-brand);
  font-variant-numeric: tabular-nums;
}

.pending-desc {
  grid-column: 1;
  font-size: var(--ll-text-xs);
  line-height: 1.6;
  color: var(--ll-text-tertiary);
}

.pending-arrow {
  grid-column: 2;
  grid-row: 1 / span 2;
  align-self: center;
  color: var(--ll-text-disabled);
}
</style>
