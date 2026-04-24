<template>
  <MobileLayout
    portal-role="teacher"
    :title="title"
    subtitle="教师移动工作台"
    :context-label="scopeLabel"
    home-path="/m/teacher/dashboard"
    profile-path="/m/teacher/profile"
    notification-path="/m/teacher/notifications"
    :tab-items="tabItems"
    :menu-items="fullMenuItems"
    menu-title="教师端全部功能"
    menu-eyebrow="Teacher Suite"
  >
    <router-view />
  </MobileLayout>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import MobileLayout from '@/components/mobile/MobileLayout.vue'
import { useUserStore } from '@/stores/user'
import { resolveMobileMenuItems } from '@/utils/portal-menu'

const route = useRoute()
const userStore = useUserStore()

const title = computed(() => route.meta.title || '教师首页')
const hasWorkspace = computed(() => Boolean(userStore.userInfo?.labId || userStore.userInfo?.managedLabId))
const canCreateApply = computed(() => userStore.hasPermission('lab:create:apply'))
const canViewAttendance = computed(() => userStore.hasPermission('attendance:view') && hasWorkspace.value)
const canViewInterview = computed(() => userStore.hasPermission('ai-interview:record:view'))
const scopeLabel = computed(() => userStore.userInfo?.labName || '实验室信息待同步')

const fallbackFullMenuItems = computed(() =>
  [
    { mobilePath: '/m/teacher/dashboard', label: '教师首页', icon: 'DataBoard' },
    canCreateApply.value ? { mobilePath: '/m/teacher/create-applies', label: '实验室创建申请', icon: 'Tickets' } : null,
    canViewAttendance.value ? { mobilePath: '/m/teacher/attendance', label: '考勤查看', icon: 'Calendar' } : null,
    userStore.hasPermission('exam:manage') ? { mobilePath: '/m/teacher/exam-hub', label: '笔试 / 阅卷中心', icon: 'EditPen' } : null,
    canViewInterview.value ? { mobilePath: '/m/teacher/ai-interview-records', label: 'AI 面试记录', icon: 'ChatDotRound' } : null,
    { mobilePath: '/m/teacher/notices', label: '公告通知', icon: 'Bell' },
    { mobilePath: '/m/teacher/notifications', label: '消息中心', icon: 'Message' },
    { mobilePath: '/m/teacher/profile', label: '我的', icon: 'User' }
  ].filter(Boolean)
)

const fullMenuItems = computed(() => resolveMobileMenuItems(userStore.menus, fallbackFullMenuItems.value))

const workTab = computed(() => {
  if (canCreateApply.value) {
    return { path: '/m/teacher/create-applies', label: '申请', icon: 'Tickets' }
  }
  if (canViewInterview.value) {
    return { path: '/m/teacher/ai-interview-records', label: '面试', icon: 'ChatDotRound' }
  }
  return { path: '/m/teacher/notices', label: '公告', icon: 'Bell' }
})

const attendanceTab = computed(() => (
  canViewAttendance.value
    ? { path: '/m/teacher/attendance', label: '考勤', icon: 'Calendar' }
    : { path: '/m/teacher/notices', label: '公告', icon: 'Bell' }
))

const tabItems = computed(() => [
  { path: '/m/teacher/dashboard', label: '首页', icon: 'DataBoard' },
  workTab.value,
  attendanceTab.value,
  { path: '/m/teacher/notifications', label: '消息', icon: 'Message', badge: true },
  { path: '/m/teacher/profile', label: '我的', icon: 'User' }
])
</script>
