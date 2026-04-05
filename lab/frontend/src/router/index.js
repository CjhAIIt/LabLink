import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getUserInfo } from '@/utils/auth'
import { resolvePortalHome, resolvePortalRole } from '@/utils/portal'

import AdminLayout from '@/layouts/AdminLayout.vue'
import StudentLayout from '@/layouts/StudentLayout.vue'
import TeacherLayout from '@/layouts/TeacherLayout.vue'

import Intro from '@/views/Intro.vue'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import TeacherRegister from '@/views/TeacherRegister.vue'
import LabInfoDetail from '@/views/LabInfoDetail.vue'
import AdminApplications from '@/views/admin/Applications.vue'
import AdminAttendanceTasks from '@/views/admin/AttendanceTasks.vue'
import AdminColleges from '@/views/admin/Colleges.vue'
import AdminCreateApplies from '@/views/admin/LabCreateApplies.vue'
import AdminDashboard from '@/views/admin/Dashboard.vue'
import AdminLabWorkspace from '@/views/admin/LabWorkspace.vue'
import AdminLabs from '@/views/admin/Labs.vue'
import AdminMembers from '@/views/admin/Members.vue'
import AdminNotices from '@/views/admin/Notices.vue'
import AdminPlans from '@/views/admin/Plans.vue'
import AdminSearchCenter from '@/views/admin/SearchCenter.vue'
import AdminStatistics from '@/views/admin/Statistics.vue'
import AdminTeacherRegisterApplies from '@/views/admin/TeacherRegisterApplies.vue'
import StudentApplications from '@/views/student/Applications.vue'
import StudentAttendance from '@/views/student/Attendance.vue'
import StudentDashboard from '@/views/student/Dashboard.vue'
import StudentLabs from '@/views/student/Labs.vue'
import StudentLabDetail from '@/views/student/LabDetail.vue'
import StudentMyLab from '@/views/student/MyLab.vue'
import StudentNotices from '@/views/student/Notices.vue'
import StudentSpace from '@/views/student/Space.vue'
import Profile from '@/views/student/Profile.vue'
import TeacherDashboard from '@/views/teacher/Dashboard.vue'
import TeacherLabCreateApplies from '@/views/teacher/LabCreateApplies.vue'

const PLATFORM_TITLE = '安徽信息工程学院实验室管理平台'

const routes = [
  {
    path: '/',
    redirect: '/intro'
  },
  {
    path: '/intro',
    name: 'Intro',
    component: Intro,
    meta: { title: '平台介绍', requiresAuth: false }
  },
  {
    path: '/lab-info/:id',
    name: 'LabInfoDetail',
    component: LabInfoDetail,
    meta: { title: '实验室详情', requiresAuth: false }
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { title: '学生注册', requiresAuth: false }
  },
  {
    path: '/teacher-register',
    name: 'TeacherRegister',
    component: TeacherRegister,
    meta: { title: '教师注册', requiresAuth: false }
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: AdminDashboard, meta: { title: '管理工作台' } },
      { path: 'search', component: AdminSearchCenter, meta: { title: '综合检索' } },
      { path: 'colleges', component: AdminColleges, meta: { title: '学院管理' } },
      { path: 'labs', component: AdminLabs, meta: { title: '实验室管理' } },
      { path: 'create-applies', component: AdminCreateApplies, meta: { title: '实验室创建审批' } },
      { path: 'teacher-register-applies', component: AdminTeacherRegisterApplies, meta: { title: '教师注册审批' } },
      { path: 'attendance-tasks', component: AdminAttendanceTasks, meta: { title: '考勤任务' } },
      { path: 'workspace', component: AdminLabWorkspace, meta: { title: '实验室工作台' } },
      { path: 'plans', component: AdminPlans, meta: { title: '招新计划' } },
      { path: 'applications', component: AdminApplications, meta: { title: '成员申请审核' } },
      { path: 'members', component: AdminMembers, meta: { title: '成员管理' } },
      { path: 'notices', component: AdminNotices, meta: { title: '公告管理' } },
      { path: 'statistics', component: AdminStatistics, meta: { title: '统计分析' } },
      { path: 'profile', component: Profile, meta: { title: '个人信息' } }
    ]
  },
  {
    path: '/student',
    component: StudentLayout,
    meta: { requiresAuth: true, role: 'student' },
    children: [
      { path: '', redirect: '/student/dashboard' },
      { path: 'dashboard', component: StudentDashboard, meta: { title: '学生工作台' } },
      { path: 'labs', component: StudentLabs, meta: { title: '实验室总览' } },
      { path: 'labs/:id', component: StudentLabDetail, meta: { title: '实验室详情' } },
      { path: 'applications', component: StudentApplications, meta: { title: '我的申请' } },
      { path: 'my-lab', component: StudentMyLab, meta: { title: '我的实验室' } },
      { path: 'attendance', component: StudentAttendance, meta: { title: '考勤记录' } },
      { path: 'space', component: StudentSpace, meta: { title: '资料空间' } },
      { path: 'notices', component: StudentNotices, meta: { title: '公告中心' } },
      { path: 'profile', component: Profile, meta: { title: '个人信息' } }
    ]
  },
  {
    path: '/teacher',
    component: TeacherLayout,
    meta: { requiresAuth: true, role: 'teacher' },
    children: [
      { path: '', redirect: '/teacher/dashboard' },
      { path: 'dashboard', component: TeacherDashboard, meta: { title: '教师工作台' } },
      { path: 'create-applies', component: TeacherLabCreateApplies, meta: { title: '实验室创建申请' } },
      { path: 'workspace', component: AdminLabWorkspace, meta: { title: '实验室工作台' } },
      { path: 'notices', component: StudentNotices, meta: { title: '公告中心' } },
      { path: 'profile', component: Profile, meta: { title: '个人信息' } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const resolveCurrentPortalRole = () => {
  const storedPortalRole = localStorage.getItem('userPortalRole')
  if (storedPortalRole) {
    return storedPortalRole
  }
  return resolvePortalRole(getUserInfo())
}

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - ${PLATFORM_TITLE}` : PLATFORM_TITLE

  const token = getToken()
  const portalRole = resolveCurrentPortalRole()

  if (token && ['/', '/login', '/register', '/teacher-register', '/intro'].includes(to.path)) {
    next(resolvePortalHome(portalRole))
    return
  }

  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  if (!requiresAuth) {
    next()
    return
  }

  if (!token) {
    next('/login')
    return
  }

  const allowed = to.matched.every((record) => {
    const expectedRole = record.meta.role
    if (!expectedRole) {
      return true
    }
    if (Array.isArray(expectedRole)) {
      return expectedRole.includes(portalRole)
    }
    return expectedRole === portalRole
  })

  if (!allowed) {
    next(resolvePortalHome(portalRole))
    return
  }

  next()
})

export default router
