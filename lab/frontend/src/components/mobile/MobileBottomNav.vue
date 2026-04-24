<template>
  <nav class="mobile-bottom-nav" aria-label="移动端底部导航">
    <button
      v-for="item in normalizedItems"
      :key="item.path"
      class="mobile-bottom-nav__item"
      :class="{ active: isActive(item.path) }"
      type="button"
      @click="go(item.path)"
    >
      <span class="mobile-bottom-nav__icon">
        <el-icon :size="21"><component :is="resolveIcon(item.icon)" /></el-icon>
        <span v-if="item.badge && badgeCount > 0" class="mobile-bottom-nav__badge">
          {{ badgeValue }}
        </span>
      </span>
      <span class="mobile-bottom-nav__label">{{ item.label }}</span>
    </button>
  </nav>
</template>

<script setup>
import {
  Calendar,
  ChatDotRound,
  DataBoard,
  EditPen,
  Message,
  OfficeBuilding,
  School,
  Tickets,
  User
} from '@element-plus/icons-vue'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNotificationTodoSummary, getUnreadNotificationCount } from '@/api/notifications'

const ICON_MAP = {
  Calendar,
  ChatDotRound,
  DataBoard,
  EditPen,
  Message,
  OfficeBuilding,
  School,
  Tickets,
  User
}

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  homePath: {
    type: String,
    default: ''
  }
})

const route = useRoute()
const router = useRouter()
const unreadCount = ref(0)
const pendingCount = ref(0)

const normalizedItems = computed(() =>
  props.items
    .map((item) => ({
      ...item,
      path: item.path || item.mobilePath
    }))
    .filter((item) => item.path)
)
const badgeCount = computed(() => unreadCount.value + pendingCount.value)
const badgeValue = computed(() => (badgeCount.value > 99 ? '99+' : badgeCount.value))
const resolveIcon = (icon) => (typeof icon === 'string' ? ICON_MAP[icon] || DataBoard : icon || DataBoard)

const refreshBadge = async () => {
  if (!normalizedItems.value.some((item) => item.badge)) {
    return
  }
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

const isActive = (path) => {
  if (!path) return false
  if (path === props.homePath) {
    return route.path === path
  }
  return route.path === path || route.path.startsWith(`${path}/`)
}

const go = async (path) => {
  if (!path || route.path === path) return
  await router.push(path)
}

watch(
  () => route.fullPath,
  () => {
    refreshBadge()
  }
)

onMounted(() => {
  refreshBadge()
  window.addEventListener('lablink:notifications-refresh', refreshBadge)
})

onBeforeUnmount(() => {
  window.removeEventListener('lablink:notifications-refresh', refreshBadge)
})
</script>

<style scoped>
.mobile-bottom-nav {
  position: fixed;
  left: 50%;
  bottom: calc(10px + env(safe-area-inset-bottom));
  z-index: 80;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 4px;
  width: min(720px, calc(100vw - 20px));
  transform: translateX(-50%);
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 22px 46px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(20px);
}

.mobile-bottom-nav__item {
  position: relative;
  min-width: 0;
  min-height: 52px;
  padding: 7px 4px;
  border: 0;
  border-radius: 17px;
  background: transparent;
  color: #748195;
  display: grid;
  place-items: center;
  gap: 3px;
  cursor: pointer;
}

.mobile-bottom-nav__item.active {
  color: var(--mobile-primary, #3388bb);
  background:
    linear-gradient(180deg, rgba(234, 246, 252, 0.96), rgba(255, 255, 255, 0.82));
  box-shadow: inset 0 0 0 1px rgba(51, 136, 187, 0.16);
}

.mobile-bottom-nav__icon {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.mobile-bottom-nav__badge {
  position: absolute;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  top: -8px;
  right: -12px;
  border-radius: 999px;
  background: #ef4444;
  color: #ffffff;
  font-size: 10px;
  font-weight: 800;
  line-height: 16px;
  box-shadow: 0 0 0 2px #ffffff;
}

.mobile-bottom-nav__label {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  line-height: 1;
  font-weight: 800;
}
</style>
