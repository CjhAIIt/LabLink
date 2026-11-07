<template>
  <div class="m-shell">
    <header class="m-topbar">
      <button v-if="canGoBack" class="m-icon-btn" type="button" @click="router.back()">
        <el-icon :size="20"><ArrowLeft /></el-icon>
      </button>
      <div class="m-title">
        <span class="m-title-text">{{ title }}</span>
      </div>
      <el-dropdown placement="bottom-end" @command="handleCommand">
        <button class="m-user-btn" type="button">
          <el-avatar :size="30">{{ userInitial }}</el-avatar>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人信息</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>

    <main class="m-content">
      <router-view />
    </main>

    <nav class="m-tabbar" aria-label="学生端导航">
      <button
        v-for="item in tabItems"
        :key="item.path"
        class="m-tab"
        :class="{ active: isActive(item.path) }"
        type="button"
        @click="router.push(item.path)"
      >
        <el-icon :size="22"><component :is="item.icon" /></el-icon>
        <span class="m-tab-label">{{ item.label }}</span>
      </button>
    </nav>
  </div>
</template>

<script setup>
import { ArrowLeft, Bell, Document, HomeFilled, OfficeBuilding, User } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const title = computed(() => route.meta.title || '学生端')
const userInitial = computed(() => userStore.realName?.charAt(0) || userStore.userName?.charAt(0) || 'S')
const canGoBack = computed(() => route.path !== '/m/student/dashboard')

const tabItems = [
  { path: '/m/student/dashboard', label: '首页', icon: HomeFilled },
  { path: '/m/student/labs', label: '实验室', icon: OfficeBuilding },
  { path: '/m/student/applications', label: '申请', icon: Document },
  { path: '/m/student/notices', label: '公告', icon: Bell },
  { path: '/m/student/profile', label: '我的', icon: User }
]

const isActive = (path) => route.path === path || (path !== '/m/student/dashboard' && route.path.startsWith(path))

const handleCommand = async (command) => {
  if (command === 'profile') {
    await router.push('/m/student/profile')
    return
  }
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', { type: 'warning' })
    userStore.clearUserInfo()
    await router.push('/login')
  }
}
</script>

<style scoped>
.m-shell {
  min-height: 100vh;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
}

.m-topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: calc(12px + env(safe-area-inset-top)) 14px 12px 14px;
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  backdrop-filter: blur(12px);
}

.m-title {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
}

.m-title-text {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.m-icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.m-user-btn {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  border: 0;
  background: transparent;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.m-content {
  flex: 1;
  padding: 14px 14px calc(84px + env(safe-area-inset-bottom)) 14px;
}

.m-tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 30;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 4px;
  padding: 10px 10px calc(10px + env(safe-area-inset-bottom)) 10px;
  background: rgba(255, 255, 255, 0.92);
  border-top: 1px solid rgba(226, 232, 240, 0.9);
  backdrop-filter: blur(14px);
}

.m-tab {
  border: 0;
  background: transparent;
  padding: 8px 6px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #64748b;
}

.m-tab.active {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.1);
}

.m-tab-label {
  font-size: 11px;
  line-height: 1;
  font-weight: 600;
}
</style>
