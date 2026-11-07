<template>
  <AppShell
    role-label="教师端"
    :menu-groups="menuGroups"
    :user-name="userStore.realName || '老师'"
    :user-meta="workspaceLabel"
    :user-initial="userInitial"
    @command="handleCommand"
  />
</template>

<script setup>
import { Bell, DataBoard, DocumentCopy, Files, User } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import AppShell from '@/layouts/AppShell.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const userInitial = computed(() => userStore.realName?.charAt(0) || 'T')
const hasWorkspace = computed(() => Boolean(userStore.userInfo?.labId))
const workspaceLabel = computed(() =>
  hasWorkspace.value ? `指导实验室 #${userStore.userInfo.labId}` : '指导老师'
)

const menuGroups = computed(() => {
  const labItems = [
    { path: '/teacher/create-applies', label: '实验室创建申请', icon: DocumentCopy },
    hasWorkspace.value ? { path: '/teacher/workspace', label: '实验室工作台', icon: Files } : null
  ].filter(Boolean)

  return [
    {
      label: '概览',
      items: [{ path: '/teacher/dashboard', label: '工作台', icon: DataBoard }]
    },
    { label: '实验室', items: labItems },
    {
      label: '事务',
      items: [{ path: '/teacher/notices', label: '公告中心', icon: Bell }]
    },
    {
      label: '账号',
      items: [{ path: '/teacher/profile', label: '个人信息', icon: User }]
    }
  ].filter((group) => group.items.length)
})

const ensureProfile = async () => {
  if (userStore.userInfo?.id) return
  const response = await request.get('/api/access/profile')
  userStore.setUserInfo(response.data || {})
}

const handleCommand = async (command) => {
  if (command === 'profile') {
    await router.push('/teacher/profile')
    return
  }
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', { type: 'warning' })
    userStore.clearUserInfo()
    await router.push('/login')
  }
}

onMounted(() => {
  ensureProfile()
})
</script>
