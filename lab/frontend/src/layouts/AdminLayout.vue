<template>
  <AppShell
    role-label="管理端"
    :menu-groups="menuGroups"
    :user-name="userStore.realName || '管理员'"
    :user-meta="roleLabel"
    :user-initial="userInitial"
    @command="handleCommand"
  />
</template>

<script setup>
import {
  Bell,
  Calendar,
  DataBoard,
  DocumentChecked,
  DocumentCopy,
  Files,
  FolderOpened,
  OfficeBuilding,
  Search,
  TrendCharts,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import AppShell from '@/layouts/AppShell.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const isSchoolDirector = computed(() => Boolean(userStore.userInfo?.schoolDirector))
const isCollegeManager = computed(() => Boolean(userStore.userInfo?.collegeManager))
const isLabManager = computed(() => Boolean(userStore.userInfo?.labManager))

const roleLabel = computed(() => {
  if (isSchoolDirector.value) return '学校管理员'
  if (isCollegeManager.value) return '学院管理员'
  if (isLabManager.value) return '实验室管理员'
  return userStore.userRole === 'super_admin' ? '学校管理员' : '管理员'
})

const userInitial = computed(() => userStore.realName?.charAt(0) || 'A')

const menuGroups = computed(() => {
  const governanceItems = [
    isSchoolDirector.value ? { path: '/admin/search', label: '综合检索', icon: Search } : null,
    isSchoolDirector.value ? { path: '/admin/colleges', label: '学院管理', icon: OfficeBuilding } : null,
    isSchoolDirector.value ? { path: '/admin/labs', label: '实验室管理', icon: FolderOpened } : null
  ].filter(Boolean)

  const approvalItems = [
    isSchoolDirector.value || isCollegeManager.value
      ? { path: '/admin/create-applies', label: '实验室创建审批', icon: DocumentCopy }
      : null,
    isSchoolDirector.value || isCollegeManager.value
      ? { path: '/admin/teacher-register-applies', label: '教师注册审批', icon: DocumentChecked }
      : null,
    isSchoolDirector.value || isCollegeManager.value
      ? { path: '/admin/attendance-tasks', label: '考勤任务', icon: Calendar }
      : null,
    isLabManager.value ? { path: '/admin/applications', label: '成员申请审核', icon: UserFilled } : null
  ].filter(Boolean)

  const operationItems = [
    isSchoolDirector.value || isCollegeManager.value || isLabManager.value
      ? { path: '/admin/workspace', label: '实验室工作台', icon: Files }
      : null,
    isLabManager.value ? { path: '/admin/plans', label: '招新计划', icon: Calendar } : null,
    isLabManager.value ? { path: '/admin/members', label: '成员管理', icon: User } : null,
    isLabManager.value ? { path: '/admin/notices', label: '公告管理', icon: Bell } : null,
    isSchoolDirector.value || isLabManager.value
      ? {
          path: '/admin/statistics',
          label: '统计分析',
          icon: TrendCharts,
          children: [
            { path: '/admin/statistics?tab=overview', label: '运营概览' },
            { path: '/admin/statistics?tab=trends', label: '趋势分析' },
            { path: '/admin/statistics?tab=activity', label: '实时动态' }
          ]
        }
      : null
  ].filter(Boolean)

  return [
    {
      label: '概览',
      items: [{ path: '/admin/dashboard', label: '工作台', icon: DataBoard }]
    },
    { label: '治理', items: governanceItems },
    { label: '审批', items: approvalItems },
    { label: '运营', items: operationItems },
    {
      label: '账号',
      items: [{ path: '/admin/profile', label: '个人信息', icon: UserFilled }]
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
    await router.push('/admin/profile')
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
