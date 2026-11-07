<template>
  <MobileShell
    :tab-items="tabItems"
    :user-initial="userInitial"
    root-path="/m/student/dashboard"
    @command="handleCommand"
  />
</template>

<script setup>
import { Bell, Document, HomeFilled, OfficeBuilding, User } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import MobileShell from '@/layouts/MobileShell.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const userInitial = computed(() => userStore.realName?.charAt(0) || userStore.userName?.charAt(0) || 'S')

const tabItems = [
  { path: '/m/student/dashboard', label: '首页', icon: HomeFilled },
  { path: '/m/student/labs', label: '实验室', icon: OfficeBuilding },
  { path: '/m/student/applications', label: '申请', icon: Document },
  { path: '/m/student/notices', label: '公告', icon: Bell },
  { path: '/m/student/profile', label: '我的', icon: User }
]

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
