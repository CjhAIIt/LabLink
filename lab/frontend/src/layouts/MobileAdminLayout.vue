<template>
  <MobileLayout
    portal-role="admin"
    :title="title"
    subtitle="管理员移动工作台"
    :context-label="scopeLabel"
    home-path="/m/admin/dashboard"
    profile-path="/m/admin/profile"
    notification-path="/m/admin/notifications"
    :tab-items="tabItems"
    :menu-items="fullMenuItems"
    menu-title="管理端全部功能"
    menu-eyebrow="Admin Suite"
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

const title = computed(() => route.meta.title || '管理首页')
const scopeLabel = computed(() =>
  userStore.userInfo?.labName || userStore.userInfo?.collegeName || userStore.userInfo?.college || '综合管理范围'
)

const hasAny = (permissions) => permissions.some((permission) => userStore.hasPermission(permission))

const reviewTab = computed(() => {
  if (userStore.hasPermission('lab:apply:audit')) {
    return { path: '/m/admin/applications', label: '审核', icon: 'Tickets' }
  }
  if (userStore.hasPermission('lab:create:audit')) {
    return { path: '/m/admin/create-applies', label: '审核', icon: 'CircleCheck' }
  }
  if (userStore.hasPermission('teacher:register:audit')) {
    return { path: '/m/admin/teacher-register-applies', label: '审核', icon: 'UserFilled' }
  }
  if (userStore.hasPermission('profile:review')) {
    return { path: '/m/admin/profiles', label: '审核', icon: 'Files' }
  }
  return { path: '/m/admin/labs', label: '实验室', icon: 'OfficeBuilding' }
})

const statisticsTab = computed(() => {
  if (hasAny(['statistics:school:view', 'statistics:college:view', 'statistics:lab:view'])) {
    return { path: '/m/admin/statistics', label: '统计', icon: 'TrendCharts' }
  }
  if (hasAny(['attendance:task:manage', 'attendance:record:manage'])) {
    return { path: '/m/admin/attendance', label: '考勤', icon: 'Calendar' }
  }
  return { path: '/m/admin/notices', label: '公告', icon: 'Bell' }
})

const fallbackFullMenuItems = computed(() =>
  [
    { mobilePath: '/m/admin/dashboard', label: '管理首页', icon: 'DataBoard' },
    userStore.hasPermission('lab:manage') ? { mobilePath: '/m/admin/labs', label: '实验室管理', icon: 'OfficeBuilding' } : null,
    userStore.hasPermission('lab:apply:audit') ? { mobilePath: '/m/admin/applications', label: '学生加入审核', icon: 'Tickets' } : null,
    userStore.hasPermission('lab:create:audit') ? { mobilePath: '/m/admin/create-applies', label: '实验室创建审核', icon: 'CircleCheck' } : null,
    userStore.hasPermission('teacher:register:audit') ? { mobilePath: '/m/admin/teacher-register-applies', label: '教师注册审核', icon: 'UserFilled' } : null,
    hasAny(['attendance:task:manage', 'attendance:record:manage']) ? { mobilePath: '/m/admin/attendance', label: '考勤管理', icon: 'Calendar' } : null,
    userStore.hasPermission('member:manage') ? { mobilePath: '/m/admin/members', label: '成员管理', icon: 'UserFilled' } : null,
    userStore.hasPermission('device:manage') ? { mobilePath: '/m/admin/devices', label: '设备中心', icon: 'Monitor' } : null,
    userStore.hasPermission('profile:review') ? { mobilePath: '/m/admin/profiles', label: '资料审核', icon: 'Files' } : null,
    hasAny(['statistics:school:view', 'statistics:college:view', 'statistics:lab:view']) ? { mobilePath: '/m/admin/statistics', label: '统计分析', icon: 'TrendCharts' } : null,
    userStore.hasPermission('exam:manage') ? { mobilePath: '/m/admin/exam-hub', label: '笔试 / 阅卷中心', icon: 'EditPen' } : null,
    userStore.hasPermission('ai-interview:record:view') ? { mobilePath: '/m/admin/ai-interview-records', label: 'AI 面试记录', icon: 'ChatDotRound' } : null,
    userStore.hasPermission('notice:manage') ? { mobilePath: '/m/admin/notices-manage', label: '公告管理', icon: 'Bell' } : null,
    { mobilePath: '/m/admin/notices', label: '公告查看', icon: 'Bell' },
    { mobilePath: '/m/admin/notifications', label: '消息中心', icon: 'Message' },
    { mobilePath: '/m/admin/profile', label: '我的', icon: 'User' }
  ].filter(Boolean)
)

const fullMenuItems = computed(() => resolveMobileMenuItems(userStore.menus, fallbackFullMenuItems.value))

const tabItems = computed(() => [
  { path: '/m/admin/dashboard', label: '首页', icon: 'DataBoard' },
  reviewTab.value,
  statisticsTab.value,
  { path: '/m/admin/notifications', label: '消息', icon: 'Message', badge: true },
  { path: '/m/admin/profile', label: '我的', icon: 'User' }
])
</script>
