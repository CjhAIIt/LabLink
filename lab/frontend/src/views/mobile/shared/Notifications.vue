<template>
  <MobilePageContainer>
    <section class="message-hero">
      <div>
        <span class="mobile-kicker">Messages</span>
        <h1>消息中心</h1>
        <p>审核通知、考勤提醒、任务消息和系统公告统一收纳。</p>
      </div>
      <div class="message-hero__side">
        <strong>{{ unreadCount }}</strong>
        <span>未读</span>
      </div>
    </section>

    <section class="message-toolbar">
      <div class="message-tabs">
        <button
          v-for="item in readTabs"
          :key="item.value"
          type="button"
          :class="{ active: readFilter === item.value }"
          @click="setReadFilter(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
      <el-button plain :disabled="!unreadCount" @click="markAllRead">一键已读</el-button>
    </section>

    <div class="message-type-row">
      <button
        v-for="item in typeTabs"
        :key="item.value"
        class="message-type-chip"
        :class="{ active: typeFilter === item.value }"
        type="button"
        @click="typeFilter = item.value"
      >
        {{ item.label }}
      </button>
    </div>

    <section class="message-card-flow">
      <article v-for="row in visibleRows" :key="row.id" class="message-card" :class="{ unread: row.isRead !== 1 }">
        <div class="message-card__head">
          <div>
            <span>{{ typeLabel(row.notificationType) }}</span>
            <h2>{{ row.title || '系统消息' }}</h2>
          </div>
          <MobileStatusTag :type="row.isRead === 1 ? 'default' : 'danger'" :label="row.isRead === 1 ? '已读' : '未读'" />
        </div>

        <p>{{ row.content || '暂无内容' }}</p>

        <footer>
          <span>{{ formatDateTime(row.createTime) }}</span>
          <div>
            <el-button v-if="row.isRead !== 1" plain @click="markRead(row)">标记已读</el-button>
            <el-button v-if="row.redirectPath" type="primary" @click="openNotification(row)">打开</el-button>
          </div>
        </footer>
      </article>

      <MobileEmptyState
        v-if="!loading && visibleRows.length === 0"
        icon="Message"
        title="暂无消息"
        description="新的审核、考勤、任务或系统消息会显示在这里。"
      />
    </section>

    <div class="load-more">
      <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
      <span v-else-if="rows.length" class="muted">已经到底了</span>
    </div>
  </MobilePageContainer>
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
import MobileEmptyState from '@/components/mobile/MobileEmptyState.vue'
import MobilePageContainer from '@/components/mobile/MobilePageContainer.vue'
import MobileStatusTag from '@/components/mobile/MobileStatusTag.vue'
import { mapPathToSurface } from '@/utils/portal'

const router = useRouter()
const loading = ref(false)
const loadingMore = ref(false)
const unreadCount = ref(0)
const rows = ref([])
const readFilter = ref('all')
const typeFilter = ref('all')
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)

const readTabs = [
  { label: '全部', value: 'all' },
  { label: '未读', value: 'unread' },
  { label: '已读', value: 'read' }
]

const typeTabs = [
  { label: '全部', value: 'all' },
  { label: '审核通知', value: 'audit' },
  { label: '考勤通知', value: 'attendance' },
  { label: '任务通知', value: 'task' },
  { label: '系统公告', value: 'system' }
]

const hasMore = computed(() => rows.value.length < total.value)
const visibleRows = computed(() => {
  if (typeFilter.value === 'all') {
    return rows.value
  }
  return rows.value.filter((row) => normalizeType(row.notificationType) === typeFilter.value)
})

const normalizeType = (value = '') => {
  const type = String(value).toLowerCase()
  if (/audit|apply|review|审批|审核/.test(type)) return 'audit'
  if (/attendance|考勤|签到/.test(type)) return 'attendance'
  if (/task|任务/.test(type)) return 'task'
  if (/notice|system|公告|系统/.test(type)) return 'system'
  return 'system'
}

const typeLabel = (value) => ({
  audit: '审核通知',
  attendance: '考勤通知',
  task: '任务通知',
  system: '系统公告'
}[normalizeType(value)] || '系统公告')

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
    window.dispatchEvent(new CustomEvent('lablink:notifications-refresh'))
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

const setReadFilter = (value) => {
  readFilter.value = value
  resetAndFetch()
}

const markRead = async (row) => {
  await markNotificationRead(row.id)
  ElMessage.success('已标记为已读')
  await resetAndFetch()
}

const markAllRead = async () => {
  await markAllNotificationsRead()
  ElMessage.success('全部消息已设为已读')
  await resetAndFetch()
}

const openNotification = async (row) => {
  if (row.isRead !== 1) {
    await markNotificationRead(row.id)
  }
  await resetAndFetch()
  if (row.redirectPath) {
    window.dispatchEvent(new CustomEvent('lablink:notifications-refresh'))
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
.message-hero {
  padding: 22px;
  border-radius: 26px;
  color: #ffffff;
  background: linear-gradient(135deg, #15324b, #176b9a 52%, #19a7b8);
  box-shadow: 0 22px 48px rgba(23, 107, 154, 0.22);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: end;
}

.mobile-kicker {
  width: fit-content;
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.13);
  font-size: 12px;
  font-weight: 900;
}

.message-hero h1 {
  margin: 12px 0 8px;
  font-size: 28px;
  line-height: 1.12;
}

.message-hero p {
  margin: 0;
  color: rgba(240, 249, 255, 0.88);
  line-height: 1.68;
}

.message-hero__side {
  width: 82px;
  height: 82px;
  display: grid;
  place-items: center;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.13);
  border: 1px solid rgba(255, 255, 255, 0.16);
}

.message-hero__side strong {
  font-size: 28px;
  line-height: 1;
}

.message-hero__side span {
  font-size: 12px;
  font-weight: 900;
}

.message-toolbar {
  padding: 10px;
  border-radius: 22px;
  border: 1px solid rgba(51, 136, 187, 0.12);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 38px rgba(23, 32, 51, 0.08);
  display: grid;
  gap: 10px;
}

.message-tabs,
.message-type-row {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
}

.message-tabs::-webkit-scrollbar,
.message-type-row::-webkit-scrollbar {
  display: none;
}

.message-tabs button,
.message-type-chip {
  flex: 0 0 auto;
  min-height: 38px;
  padding: 8px 13px;
  border: 1px solid rgba(51, 136, 187, 0.13);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #64748b;
  font-weight: 900;
}

.message-tabs button.active,
.message-type-chip.active {
  color: #176b9a;
  background: #eaf6fc;
  border-color: rgba(51, 136, 187, 0.26);
}

.message-card-flow {
  display: grid;
  gap: 13px;
}

.message-card {
  padding: 16px;
  display: grid;
  gap: 13px;
  border-radius: 24px;
  border: 1px solid rgba(51, 136, 187, 0.12);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 38px rgba(23, 32, 51, 0.08);
}

.message-card.unread {
  border-color: rgba(220, 61, 67, 0.18);
}

.message-card__head,
.message-card footer,
.message-card footer div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.message-card__head span {
  color: #3388bb;
  font-size: 12px;
  font-weight: 900;
}

.message-card h2 {
  margin: 5px 0 0;
  color: #172033;
  font-size: 18px;
}

.message-card p {
  margin: 0;
  color: #475569;
  line-height: 1.72;
  white-space: pre-wrap;
}

.message-card footer > span {
  color: #94a3b8;
  font-size: 12px;
}

.load-more {
  display: flex;
  justify-content: center;
}

.muted {
  color: #94a3b8;
  font-size: 12px;
}

@media (max-width: 430px) {
  .message-hero,
  .message-card__head,
  .message-card footer {
    grid-template-columns: 1fr;
    display: grid;
  }

  .message-hero__side {
    width: 100%;
    height: auto;
    min-height: 64px;
    grid-template-columns: auto auto;
    justify-content: center;
    gap: 8px;
  }

  .message-card footer div {
    width: 100%;
  }

  .message-card footer .el-button {
    flex: 1 1 0;
  }
}
</style>
