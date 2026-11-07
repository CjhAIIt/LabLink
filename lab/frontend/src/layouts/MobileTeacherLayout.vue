<template>
  <MobileShell
    :tab-items="tabItems"
    :user-initial="userInitial"
    root-path="/m/teacher/dashboard"
    @command="handleCommand"
  />
</template>

<script setup>
import { Bell, HomeFilled, User } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import MobileShell from '@/layouts/MobileShell.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const userInitial = computed(() => userStore.realName?.charAt(0) || userStore.userName?.charAt(0) || 'T')

const tabItems = [
  { path: '/m/teacher/dashboard', label: '首页', icon: HomeFilled },
  { path: '/m/teacher/notices', label: '公告', icon: Bell },
  { path: '/m/teacher/profile', label: '我的', icon: User }
]

const handleCommand = async (command) => {
  if (command === 'profile') {
    await router.push('/m/teacher/profile')
    return
  }
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', { type: 'warning' })
    userStore.clearUserInfo()
    await router.push('/login')
  }
}
</script>
