<template>
  <div class="m-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Teacher Mobile</p>
        <h1>{{ userStore.realName || '老师' }}，你好</h1>
        <p>{{ hasWorkspace ? '查看当前实验室考勤、创建申请进度和公告提醒。' : '发起实验室创建申请，查看当前进度与公告提醒。' }}</p>
      </div>
      <button class="refresh-btn" type="button" :disabled="loading" @click="refresh">刷新</button>
    </section>

    <section class="metric-grid">
      <article class="metric-card"><span>创建申请</span><strong>{{ createApplyTotal }}</strong></article>
      <article class="metric-card"><span>所属学院</span><strong>{{ userStore.userInfo?.collegeName || userStore.userInfo?.college || '-' }}</strong></article>
      <article class="metric-card"><span>未读消息</span><strong>{{ unreadCount }}</strong></article>
      <article class="metric-card"><span>{{ hasWorkspace ? '实验室状态' : '最新公告' }}</span><strong>{{ hasWorkspace ? '已绑定' : latestNotices.length }}</strong></article>
    </section>

    <section class="quick-grid">
      <button v-for="item in quickActions" :key="item.path" class="quick-card" type="button" @click="router.push(item.path)">
        <strong>{{ item.title }}</strong>
        <span>{{ item.description }}</span>
      </button>
    </section>

    <section class="panel-card">
      <header class="panel-head">
        <h2>最近公告</h2>
        <button class="text-btn" type="button" @click="router.push('/m/teacher/notices')">更多</button>
      </header>
      <div v-if="latestNotices.length" class="notice-list">
        <article v-for="item in latestNotices" :key="item.id" class="notice-card">
          <strong>{{ item.title || '公告' }}</strong>
          <p>{{ item.content || '暂无内容' }}</p>
          <span>{{ formatDate(item.publishTime || item.createTime || item.createdAt) }}</span>
        </article>
      </div>
      <el-empty v-else description="暂无公告" :image-size="72" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLabCreateApplyPage } from '@/api/labCreateApplies'
import { getLatestNotices } from '@/api/notices'
import { getUnreadNotificationCount } from '@/api/notifications'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const createApplyTotal = ref(0)
const unreadCount = ref(0)
const latestNotices = ref([])
const hasWorkspace = computed(() => Boolean(userStore.userInfo?.labId))

const quickActions = computed(() =>
  [
    { path: '/m/teacher/create-applies', title: '创建申请', description: '发起新的实验室创建流程' },
    hasWorkspace.value ? { path: '/m/teacher/attendance', title: '考勤查看', description: '查看当前场次、动态码和签到状态' } : null,
    { path: '/m/teacher/notifications', title: '消息中心', description: '统一查看审核反馈' },
    { path: '/m/teacher/notices', title: '公告查看', description: '查看最新通知' }
  ].filter(Boolean)
)

const refresh = async () => {
  loading.value = true
  try {
    const [applyRes, unreadRes, noticeRes] = await Promise.all([
      getLabCreateApplyPage({ pageNum: 1, pageSize: 1 }),
      getUnreadNotificationCount(),
      getLatestNotices({ size: 3 })
    ])
    createApplyTotal.value = Number(applyRes.data?.total || 0)
    unreadCount.value = unreadRes.data?.unreadCount || 0
    latestNotices.value = noticeRes.data || []
  } finally {
    loading.value = false
  }
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
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.94), rgba(124, 58, 237, 0.88));
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

.refresh-btn {
  height: fit-content;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.12);
  color: #f8fafc;
  border-radius: 14px;
  padding: 10px 14px;
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
.notice-card p,
.notice-card span {
  color: #64748b;
}

.metric-card strong,
.quick-card strong,
.notice-card strong,
.panel-head h2 {
  color: #0f172a;
}

.metric-card strong {
  font-size: 20px;
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
  color: #7c3aed;
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
  display: grid;
  gap: 8px;
}

.notice-card p {
  margin: 0;
  line-height: 1.7;
}

@media (max-width: 480px) {
  .metric-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
