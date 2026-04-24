<template>
  <el-popover
    placement="bottom-end"
    trigger="click"
    width="340"
    popper-class="topbar-notification-popover"
    @show="fetchNotificationState"
  >
    <template #reference>
      <button class="topbar-notification" :class="{ 'is-mobile': mobile }" type="button" :aria-label="ariaLabel">
        <el-badge :value="badgeValue" :hidden="badgeCount <= 0" :max="99" class="notification-badge">
          <el-icon :size="20"><Bell /></el-icon>
        </el-badge>
      </button>
    </template>

    <div class="notification-panel">
      <div class="panel-header">
        <div>
          <strong>消息与待办</strong>
          <p>未读 {{ unreadCount }} 条，待办 {{ pendingCount }} 项</p>
        </div>
        <el-button text type="primary" @click="goNotifications">消息中心</el-button>
      </div>

      <div class="panel-section">
        <div class="section-title">最近消息</div>
        <button v-for="item in recentMessages" :key="item.id" class="panel-row" type="button" @click="openMessage(item)">
          <span class="row-dot" :class="{ muted: item.isRead === 1 }" />
          <span class="row-main">
            <strong>{{ item.title || '系统消息' }}</strong>
            <small>{{ item.content || '暂无内容' }}</small>
          </span>
        </button>
        <el-empty v-if="recentMessages.length === 0" description="暂无消息" :image-size="56" />
      </div>

      <div v-if="todoItems.length" class="panel-section">
        <div class="section-title">待办中心</div>
        <button v-for="item in visibleTodoItems" :key="item.key" class="panel-row todo-row" type="button" @click="goRoute(item.route)">
          <span class="todo-count">{{ item.value }}</span>
          <span class="row-main">
            <strong>{{ item.label }}</strong>
            <small>点击进入处理</small>
          </span>
        </button>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import {
  getMyNotifications,
  getNotificationTodoSummary,
  getUnreadNotificationCount,
  markNotificationRead
} from '@/api/notifications'

const props = defineProps({
  path: {
    type: String,
    required: true
  },
  mobile: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()
const router = useRouter()
const unreadCount = ref(0)
const pendingCount = ref(0)
const recentMessages = ref([])
const todoItems = ref([])

const badgeCount = computed(() => unreadCount.value + pendingCount.value)
const badgeValue = computed(() => (badgeCount.value > 99 ? '99+' : badgeCount.value))
const visibleTodoItems = computed(() => todoItems.value.filter((item) => Number(item.value || 0) > 0).slice(0, 4))
const ariaLabel = computed(() => (badgeCount.value > 0 ? `消息中心，${badgeValue.value} 条待处理` : '消息中心'))

const fetchNotificationState = async () => {
  try {
    const [countResponse, listResponse, todoResponse] = await Promise.all([
      getUnreadNotificationCount(),
      getMyNotifications({ pageNum: 1, pageSize: 5 }),
      getNotificationTodoSummary()
    ])
    unreadCount.value = Number(countResponse.data?.unreadCount || 0)
    recentMessages.value = listResponse.data?.records || []
    pendingCount.value = Number(todoResponse.data?.pendingCount || 0)
    todoItems.value = todoResponse.data?.items || []
  } catch {
    unreadCount.value = 0
    pendingCount.value = 0
    recentMessages.value = []
    todoItems.value = []
  }
}

const goNotifications = async () => {
  await router.push(props.path)
}

const goRoute = async (path) => {
  if (!path) return
  await router.push(path)
}

const openMessage = async (item) => {
  if (!item) return
  if (item.isRead !== 1) {
    await markNotificationRead(item.id)
    await fetchNotificationState()
    window.dispatchEvent(new CustomEvent('lablink:notifications-refresh'))
  }
  if (item.redirectPath) {
    await router.push(item.redirectPath)
  }
}

watch(
  () => route.fullPath,
  () => {
    fetchNotificationState()
  }
)

onMounted(() => {
  fetchNotificationState()
  window.addEventListener('lablink:notifications-refresh', fetchNotificationState)
})

onBeforeUnmount(() => {
  window.removeEventListener('lablink:notifications-refresh', fetchNotificationState)
})
</script>

<style scoped>
.topbar-notification {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #334155;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.05);
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease, border-color 0.2s ease;
}

.topbar-notification:hover {
  border-color: #cbd5e1;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.topbar-notification.is-mobile {
  width: 40px;
  height: 40px;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.08);
}

.topbar-notification.is-mobile:hover {
  transform: none;
}

.notification-badge :deep(.el-badge__content) {
  border: 2px solid #ffffff;
  font-weight: 700;
}

:global(.topbar-notification-popover) {
  padding: 0;
  border-radius: 18px;
  overflow: hidden;
}

.notification-panel {
  padding: 14px;
  display: grid;
  gap: 14px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.panel-header strong {
  color: #0f172a;
}

.panel-header p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.panel-section {
  display: grid;
  gap: 8px;
}

.section-title {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 700;
}

.panel-row {
  width: 100%;
  display: flex;
  gap: 10px;
  align-items: center;
  border: 0;
  border-radius: 12px;
  padding: 10px;
  background: #f8fafc;
  text-align: left;
  cursor: pointer;
}

.panel-row:hover {
  background: #eef6ff;
}

.row-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #ef4444;
  flex: 0 0 auto;
}

.row-dot.muted {
  background: #cbd5e1;
}

.row-main {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.row-main strong,
.row-main small {
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
}

.row-main strong {
  color: #1e293b;
  font-size: 13px;
  -webkit-line-clamp: 2;
}

.row-main small {
  color: #64748b;
  font-size: 12px;
  -webkit-line-clamp: 3;
}

.todo-count {
  min-width: 28px;
  height: 28px;
  padding: 0 7px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fee2e2;
  color: #dc2626;
  font-weight: 800;
}
</style>
