<template>
  <AppShell
    role-label="学生端"
    :menu-groups="menuGroups"
    :user-name="userStore.realName || '同学'"
    :user-meta="joinedLabLabel"
    :user-initial="userInitial"
    @command="handleCommand"
  />
</template>

<script setup>
import { Bell, Calendar, DataBoard, Document, Files, FolderOpened, User, UserFilled } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import AppShell from '@/layouts/AppShell.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const userInitial = computed(() => userStore.realName?.charAt(0) || 'S')
const hasLabAccess = computed(() => Boolean(userStore.userInfo?.labId))

const joinedLabLabel = computed(() =>
  hasLabAccess.value ? `已加入实验室 #${userStore.userInfo.labId}` : '当前未加入实验室'
)

const menuGroups = computed(() => {
  const labItems = [
    { path: '/student/labs', label: '实验室总览', icon: FolderOpened },
    hasLabAccess.value ? { path: '/student/my-lab', label: '我的实验室', icon: UserFilled } : null,
    hasLabAccess.value ? { path: '/student/attendance', label: '考勤记录', icon: Calendar } : null,
    hasLabAccess.value ? { path: '/student/space', label: '资料空间', icon: Document } : null
  ].filter(Boolean)

  return [
    {
      label: '概览',
      items: [{ path: '/student/dashboard', label: '首页', icon: DataBoard }]
    },
    { label: '实验室', items: labItems },
    {
      label: '事务',
      items: [
        { path: '/student/applications', label: '我的申请', icon: Files },
        { path: '/student/notices', label: '公告中心', icon: Bell }
      ]
    },
    {
      label: '账号',
      items: [{ path: '/student/profile', label: '个人信息', icon: User }]
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
    await router.push('/student/profile')
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
