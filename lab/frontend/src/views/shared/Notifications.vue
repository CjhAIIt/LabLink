<template>
  <div class="notification-page">
    <section class="page-hero notification-hero">
      <div class="hero-header">
        <div>
          <div class="eyebrow">消息中心</div>
          <h2>站内消息</h2>
          <p class="hero-text">统一查看审核结果、借用审批和其他系统通知。</p>
        </div>
        <div class="hero-actions">
          <el-tag type="danger" effect="plain">未读 {{ unreadCount }}</el-tag>
          <el-button @click="refresh">刷新</el-button>
          <el-button type="primary" plain :disabled="!unreadCount" @click="markAllRead">全部已读</el-button>
        </div>
      </div>
    </section>

    <TablePageCard title="消息列表" subtitle="站内消息" :count-label="`${pagination.total} 条`">
      <template #filters>
        <SearchToolbar
          :show-keyword="false"
          collapsible-on-mobile
          collapse-title="消息筛选"
          :collapse-summary="filterSummary"
          @search="handleSearch"
          @reset="resetFilters"
        >
          <el-form-item label="阅读状态">
            <el-select v-model="filters.isRead" clearable placeholder="全部" style="width: 180px">
              <el-option label="未读" :value="0" />
              <el-option label="已读" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="消息类型">
            <el-input v-model="filters.notificationType" clearable placeholder="可选的消息类型筛选" />
          </el-form-item>
        </SearchToolbar>
      </template>
      <div class="desktop-mobile-card-list mobile-only">
        <article v-for="row in rows" :key="row.id" class="desktop-mobile-card">
          <div class="desktop-mobile-card__header">
            <div class="desktop-mobile-card__title-wrap">
              <strong class="desktop-mobile-card__title">{{ row.title || '系统消息' }}</strong>
              <span class="desktop-mobile-card__subtitle">{{ row.notificationType || '通知' }}</span>
            </div>
            <StatusTag
              :value="row.isRead"
              :label-map="{ 0: '未读', 1: '已读' }"
              :type-map="{ 0: 'danger', 1: 'info' }"
            />
          </div>
          <div class="desktop-mobile-card__summary">{{ row.content || '暂无内容' }}</div>
          <div class="desktop-mobile-card__grid">
            <div class="desktop-mobile-card__line">
              <span class="desktop-mobile-card__line-label">创建时间</span>
              <span class="desktop-mobile-card__line-value">{{ formatDateTime(row.createTime) }}</span>
            </div>
          </div>
          <div class="desktop-mobile-card__actions">
            <el-button v-if="row.isRead !== 1" size="large" @click="markRead(row)">标记已读</el-button>
            <el-button v-if="row.redirectPath" size="large" type="primary" plain @click="openNotification(row)">打开</el-button>
          </div>
        </article>
      </div>

      <el-table v-loading="loading" :data="rows" stripe class="desktop-only">
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag
              :value="row.isRead"
              :label-map="{ 0: '未读', 1: '已读' }"
              :type-map="{ 0: 'danger', 1: 'info' }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
        <el-table-column prop="notificationType" label="类型" min-width="150" />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.isRead !== 1" size="small" @click="markRead(row)">标记已读</el-button>
            <el-button v-if="row.redirectPath" size="small" type="primary" plain @click="openNotification(row)">打开</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #pagination>
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          :layout="paginationLayout"
          @size-change="fetchRows"
          @current-change="fetchRows"
        />
      </template>
    </TablePageCard>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SearchToolbar from '@/components/common/SearchToolbar.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import {
  getMyNotifications,
  getUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead
} from '@/api/notifications'
import { useViewport } from '@/composables/useViewport'

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const unreadCount = ref(0)
const { isPhoneWide } = useViewport()

const filters = reactive({
  isRead: '',
  notificationType: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const filterSummary = computed(() => {
  const parts = []
  if (filters.isRead !== '') {
    parts.push(filters.isRead === 1 ? '已读' : '未读')
  }
  if (filters.notificationType.trim()) {
    parts.push(filters.notificationType.trim())
  }
  return parts.length ? parts.join(' / ') : '全部消息'
})
const paginationLayout = computed(() => (isPhoneWide.value ? 'prev, pager, next' : 'total, sizes, prev, pager, next, jumper'))

const fetchUnreadCount = async () => {
  const response = await getUnreadNotificationCount()
  unreadCount.value = response.data.unreadCount || 0
}

const fetchRows = async () => {
  loading.value = true
  try {
    const response = await getMyNotifications({
      pageNum: pagination.current,
      pageSize: pagination.size,
      isRead: filters.isRead === '' ? undefined : filters.isRead,
      notificationType: filters.notificationType.trim() || undefined
    })
    rows.value = response.data.records || []
    pagination.total = response.data.total || 0
  } finally {
    loading.value = false
  }
}

const refresh = async () => {
  await Promise.all([fetchUnreadCount(), fetchRows()])
  window.dispatchEvent(new CustomEvent('lablink:notifications-refresh'))
}

const handleSearch = () => {
  pagination.current = 1
  fetchRows()
}

const resetFilters = () => {
  filters.isRead = ''
  filters.notificationType = ''
  pagination.current = 1
  fetchRows()
}

const markRead = async (row) => {
  await markNotificationRead(row.id)
  ElMessage.success('消息已标记为已读')
  await refresh()
}

const markAllRead = async () => {
  await markAllNotificationsRead()
  ElMessage.success('全部消息已标记为已读')
  await refresh()
}

const openNotification = async (row) => {
  if (row.isRead !== 1) {
    await markNotificationRead(row.id)
  }
  if (row.redirectPath) {
    window.dispatchEvent(new CustomEvent('lablink:notifications-refresh'))
    await router.push(row.redirectPath)
    return
  }
  await refresh()
}

const formatDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(
    2,
    '0'
  )} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  refresh()
})
</script>

<style scoped>
.notification-page {
  display: grid;
  gap: 20px;
}

.notification-hero {
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.92), rgba(2, 132, 199, 0.88)),
    radial-gradient(circle at top right, rgba(34, 197, 94, 0.16), transparent 35%);
}

.hero-header,
.hero-actions {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.hero-header h2 {
  margin: 6px 0 10px;
  font-size: 28px;
}

.hero-text {
  margin: 0;
  color: rgba(226, 232, 240, 0.86);
}
</style>
