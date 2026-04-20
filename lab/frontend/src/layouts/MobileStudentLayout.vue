<template>
  <div class="m-shell student-shell">
    <header class="m-topbar">
      <button v-if="canGoBack" class="m-nav-btn" type="button" @click="router.back()">
        <el-icon :size="20"><ArrowLeft /></el-icon>
      </button>
      <div class="m-title-block">
        <span class="m-title">{{ title }}</span>
        <div class="m-subline">
          <span class="m-subtitle">学生移动工作台</span>
          <span class="m-context-chip" :class="{ muted: !hasLabAccess }">{{ scopeLabel }}</span>
        </div>
      </div>
      <button class="m-nav-btn" type="button" @click="router.push('/m/student/notifications')">
        <el-icon :size="20"><Bell /></el-icon>
      </button>
      <el-dropdown placement="bottom-end" @command="handleCommand">
        <button class="m-user-btn" type="button">
          <el-avatar :size="34">{{ userInitial }}</el-avatar>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人资料</el-dropdown-item>
            <el-dropdown-item command="desktop">切换桌面端</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>

    <main class="m-content">
      <router-view />
    </main>

    <nav class="m-tabbar" aria-label="Student navigation">
      <button
        v-for="item in tabItems"
        :key="item.path"
        class="m-tab"
        :class="{ active: isActive(item.path) }"
        type="button"
        @click="router.push(item.path)"
      >
        <el-icon :size="20"><component :is="item.icon" /></el-icon>
        <span class="m-tab-label">{{ item.label }}</span>
      </button>
    </nav>
  </div>
</template>

<script setup>
import { ArrowLeft, Bell } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ensureAuthContext } from '@/utils/auth-context'
import { resolvePortalLogin, setPortalSurface } from '@/utils/portal'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const title = computed(() => route.meta.title || '学生工作台')
const userInitial = computed(() => userStore.realName?.charAt(0) || userStore.userName?.charAt(0) || 'S')
const canGoBack = computed(() => route.path !== '/m/student/dashboard')
const hasLabAccess = computed(() => Boolean(userStore.userInfo?.labId))
const canApplyLab = computed(() => userStore.hasPermission('lab:apply:self'))
const scopeLabel = computed(() => userStore.userInfo?.labName || (hasLabAccess.value ? `实验室 #${userStore.userInfo.labId}` : '待加入实验室'))

const tabItems = computed(() =>
  [
    { path: '/m/student/dashboard', label: '首页', icon: 'DataBoard' },
    { path: '/m/student/labs', label: '实验室', icon: 'OfficeBuilding' },
    hasLabAccess.value
      ? { path: '/m/student/attendance', label: '考勤', icon: 'Calendar' }
      : (canApplyLab.value ? { path: '/m/student/applications', label: '申请', icon: 'Tickets' } : null),
    { path: '/m/student/profile', label: '我的', icon: 'User' }
  ].filter(Boolean)
)

const isActive = (path) => route.path === path || (path !== '/m/student/dashboard' && route.path.startsWith(path))

const handleCommand = async (command) => {
  if (command === 'profile') {
    await router.push('/m/student/profile')
    return
  }
  if (command === 'desktop') {
    setPortalSurface('desktop')
    await router.push('/login')
    return
  }
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', { type: 'warning' })
    setPortalSurface('mobile')
    userStore.clearUserInfo()
    await router.push(resolvePortalLogin({ surface: 'mobile' }))
  }
}

onMounted(() => {
  setPortalSurface('mobile')
  ensureAuthContext(userStore)
})
</script>

<style scoped>
.m-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  background:
    radial-gradient(circle at top left, var(--shell-glow), transparent 32%),
    radial-gradient(circle at top right, var(--shell-glow-soft), transparent 26%),
    linear-gradient(180deg, var(--shell-bg-top) 0%, var(--shell-bg-bottom) 44%, #f8fafc 100%);
}

.student-shell {
  --accent: #2563eb;
  --accent-soft: rgba(37, 99, 235, 0.12);
  --accent-strong: rgba(37, 99, 235, 0.22);
  --accent-highlight: #60a5fa;
  --shell-bg-top: #ebf3ff;
  --shell-bg-bottom: #f5f8ff;
  --shell-glow: rgba(59, 130, 246, 0.18);
  --shell-glow-soft: rgba(96, 165, 250, 0.14);
}

.m-shell::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.42), transparent 28%);
  opacity: 0.9;
}

.m-shell::after {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.06) 1px, transparent 1px);
  background-size: 24px 24px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.4), transparent 70%);
  opacity: 0.35;
}

.m-topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 10px;
  padding: calc(14px + env(safe-area-inset-top)) 14px 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(255, 255, 255, 0.86));
  border-bottom: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(18px);
}

.m-topbar::after {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(37, 99, 235, 0.18), transparent);
}

.m-nav-btn,
.m-user-btn {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  color: #0f172a;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.08);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.m-user-btn {
  border-radius: 999px;
  padding: 0;
}

.m-nav-btn:active,
.m-user-btn:active {
  transform: translateY(1px);
}

.m-user-btn :deep(.el-avatar) {
  background: linear-gradient(135deg, var(--accent), var(--accent-highlight));
  color: #ffffff;
  box-shadow: 0 8px 18px var(--accent-strong);
}

.m-title-block {
  position: relative;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-left: 16px;
}

.m-title-block::before {
  content: '';
  position: absolute;
  left: 0;
  top: 7px;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--accent);
  box-shadow: 0 0 0 6px var(--accent-soft);
}

.m-title {
  color: #0f172a;
  font-size: 17px;
  font-weight: 800;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.m-subtitle {
  color: #64748b;
  font-size: 11px;
  letter-spacing: 0.04em;
}

.m-subline {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.m-context-chip {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
}

.m-context-chip.muted {
  background: rgba(148, 163, 184, 0.14);
  color: #64748b;
}

.m-content {
  flex: 1;
  width: min(720px, 100%);
  margin: 0 auto;
  padding: 20px 14px calc(118px + env(safe-area-inset-bottom));
}

.m-tabbar {
  position: fixed;
  left: 12px;
  right: 12px;
  bottom: calc(12px + env(safe-area-inset-bottom));
  z-index: 50;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.82);
  box-shadow: 0 20px 42px rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(18px);
}

.m-tabbar::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.42), transparent 48%);
  pointer-events: none;
}

.m-tab {
  border: 0;
  background: transparent;
  padding: 10px 6px;
  border-radius: 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #64748b;
  transition: transform 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.m-tab.active {
  color: var(--accent);
  background: linear-gradient(180deg, var(--accent-soft), rgba(255, 255, 255, 0.78));
  box-shadow: inset 0 0 0 1px var(--accent-strong);
  transform: translateY(-1px);
}

.m-tab.active :deep(.el-icon) {
  transform: translateY(-1px);
}

.m-tab-label {
  font-size: 11px;
  line-height: 1;
  font-weight: 700;
}

@media (max-width: 420px) {
  .m-topbar {
    gap: 8px;
    padding-left: 12px;
    padding-right: 12px;
  }

  .m-content {
    padding-left: 12px;
    padding-right: 12px;
  }

  .m-tabbar {
    left: 10px;
    right: 10px;
    bottom: calc(10px + env(safe-area-inset-bottom));
    border-radius: 24px;
    gap: 6px;
  }
}
</style>
