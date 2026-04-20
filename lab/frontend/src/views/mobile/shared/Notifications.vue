<template>
  <div class="m-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Notification Center</p>
        <h1>消息中心</h1>
        <p>统一查看审核结果、系统提醒和跳转入口。</p>
      </div>
      <div class="hero-side">
        <span class="unread-chip">未读 {{ unreadCount }}</span>
        <el-button text @click="refresh">刷新</el-button>
      </div>
    </section>

    <section class="toolbar-card">
      <el-radio-group v-model="readFilter" size="small" @change="resetAndFetch">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="unread">未读</el-radio-button>
        <el-radio-button label="read">已读</el-radio-button>
      </el-radio-group>
      <el-button plain :disabled="!unreadCount" @click="markAllRead">全部已读</el-button>
    </section>

    <section class="card-list">
      <article v-for="row in rows" :key="row.id" class="notice-card">
        <div class="card-head">
          <div>
            <h3>{{ row.title || '系统消息' }}</h3>
            <p class="card-type">{{ row.notificationType || '通知' }}</p>
          </div>
          <span class="status-pill" :class="row.isRead === 1 ? 'read' : 'unread'">
            {{ row.isRead === 1 ? '已读' : '未读' }}
          </span>
        </div>
        <p class="card-content">{{ row.content || '暂无内容' }}</p>
        <div class="card-foot">
          <span>{{ formatDateTime(row.createTime) }}</span>
          <div class="actions">
            <el-button v-if="row.isRead !== 1" text @click="markRead(row)">标记已读</el-button>
            <el-button v-if="row.redirectPath" type="primary" text @click="openNotification(row)">打开</el-button>
          </div>
        </div>
      </article>

      <el-empty v-if="!loading && rows.length === 0" description="暂无消息" :image-size="88" />
    </section>

    <div class="load-more">
      <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
      <span v-else-if="rows.length" class="muted">已经到底了</span>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getMyNotifications,
  getUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead
} from '@/api/notifications'
import { mapPathToSurface } from '@/utils/portal'

const router = useRouter()
const loading = ref(false)
const loadingMore = ref(false)
const unreadCount = ref(0)
const rows = ref([])
const readFilter = ref('all')
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)

const hasMore = computed(() => rows.value.length < total.value)

const fetchUnreadCount = async () => {
  const response = await getUnreadNotificationCount()
  unreadCount.value = response.data?.unreadCount || 0
}

const buildQuery = (page) => ({
  pageNum: page,
  pageSize,
  isRead: readFilter.value === 'all' ? undefined : readFilter.value === 'read' ? 1 : 0
})

const fetchPage = async (page) => {
  const response = await getMyNotifications(buildQuery(page))
  const pageData = response.data || {}
  total.value = Number(pageData.total || 0)
  return pageData.records || []
}

const resetAndFetch = async () => {
  loading.value = true
  try {
    pageNum.value = 1
    const [list] = await Promise.all([fetchPage(1), fetchUnreadCount()])
    rows.value = list
  } finally {
    loading.value = false
  }
}

const fetchMore = async () => {
  if (loadingMore.value || !hasMore.value) {
    return
  }
  loadingMore.value = true
  try {
    const nextPage = pageNum.value + 1
    const list = await fetchPage(nextPage)
    pageNum.value = nextPage
    rows.value = rows.value.concat(list)
  } finally {
    loadingMore.value = false
  }
}

const refresh = async () => {
  await resetAndFetch()
}

const markRead = async (row) => {
  await markNotificationRead(row.id)
  ElMessage.success('已标记为已读')
  await refresh()
}

const markAllRead = async () => {
  await markAllNotificationsRead()
  ElMessage.success('全部消息已设为已读')
  await refresh()
}

const openNotification = async (row) => {
  if (row.isRead !== 1) {
    await markNotificationRead(row.id)
  }
  await refresh()
  if (row.redirectPath) {
    await router.push(mapPathToSurface(row.redirectPath, 'mobile'))
  }
}

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate()
  ).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  resetAndFetch()
})
</script>

<style scoped>
.m-page {
  display: grid;
  gap: 14px;
}

.hero-card {
  border-radius: 22px;
  padding: 18px;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.94), rgba(2, 132, 199, 0.88));
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
  margin: 0 0 6px;
  font-size: 22px;
}

.hero-card p {
  margin: 0;
  color: rgba(226, 232, 240, 0.9);
  line-height: 1.6;
}

.hero-side {
  display: grid;
  align-content: space-between;
  justify-items: end;
}

.unread-chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-weight: 700;
}

.toolbar-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  border-radius: 18px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.card-list {
  display: grid;
  gap: 10px;
}

.notice-card {
  border-radius: 18px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
  display: grid;
  gap: 10px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.card-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
}

.card-type {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.card-content {
  margin: 0;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
}

.card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  color: #64748b;
  font-size: 12px;
}

.actions {
  display: flex;
  gap: 8px;
}

.status-pill {
  height: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-pill.unread {
  color: #b91c1c;
  background: rgba(254, 226, 226, 0.9);
}

.status-pill.read {
  color: #475569;
  background: rgba(241, 245, 249, 0.9);
}

.load-more {
  display: flex;
  justify-content: center;
  padding-bottom: 4px;
}

.muted {
  color: #94a3b8;
  font-size: 12px;
}
</style>
