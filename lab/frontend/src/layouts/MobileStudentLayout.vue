<template>
  <MobileLayout
    portal-role="student"
    :title="title"
    subtitle="学生移动工作台"
    :context-label="scopeLabel"
    home-path="/m/student/dashboard"
    profile-path="/m/student/profile"
    notification-path="/m/student/notifications"
    :tab-items="tabItems"
    :menu-items="fullMenuItems"
    :full-screen="isFullScreenRoute"
    force-auth-context
    menu-title="学生端全部功能"
    menu-eyebrow="Student Suite"
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

const title = computed(() => route.meta.title || '学生首页')
const isFullScreenRoute = computed(() => Boolean(route.meta?.fullScreen))
const hasLabAccess = computed(() => Boolean(userStore.userInfo?.labId || userStore.userInfo?.labName))
const canApplyLab = computed(() => userStore.hasPermission('lab:apply:self'))
const canUseExam = computed(() => userStore.hasPermission('exam:self:view'))
const canUseInterview = computed(() => userStore.hasPermission('ai-interview:self:view'))
const scopeLabel = computed(() => userStore.userInfo?.labName || '暂未加入实验室')

const fallbackFullMenuItems = computed(() =>
  [
    { mobilePath: '/m/student/dashboard', label: '首页工作台', icon: 'DataBoard' },
    { mobilePath: '/m/student/labs', label: '实验室广场', icon: 'OfficeBuilding' },
    canApplyLab.value ? { mobilePath: '/m/student/applications', label: '申请记录', icon: 'Tickets' } : null,
    hasLabAccess.value ? { mobilePath: '/m/student/my-lab', label: '我的实验室', icon: 'School' } : null,
    hasLabAccess.value ? { mobilePath: '/m/student/attendance', label: '考勤打卡', icon: 'Calendar' } : null,
    hasLabAccess.value ? { mobilePath: '/m/student/space', label: '资料空间', icon: 'Files' } : null,
    canUseExam.value ? { mobilePath: '/m/student/exam-center', label: '笔试中心', icon: 'EditPen' } : null,
    canUseInterview.value ? { mobilePath: '/m/student/ai-interview', label: 'AI 面试', icon: 'ChatDotRound' } : null,
    { mobilePath: '/m/student/notices', label: '公告通知', icon: 'Bell' },
    { mobilePath: '/m/student/notifications', label: '消息中心', icon: 'Message' },
    { mobilePath: '/m/student/profile', label: '我的', icon: 'User' }
  ].filter(Boolean)
)

const fullMenuItems = computed(() => resolveMobileMenuItems(userStore.menus, fallbackFullMenuItems.value))

const actionTab = computed(() => {
  if (hasLabAccess.value) {
    return { path: '/m/student/attendance', label: '考勤', icon: 'Calendar' }
  }
  if (canApplyLab.value) {
    return { path: '/m/student/applications', label: '申请', icon: 'Tickets' }
  }
  if (canUseExam.value) {
    return { path: '/m/student/exam-center', label: '笔试', icon: 'EditPen' }
  }
  return { path: '/m/student/notices', label: '公告', icon: 'Bell' }
})

const tabItems = computed(() => [
  { path: '/m/student/dashboard', label: '首页', icon: 'DataBoard' },
  { path: '/m/student/labs', label: '实验室', icon: 'OfficeBuilding' },
  actionTab.value,
  { path: '/m/student/notifications', label: '消息', icon: 'Message', badge: true },
  { path: '/m/student/profile', label: '我的', icon: 'User' }
])
</script>
