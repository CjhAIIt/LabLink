<template>
  <MobileShell
    :tab-items="tabItems"
    :user-initial="userInitial"
    root-path="/m/admin/dashboard"
    @command="handleCommand"
  />
</template>

<script setup>
import { Bell, DocumentChecked, HomeFilled, OfficeBuilding, User } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import MobileShell from '@/layouts/MobileShell.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const userInitial = computed(() => userStore.realName?.charAt(0) || 'A')

const tabItems = [
  { path: '/m/admin/dashboard', label: '首页', icon: HomeFilled },
  { path: '/m/admin/approvals', label: '审批', icon: DocumentChecked },
  { path: '/m/admin/labs', label: '实验室', icon: OfficeBuilding },
  { path: '/m/admin/notices', label: '公告', icon: Bell },
  { path: '/m/admin/profile', label: '我的', icon: User }
]

const handleCommand = async (command) => {
  if (command === 'profile') {
    await router.push('/m/admin/profile')
    return
  }
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', { type: 'warning' })
    userStore.clearUserInfo()
    await router.push('/login')
  }
}
</script>
