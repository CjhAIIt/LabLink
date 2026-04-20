<template>
  <div class="m-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Student Mobile</p>
        <h1>{{ userStore.realName || '同学' }}，你好</h1>
        <p>快速查看实验室动态、报名进度和消息提醒。</p>
      </div>
      <button class="refresh-btn" type="button" :disabled="loading" @click="refresh">刷新</button>
    </section>

    <section class="metric-grid">
      <article class="metric-card"><span>实验室总数</span><strong>{{ stats.labCount ?? 0 }}</strong></article>
      <article class="metric-card"><span>学院数量</span><strong>{{ stats.collegeCount ?? 0 }}</strong></article>
      <article class="metric-card"><span>我的申请</span><strong>{{ myApplyTotal }}</strong></article>
      <article class="metric-card"><span>未读消息</span><strong>{{ unreadCount }}</strong></article>
    </section>

    <section class="quick-grid">
      <button v-for="item in quickActions" :key="item.path" class="quick-card" type="button" @click="router.push(item.path)">
        <strong>{{ item.title }}</strong>
        <span>{{ item.description }}</span>
      </button>
    </section>

    <section class="panel-card">
      <header class="panel-head">
        <h2>最新公告</h2>
        <button class="text-btn" type="button" @click="router.push('/m/student/notices')">更多</button>
      </header>
      <div v-if="latestNotices.length" class="notice-list">
        <button v-for="item in latestNotices" :key="item.id" class="notice-card" type="button" @click="openNotice(item)">
          <strong>{{ item.title || '公告' }}</strong>
          <span>{{ formatDate(item.publishTime || item.createTime || item.createdAt) }}</span>
        </button>
      </div>
      <el-empty v-else description="暂无公告" :image-size="72" />
    </section>

    <el-drawer v-model="drawerVisible" :with-header="false" size="92%">
      <div class="drawer-body">
        <div class="drawer-head">
          <strong>{{ activeNotice?.title || '公告详情' }}</strong>
          <button class="drawer-close" type="button" @click="drawerVisible = false">关闭</button>
        </div>
        <p class="drawer-meta">{{ formatDate(activeNotice?.publishTime || activeNotice?.createTime || activeNotice?.createdAt) }}</p>
        <div class="drawer-content">{{ activeNotice?.content || '暂无内容' }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLabStats } from '@/api/lab'
import { getMyLabApplyPage } from '@/api/labApplies'
import { getLatestNotices } from '@/api/notices'
import { getUnreadNotificationCount } from '@/api/notifications'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const stats = ref({})
const myApplyTotal = ref(0)
const unreadCount = ref(0)
const latestNotices = ref([])
const drawerVisible = ref(false)
const activeNotice = ref(null)
const hasLabAccess = computed(() => Boolean(userStore.userInfo?.labId))

const quickActions = computed(() =>
  [
    { path: '/m/student/labs', title: '实验室广场', description: '浏览实验室并提交申请' },
    { path: '/m/student/applications', title: '我的申请', description: '跟踪审核进度和反馈' },
    hasLabAccess.value ? { path: '/m/student/attendance', title: '移动签到', description: '输入动态签到码，查看考勤历史' } : null,
    { path: '/m/student/notifications', title: '消息中心', description: '集中查看系统通知' }
  ].filter(Boolean)
)

const refresh = async () => {
  loading.value = true
  try {
    const [statsRes, applyRes, noticesRes, unreadRes] = await Promise.all([
      getLabStats(),
      getMyLabApplyPage({ pageNum: 1, pageSize: 1 }),
      getLatestNotices({ size: 4 }),
      getUnreadNotificationCount()
    ])
    stats.value = statsRes.data || {}
    myApplyTotal.value = Number(applyRes.data?.total || 0)
    latestNotices.value = noticesRes.data || []
    unreadCount.value = unreadRes.data?.unreadCount || 0
  } finally {
    loading.value = false
  }
}

const openNotice = (item) => {
  activeNotice.value = item
  drawerVisible.value = true
}

const formatDate = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  refresh()
})
</script>

<style scoped>
.m-page {
  display: grid;
  gap: 14px;
}

.hero-card,
.metric-card,
.quick-card,
.panel-card {
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.hero-card {
  padding: 18px;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.94), rgba(37, 99, 235, 0.88));
  color: #f8fafc;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.82;
}

.hero-card h1 {
  margin: 0 0 8px;
  font-size: 24px;
}

.hero-card p {
  margin: 0;
  line-height: 1.6;
  color: rgba(226, 232, 240, 0.9);
}

.refresh-btn,
.drawer-close {
  height: fit-content;
  border-radius: 14px;
  padding: 10px 14px;
}

.refresh-btn {
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.12);
  color: #f8fafc;
}

.metric-grid,
.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.metric-card,
.quick-card {
  padding: 14px;
  display: grid;
  gap: 6px;
}

.metric-card span,
.quick-card span,
.notice-card span,
.drawer-meta {
  color: #64748b;
}

.metric-card strong,
.quick-card strong,
.notice-card strong,
.panel-head h2,
.drawer-head strong {
  color: #0f172a;
}

.metric-card strong {
  font-size: 24px;
}

.quick-card {
  text-align: left;
}

.panel-card {
  padding: 14px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.panel-head h2 {
  margin: 0;
  font-size: 16px;
}

.text-btn {
  border: 0;
  background: transparent;
  color: #2563eb;
  font-weight: 700;
}

.notice-list {
  display: grid;
  gap: 10px;
}

.notice-card {
  border-radius: 16px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.86);
  display: flex;
  justify-content: space-between;
  gap: 12px;
  text-align: left;
}

.drawer-body {
  padding: 14px 14px calc(14px + env(safe-area-inset-bottom));
}

.drawer-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.drawer-content {
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
}

@media (max-width: 480px) {
  .metric-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
