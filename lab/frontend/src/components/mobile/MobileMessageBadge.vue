<template>
  <button class="mobile-message-badge" type="button" :aria-label="ariaLabel" @click="go">
    <el-badge :value="badgeValue" :hidden="badgeCount <= 0" :max="99">
      <el-icon :size="iconSize"><Bell /></el-icon>
    </el-badge>
  </button>
</template>

<script setup>
import { Bell } from '@element-plus/icons-vue'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNotificationTodoSummary, getUnreadNotificationCount } from '@/api/notifications'

const props = defineProps({
  path: {
    type: String,
    required: true
  },
  iconSize: {
    type: Number,
    default: 20
  }
})

const route = useRoute()
const router = useRouter()
const unreadCount = ref(0)
const pendingCount = ref(0)

const badgeCount = computed(() => unreadCount.value + pendingCount.value)
const badgeValue = computed(() => (badgeCount.value > 99 ? '99+' : badgeCount.value))
const ariaLabel = computed(() => (badgeCount.value > 0 ? `消息中心，${badgeValue.value} 条待处理` : '消息中心'))

const refresh = async () => {
  try {
    const [countResponse, todoResponse] = await Promise.all([
      getUnreadNotificationCount(),
      getNotificationTodoSummary()
    ])
    unreadCount.value = Number(countResponse.data?.unreadCount || 0)
    pendingCount.value = Number(todoResponse.data?.pendingCount || 0)
  } catch {
    unreadCount.value = 0
    pendingCount.value = 0
  }
}

const go = async () => {
  await router.push(props.path)
}

watch(
  () => route.fullPath,
  () => {
    refresh()
  }
)

onMounted(() => {
  refresh()
  window.addEventListener('lablink:notifications-refresh', refresh)
})

onBeforeUnmount(() => {
  window.removeEventListener('lablink:notifications-refresh', refresh)
})
</script>

<style scoped>
.mobile-message-badge {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--mobile-primary, #3388bb);
  box-shadow: 0 12px 28px rgba(23, 107, 154, 0.1);
  cursor: pointer;
}

.mobile-message-badge :deep(.el-badge__content) {
  border: 2px solid #ffffff;
  font-weight: 800;
}
</style>
