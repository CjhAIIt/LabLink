<template>
  <div class="shell student-shell">
    <aside v-if="!isFullScreenRoute" class="sidebar" :class="{ 'is-collapsed': isCollapse }">
      <div class="sidebar-inner">
        <div class="sidebar-brand">
          <BrandLogo title="LabLink" subtitle="高校实验室管理平台" tone="dark" size="sm" />
        </div>

        <el-menu :default-active="$route.path" router class="sidebar-menu" :collapse="false">
          <template v-for="group in menuGroups" :key="group.title">
            <div class="sidebar-group-title">{{ group.title }}</div>
            <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </div>
    </aside>
    <div v-if="sidebarVisible && !isFullScreenRoute" class="sidebar-mask" @click="closeSidebar"></div>

    <div class="main-shell">
      <header v-if="!isFullScreenRoute" class="topbar">
        <div class="topbar-left">
          <el-button class="collapse-btn" text @click="toggleCollapse">
            <el-icon :size="20">
              <component :is="isCollapse ? Expand : Fold" />
            </el-icon>
          </el-button>
          <div class="topbar-title">
            <p class="topbar-label">学生端</p>
            <h2>{{ $route.meta.title || '工作台' }}</h2>
          </div>
        </div>
        <div class="topbar-actions">
          <TopbarNotification path="/student/notifications" />
          <el-dropdown @command="handleCommand">
            <div class="user-chip">
              <el-avatar :size="34">{{ userInitial }}</el-avatar>
              <div>
                <strong>{{ userStore.realName || '学生' }}</strong>
                <span>{{ joinedLabLabel }}</span>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content" :class="{ 'content--fullscreen': isFullScreenRoute }">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { Expand, Fold } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BrandLogo from '@/components/BrandLogo.vue'
import { useUserStore } from '@/stores/user'
import { ensureAuthContext } from '@/utils/auth-context'
import TopbarNotification from '@/components/common/TopbarNotification.vue'
import { resolveDesktopMenuGroups, resolveDesktopMenuItems } from '@/utils/portal-menu'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isFullScreenRoute = computed(() => Boolean(route.meta?.fullScreen))

const isMobile = ref(typeof window !== 'undefined' ? window.innerWidth <= 960 : false)
const isCollapse = ref(isMobile.value)
const sidebarVisible = computed(() => isMobile.value && !isCollapse.value)

const updateViewport = () => {
  const mobile = window.innerWidth <= 960
  if (mobile !== isMobile.value) {
    isMobile.value = mobile
    isCollapse.value = mobile
    return
  }
  isMobile.value = mobile
}

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const closeSidebar = () => {
  if (isMobile.value) {
    isCollapse.value = true
  }
}

const userInitial = computed(() => userStore.realName?.charAt(0) || 'S')
const hasLabAccess = computed(() => Boolean(userStore.userInfo?.labId && userStore.userInfo?.labName))
const canApplyLab = computed(() => userStore.hasPermission('lab:apply:self'))

const joinedLabLabel = computed(() => userStore.userInfo?.labName || '暂未加入实验室')

const fallbackMenuItems = computed(() =>
  [
    { path: '/student/dashboard', label: '工作台', icon: 'DataBoard' },
    { path: '/student/labs', label: '实验室广场', icon: 'FolderOpened' },
    canApplyLab.value ? { path: '/student/applications', label: '我的申请', icon: 'Tickets' } : null,
    hasLabAccess.value ? { path: '/student/my-lab', label: '我的实验室', icon: 'UserFilled' } : null,
    hasLabAccess.value ? { path: '/student/attendance', label: '我的考勤', icon: 'Calendar' } : null,
    hasLabAccess.value ? { path: '/student/space', label: '资料空间', icon: 'Files' } : null,
    { path: '/student/notices', label: '公告通知', icon: 'Bell' },
    { path: '/student/profile', label: '个人资料', icon: 'User' }
  ].filter(Boolean)
)

const menuItems = computed(() => resolveDesktopMenuItems(userStore.menus, fallbackMenuItems.value))
const menuGroups = computed(() => resolveDesktopMenuGroups(menuItems.value, 'student'))

const ensureContext = async () => {
  await ensureAuthContext(userStore, { force: true })
}

const handleCommand = async (command) => {
  if (command === 'profile') {
    await router.push('/student/profile')
    return
  }
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', { type: 'warning' })
    userStore.clearUserInfo()
    await router.push('/login')
  }
}

watch(
  () => route.fullPath,
  () => {
    closeSidebar()
  }
)

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
  ensureContext()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewport)
})
</script>

<style scoped>
.shell {
  min-height: 100vh;
  display: flex;
  background-color: #ffffff;
  overflow-x: hidden;
  overflow-y: visible;
  position: relative;
  align-items: flex-start;
}

.sidebar {
  width: 260px;
  background: #f9f9f9;
  color: #0f172a;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1), transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.3s ease;
  overflow: hidden;
  flex-shrink: 0;
  border-right: 1px solid #e5e5e5;
  align-self: flex-start;
}

.sidebar.is-collapsed {
  margin-left: -260px;
}

.sidebar-inner {
  width: 260px;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
}

.sidebar-brand {
  margin-bottom: 24px;
  padding: 0 8px;
}

.sidebar-menu {
  border: 0;
  background: transparent;
  flex: 1;
  overflow-y: auto;
}

.sidebar-menu::-webkit-scrollbar {
  width: 4px;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 44px;
  border-radius: 8px;
  color: #333333;
  margin-bottom: 4px;
  font-size: 14px;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: #ececec;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  color: #000000;
  background: #ececec;
  font-weight: 600;
}

.sidebar-group-title {
  margin: 16px 12px 6px;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.sidebar-group-title:first-child {
  margin-top: 0;
}

.main-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 100vh;
  height: auto;
}

.sidebar-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(2px);
  z-index: 90;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #ffffff;
  gap: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.topbar-title {
  min-width: 0;
}

.collapse-btn {
  color: #475569;
  padding: 8px;
}

.collapse-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 8px;
}

.topbar-label {
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.topbar h2 {
  margin-top: 2px;
  margin-bottom: 0;
  color: #0f172a;
  font-size: 20px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s;
}

.user-chip:hover {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}

.user-chip span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.user-chip strong {
  font-size: 14px;
  color: #1e293b;
}

.content {
  flex: 1;
  min-height: 0;
  overflow: visible;
  padding: 0 24px 24px;
  background-color: #ffffff;
}

@media (min-width: 961px) {
  .sidebar {
    position: sticky;
    top: 0;
    height: 100vh;
  }
}

.content.content--fullscreen {
  padding: 0;
  overflow: hidden;
  background: #dfe6f1;
}

@media (max-width: 960px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 120;
    height: 100dvh;
    border-right: 1px solid #e5e5e5;
    box-shadow: 0 18px 42px rgba(15, 23, 42, 0.18);
  }

  .sidebar.is-collapsed {
    transform: translateX(-100%);
    opacity: 0;
    margin-left: 0;
    pointer-events: none;
  }

  .content {
    padding: 0 16px 24px;
  }

  .content.content--fullscreen {
    padding: 0;
  }

  .topbar {
    padding: 16px;
  }
}

@media (max-width: 768px) {
  .main-shell {
    height: 100dvh;
  }

  .topbar {
    padding: 12px 14px;
    gap: 12px;
  }

  .topbar-left {
    gap: 12px;
  }

  .topbar-actions {
    gap: 8px;
  }

  .topbar-label {
    font-size: 11px;
    letter-spacing: 0.12em;
  }

  .topbar h2 {
    font-size: 16px;
  }

  .user-chip {
    max-width: 132px;
    gap: 8px;
    padding: 6px 8px;
  }

  .user-chip div {
    min-width: 0;
  }

  .user-chip span {
    display: none;
  }

  .user-chip strong {
    display: block;
    font-size: 13px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .content {
    padding: 0 12px 16px;
  }

  .content.content--fullscreen {
    padding: 0;
  }
}
</style>
