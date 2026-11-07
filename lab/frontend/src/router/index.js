import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getUserInfo } from '@/utils/auth'
import { isMobilePortal, resolvePortalHome, resolvePortalRole } from '@/utils/portal'

// 页面组件全部按路由懒加载，首屏只下载实际访问的模块。
const AdminLayout = () => import('@/layouts/AdminLayout.vue')
const StudentLayout = () => import('@/layouts/StudentLayout.vue')
const TeacherLayout = () => import('@/layouts/TeacherLayout.vue')
const MobileStudentLayout = () => import('@/layouts/MobileStudentLayout.vue')
const MobileTeacherLayout = () => import('@/layouts/MobileTeacherLayout.vue')

const Intro = () => import('@/views/Intro.vue')
const Login = () => import('@/views/Login.vue')
const Register = () => import('@/views/Register.vue')
const TeacherRegister = () => import('@/views/TeacherRegister.vue')
const LabInfoDetail = () => import('@/views/LabInfoDetail.vue')
const AdminApplications = () => import('@/views/admin/Applications.vue')
const AdminAttendanceTasks = () => import('@/views/admin/AttendanceTasks.vue')
const AdminColleges = () => import('@/views/admin/Colleges.vue')
const AdminCreateApplies = () => import('@/views/admin/LabCreateApplies.vue')
const AdminDashboard = () => import('@/views/admin/Dashboard.vue')
const AdminLabWorkspace = () => import('@/views/admin/LabWorkspace.vue')
const AdminLabs = () => import('@/views/admin/Labs.vue')
const AdminMembers = () => import('@/views/admin/Members.vue')
const AdminNotices = () => import('@/views/admin/Notices.vue')
const AdminPlans = () => import('@/views/admin/Plans.vue')
const AdminSearchCenter = () => import('@/views/admin/SearchCenter.vue')
const AdminStatistics = () => import('@/views/admin/Statistics.vue')
const AdminTeacherRegisterApplies = () => import('@/views/admin/TeacherRegisterApplies.vue')
const StudentApplications = () => import('@/views/student/Applications.vue')
const StudentAttendance = () => import('@/views/student/Attendance.vue')
const StudentDashboard = () => import('@/views/student/Dashboard.vue')
const StudentLabs = () => import('@/views/student/Labs.vue')
const StudentLabDetail = () => import('@/views/student/LabDetail.vue')
const StudentMyLab = () => import('@/views/student/MyLab.vue')
const StudentNotices = () => import('@/views/student/Notices.vue')
const StudentSpace = () => import('@/views/student/Space.vue')
const Profile = () => import('@/views/student/Profile.vue')
const TeacherDashboard = () => import('@/views/teacher/Dashboard.vue')
const TeacherLabCreateApplies = () => import('@/views/teacher/LabCreateApplies.vue')
const MobileNotices = () => import('@/views/mobile/Notices.vue')
const MobileProfile = () => import('@/views/mobile/Profile.vue')
const MobileStudentApplications = () => import('@/views/mobile/student/Applications.vue')
const MobileStudentDashboard = () => import('@/views/mobile/student/Dashboard.vue')
const MobileStudentLabDetail = () => import('@/views/mobile/student/LabDetail.vue')
const MobileStudentLabs = () => import('@/views/mobile/student/Labs.vue')
const MobileTeacherCreateApplies = () => import('@/views/mobile/teacher/CreateApplies.vue')
const MobileTeacherDashboard = () => import('@/views/mobile/teacher/Dashboard.vue')

const PLATFORM_TITLE = 'LabLink'

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
    path: '/m/student',
    component: MobileStudentLayout,
    meta: { requiresAuth: true, role: 'student' },
    children: [
      { path: '', redirect: '/m/student/dashboard' },
      { path: 'dashboard', component: MobileStudentDashboard, meta: { title: '学生首页' } },
      { path: 'labs', component: MobileStudentLabs, meta: { title: '实验室总览' } },
      { path: 'labs/:id', component: MobileStudentLabDetail, meta: { title: '实验室详情' } },
      { path: 'applications', component: MobileStudentApplications, meta: { title: '我的申请' } },
      { path: 'notices', component: MobileNotices, meta: { title: '公告中心' } },
      { path: 'profile', component: MobileProfile, meta: { title: '我的' } }
    ]
  },
  {
    path: '/m/teacher',
    component: MobileTeacherLayout,
    meta: { requiresAuth: true, role: 'teacher' },
    children: [
      { path: '', redirect: '/m/teacher/dashboard' },
      { path: 'dashboard', component: MobileTeacherDashboard, meta: { title: '教师首页' } },
      { path: 'create-applies', component: MobileTeacherCreateApplies, meta: { title: '创建申请' } },
      { path: 'notices', component: MobileNotices, meta: { title: '公告中心' } },
      { path: 'profile', component: MobileProfile, meta: { title: '我的' } }
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

const CATCH_ALL_ROUTE_PATH = '/:pathMatch(.*)*'

const hasRoute = (path) => {
  const resolved = router.resolve({ path })
  return resolved.matched.length > 0 && !resolved.matched.some((record) => record.path === CATCH_ALL_ROUTE_PATH)
}

const resolveCounterpartPath = (path, fromPrefix, toPrefix) => {
  if (!path.startsWith(fromPrefix)) {
    return null
  }
  const candidate = path.replace(fromPrefix, toPrefix)
  return hasRoute(candidate) ? candidate : null
}

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - ${PLATFORM_TITLE}` : PLATFORM_TITLE

  const token = getToken()
  const portalRole = resolveCurrentPortalRole()
  const shouldUseMobile = isMobilePortal()

  if (shouldUseMobile) {
    const studentTarget = resolveCounterpartPath(to.path, '/student', '/m/student')
    if (studentTarget) {
      next({ path: studentTarget, query: to.query, hash: to.hash })
      return
    }
    const teacherTarget = resolveCounterpartPath(to.path, '/teacher', '/m/teacher')
    if (teacherTarget) {
      next({ path: teacherTarget, query: to.query, hash: to.hash })
      return
    }
  } else {
    const studentTarget = resolveCounterpartPath(to.path, '/m/student', '/student')
    if (studentTarget) {
      next({ path: studentTarget, query: to.query, hash: to.hash })
      return
    }
    const teacherTarget = resolveCounterpartPath(to.path, '/m/teacher', '/teacher')
    if (teacherTarget) {
      next({ path: teacherTarget, query: to.query, hash: to.hash })
      return
    }
  }

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
