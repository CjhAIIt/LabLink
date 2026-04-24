import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getUserInfo } from '@/utils/auth'
import {
  getPortalSurface,
  isMobilePortal,
  resolvePortalHome,
  resolvePortalLogin,
  resolvePortalRole,
  resolvePublicEntry,
  resolveRouteSurface,
  setPortalSurface
} from '@/utils/portal'

import AdminLayout from '@/layouts/AdminLayout.vue'
import StudentLayout from '@/layouts/StudentLayout.vue'
import TeacherLayout from '@/layouts/TeacherLayout.vue'
import MobileAdminLayout from '@/layouts/MobileAdminLayout.vue'
import MobileStudentLayout from '@/layouts/MobileStudentLayout.vue'
import MobileTeacherLayout from '@/layouts/MobileTeacherLayout.vue'

import Intro from '@/views/Intro.vue'
import Login from '@/views/Login.vue'
import MobileLogin from '@/views/mobile/Login.vue'
import Register from '@/views/Register.vue'
import TeacherRegister from '@/views/TeacherRegister.vue'
import MobileRegister from '@/views/mobile/Register.vue'
import MobileTeacherRegister from '@/views/mobile/TeacherRegister.vue'
import LabInfoDetail from '@/views/LabInfoDetail.vue'
import AdminApplications from '@/views/admin/Applications.vue'
import AdminAttendanceTasks from '@/views/admin/AttendanceTasks.vue'
import AdminAttendanceDashboard from '@/views/admin/AttendanceDashboard.vue'
import AdminAttendanceStats from '@/views/admin/AttendanceStats.vue'
import AdminAttendanceAnomaly from '@/views/admin/AttendanceAnomaly.vue'
import AdminAttendanceLeaveApproval from '@/views/admin/AttendanceLeaveApproval.vue'
import AdminExamHub from '@/views/admin/ExamHub.vue'
import AdminExamManage from '@/views/admin/ExamManage.vue'
import AdminQuestionBank from '@/views/admin/QuestionBank.vue'
import AdminPaperCompose from '@/views/admin/PaperCompose.vue'
import AdminGradingCenter from '@/views/admin/GradingCenter.vue'
import AdminExamStatistics from '@/views/admin/ExamStatistics.vue'
import AdminColleges from '@/views/admin/Colleges.vue'
import AdminCreateApplies from '@/views/admin/LabCreateApplies.vue'
import AdminDashboard from '@/views/admin/Dashboard.vue'
import AdminDevices from '@/views/admin/Devices.vue'
import AdminAuditLogs from '@/views/admin/AuditLogs.vue'
import AdminLabWorkspace from '@/views/admin/LabWorkspace.vue'
import AdminLabInfoManagement from '@/views/admin/LabInfoManagement.vue'
import AdminLabs from '@/views/admin/Labs.vue'
import AdminMembers from '@/views/admin/Members.vue'
import AdminNotices from '@/views/admin/Notices.vue'
import AdminPlans from '@/views/admin/Plans.vue'
import AdminProfileReviews from '@/views/admin/ProfileReviews.vue'
import AdminSearchCenter from '@/views/admin/SearchCenter.vue'
import AdminStatistics from '@/views/admin/Statistics.vue'
import AdminTeacherRegisterApplies from '@/views/admin/TeacherRegisterApplies.vue'
import StudentApplications from '@/views/student/Applications.vue'
import StudentAttendance from '@/views/student/Attendance.vue'
import StudentExamCenter from '@/views/student/ExamCenter.vue'
import StudentExamDetail from '@/views/student/ExamDetail.vue'
import StudentExamSign from '@/views/student/ExamSign.vue'
import StudentExamTake from '@/views/student/ExamTake.vue'
import StudentExamResult from '@/views/student/ExamResult.vue'
import StudentDashboard from '@/views/student/Dashboard.vue'
import StudentLabs from '@/views/student/Labs.vue'
import StudentLabDetail from '@/views/student/LabDetail.vue'
import StudentMyLab from '@/views/student/MyLab.vue'
import StudentNotices from '@/views/student/Notices.vue'
import StudentSpace from '@/views/student/Space.vue'
import Profile from '@/views/student/Profile.vue'
import Notifications from '@/views/shared/Notifications.vue'
import TeacherAttendanceOverview from '@/views/teacher/AttendanceOverview.vue'
import TeacherDashboard from '@/views/teacher/Dashboard.vue'
import TeacherLabCreateApplies from '@/views/teacher/LabCreateApplies.vue'
import TeacherProfileDirectory from '@/views/teacher/ProfileDirectory.vue'
import MobileNotices from '@/views/mobile/Notices.vue'
import MobileProfile from '@/views/mobile/Profile.vue'
import MobileNotifications from '@/views/mobile/shared/Notifications.vue'
import MobileAdminDashboard from '@/views/mobile/admin/Dashboard.vue'
import MobileAdminApplications from '@/views/mobile/admin/Applications.vue'
import MobileAdminAttendance from '@/views/mobile/admin/Attendance.vue'
import MobileAdminCreateApplies from '@/views/mobile/admin/CreateApplies.vue'
import MobileAdminLabs from '@/views/mobile/admin/Labs.vue'
import MobileAdminMembers from '@/views/mobile/admin/Members.vue'
import MobileAdminDevices from '@/views/mobile/admin/Devices.vue'
import MobileAdminNoticeManagement from '@/views/mobile/admin/NoticeManagement.vue'
import MobileAdminProfileReviews from '@/views/mobile/admin/ProfileReviews.vue'
import MobileAdminTeacherRegisterApplies from '@/views/mobile/admin/TeacherRegisterApplies.vue'
import MobileAdminStatistics from '@/views/admin/Statistics.vue'
import MobileStudentApplications from '@/views/mobile/student/Applications.vue'
import MobileStudentAttendance from '@/views/mobile/student/Attendance.vue'
import MobileStudentDashboard from '@/views/mobile/student/Dashboard.vue'
import MobileStudentLabDetail from '@/views/mobile/student/LabDetail.vue'
import MobileStudentLabs from '@/views/mobile/student/Labs.vue'
import MobileStudentMyLab from '@/views/mobile/student/MyLab.vue'
import MobileStudentSpace from '@/views/mobile/student/Space.vue'
import MobileTeacherAttendance from '@/views/mobile/teacher/Attendance.vue'
import MobileTeacherCreateApplies from '@/views/mobile/teacher/CreateApplies.vue'
import MobileTeacherDashboard from '@/views/mobile/teacher/Dashboard.vue'

// AI 面试
import AiInterviewHome from '@/views/student/AiInterviewHome.vue'
import AiInterviewMock from '@/views/student/AiInterviewMock.vue'
import AiInterviewFormal from '@/views/student/AiInterviewFormal.vue'
import AiInterviewSession from '@/views/student/AiInterviewSession.vue'
import AiInterviewReport from '@/views/student/AiInterviewReport.vue'
import AdminAiInterviewModules from '@/views/admin/AiInterviewModules.vue'
import AdminAiInterviewRecords from '@/views/admin/AiInterviewRecords.vue'
import AdminAiInterviewDetail from '@/views/admin/AiInterviewDetail.vue'

const PLATFORM_TITLE = 'LabLink'
const DESKTOP_AUTH_PATHS = new Map([
  ['/login', '/m/login'],
  ['/register', '/m/register'],
  ['/teacher-register', '/m/teacher-register']
])
const MOBILE_AUTH_PATHS = new Map([
  ['/m/login', '/login'],
  ['/m/register', '/register'],
  ['/m/teacher-register', '/teacher-register']
])
const USER_PERMISSIONS_KEY = 'lab_user_permissions'
const TOKEN_BYPASS_PATHS = new Set([
  '/',
  '/intro',
  '/login',
  '/register',
  '/teacher-register',
  '/m',
  '/m/login',
  '/m/register',
  '/m/teacher-register'
])

const routes = [
  {
    path: '/',
    redirect: () => resolvePublicEntry({ surface: getPortalSurface() })
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
    path: '/m',
    redirect: '/m/login'
  },
  {
    path: '/m/login',
    name: 'MobileLogin',
    component: MobileLogin,
    meta: { title: '移动端登录', requiresAuth: false }
  },
  {
    path: '/m/register',
    name: 'MobileRegister',
    component: MobileRegister,
    meta: { title: '学生注册', requiresAuth: false }
  },
  {
    path: '/m/teacher-register',
    name: 'MobileTeacherRegister',
    component: MobileTeacherRegister,
    meta: { title: '教师注册', requiresAuth: false }
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: AdminDashboard, meta: { title: '管理工作台' } },
      { path: 'search', component: AdminSearchCenter, meta: { title: '综合搜索' } },
      { path: 'colleges', component: AdminColleges, meta: { title: '学院管理' } },
      { path: 'labs', component: AdminLabs, meta: { title: '实验室管理' } },
      { path: 'create-applies', component: AdminCreateApplies, meta: { title: '实验室创建申请' } },
      { path: 'teacher-register-applies', component: AdminTeacherRegisterApplies, meta: { title: '教师注册申请' } },
      { path: 'attendance-tasks', component: AdminAttendanceTasks, meta: { title: '考勤管理' } },
      { path: 'attendance-dashboard', component: AdminAttendanceDashboard, meta: { title: '今日出勤看板' } },
      { path: 'attendance-stats', component: AdminAttendanceStats, meta: { title: '出勤统计报表' } },
      { path: 'attendance-anomaly', component: AdminAttendanceAnomaly, meta: { title: '异常出勤处理' } },
      { path: 'attendance-leave', component: AdminAttendanceLeaveApproval, meta: { title: '请假审批' } },
      { path: 'exam-hub', component: AdminExamHub, meta: { title: '笔试中心' } },
      { path: 'exam-manage', component: AdminExamManage, meta: { title: '笔试管理' } },
      { path: 'question-bank', component: AdminQuestionBank, meta: { title: '题库管理' } },
      { path: 'paper-compose', component: AdminPaperCompose, meta: { title: '组卷' } },
      { path: 'grading-center', component: AdminGradingCenter, meta: { title: '阅卷中心' } },
      { path: 'exam-statistics', component: AdminExamStatistics, meta: { title: '成绩统计' } },
      { path: 'lab-info', component: AdminLabInfoManagement, meta: { title: '实验室资料' } },
      { path: 'workspace', component: AdminLabWorkspace, meta: { title: '资料空间' } },
      { path: 'devices', component: AdminDevices, meta: { title: '设备管理' } },
      { path: 'profiles', component: AdminProfileReviews, meta: { title: '资料审核' } },
      { path: 'audit', component: AdminAuditLogs, meta: { title: '审计日志' } },
      { path: 'notifications', component: Notifications, meta: { title: '消息中心' } },
      { path: 'plans', component: AdminPlans, meta: { title: '招新计划' } },
      { path: 'applications', component: AdminApplications, meta: { title: '申请管理' } },
      { path: 'members', component: AdminMembers, meta: { title: '成员管理' } },
      { path: 'notices', component: AdminNotices, meta: { title: '公告管理' } },
      { path: 'statistics', component: AdminStatistics, meta: { title: '统计分析' } },
      { path: 'ai-interview-modules', component: AdminAiInterviewModules, meta: { title: 'AI 面试模块管理' } },
      { path: 'ai-interview-records', component: AdminAiInterviewRecords, meta: { title: 'AI 面试记录' } },
      { path: 'ai-interview-records/:id', component: AdminAiInterviewDetail, meta: { title: '面试详情' } },
      { path: 'profile', component: Profile, meta: { title: '个人资料' } }
    ]
  },
  {
    path: '/student',
    component: StudentLayout,
    meta: { requiresAuth: true, role: 'student' },
    children: [
      { path: '', redirect: '/student/dashboard' },
      { path: 'dashboard', component: StudentDashboard, meta: { title: '学生工作台' } },
      { path: 'labs', component: StudentLabs, meta: { title: '实验室广场' } },
      { path: 'labs/:id', component: StudentLabDetail, meta: { title: '实验室详情' } },
      { path: 'applications', component: StudentApplications, meta: { title: '我的申请' } },
      { path: 'my-lab', component: StudentMyLab, meta: { title: '我的实验室' } },
      { path: 'attendance', component: StudentAttendance, meta: { title: '我的考勤' } },
      { path: 'check-in', redirect: '/student/attendance' },
      { path: 'exam-center', component: StudentExamCenter, meta: { title: '笔试中心' } },
      { path: 'exam-center/:examId', component: StudentExamDetail, meta: { title: '笔试详情' } },
      { path: 'exam-center/:examId/sign', component: StudentExamSign, meta: { title: '诚信签名' } },
      { path: 'exam-center/:examId/take', component: StudentExamTake, meta: { title: '在线答题', fullScreen: true } },
      { path: 'exam-center/:examId/result', component: StudentExamResult, meta: { title: '考试结果' } },
      { path: 'space', component: StudentSpace, meta: { title: '资料空间' } },
      { path: 'notifications', component: Notifications, meta: { title: '消息中心' } },
      { path: 'notices', component: StudentNotices, meta: { title: '公告通知' } },
      { path: 'ai-interview', component: AiInterviewHome, meta: { title: 'AI 智能面试' } },
      { path: 'ai-interview/mock', component: AiInterviewMock, meta: { title: '模拟面试' } },
      { path: 'ai-interview/formal', component: AiInterviewFormal, meta: { title: '正式 AI 面试' } },
      { path: 'ai-interview/session', component: AiInterviewSession, meta: { title: '面试进行中' } },
      { path: 'ai-interview/report', component: AiInterviewReport, meta: { title: '面试报告' } },
      { path: 'profile', component: Profile, meta: { title: '个人资料' } }
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
      { path: 'attendance', component: TeacherAttendanceOverview, meta: { title: '考勤查看' } },
      { path: 'lab-info', component: AdminLabInfoManagement, meta: { title: '实验室资料' } },
      { path: 'exam-hub', component: AdminExamHub, meta: { title: '笔试中心' } },
      { path: 'exam-manage', component: AdminExamManage, meta: { title: '笔试管理' } },
      { path: 'question-bank', component: AdminQuestionBank, meta: { title: '题库管理' } },
      { path: 'paper-compose', component: AdminPaperCompose, meta: { title: '组卷' } },
      { path: 'grading-center', component: AdminGradingCenter, meta: { title: '阅卷中心' } },
      { path: 'exam-statistics', component: AdminExamStatistics, meta: { title: '成绩统计' } },
      { path: 'profiles', component: TeacherProfileDirectory, meta: { title: '成员资料' } },
      { path: 'workspace', component: AdminLabWorkspace, meta: { title: '资料空间' } },
      { path: 'notifications', component: Notifications, meta: { title: '消息中心' } },
      { path: 'notices', component: StudentNotices, meta: { title: '公告通知' } },
      { path: 'ai-interview-modules', component: AdminAiInterviewModules, meta: { title: 'AI 面试模块管理' } },
      { path: 'ai-interview-records', component: AdminAiInterviewRecords, meta: { title: 'AI 面试记录' } },
      { path: 'ai-interview-records/:id', component: AdminAiInterviewDetail, meta: { title: '面试详情' } },
      { path: 'profile', component: Profile, meta: { title: '个人资料' } }
    ]
  },
  {
    path: '/m/admin',
    component: MobileAdminLayout,
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      { path: '', redirect: '/m/admin/dashboard' },
      { path: 'dashboard', component: MobileAdminDashboard, meta: { title: '管理概览' } },
      { path: 'labs', component: MobileAdminLabs, meta: { title: '实验室管理' } },
      { path: 'applications', component: MobileAdminApplications, meta: { title: '成员申请审批' } },
      { path: 'create-applies', component: MobileAdminCreateApplies, meta: { title: '实验室创建审批' } },
      { path: 'teacher-register-applies', component: MobileAdminTeacherRegisterApplies, meta: { title: '教师注册审批' } },
      { path: 'attendance', component: MobileAdminAttendance, meta: { title: '考勤管理' } },
      { path: 'members', component: MobileAdminMembers, meta: { title: '成员管理' } },
      { path: 'devices', component: MobileAdminDevices, meta: { title: '设备中心' } },
      { path: 'profiles', component: MobileAdminProfileReviews, meta: { title: '档案审核' } },
      { path: 'statistics', component: MobileAdminStatistics, meta: { title: '统计分析' } },
      { path: 'exam-hub', component: AdminExamHub, meta: { title: '笔试中心' } },
      { path: 'exam-manage', component: AdminExamManage, meta: { title: '笔试管理' } },
      { path: 'question-bank', component: AdminQuestionBank, meta: { title: '题库管理' } },
      { path: 'paper-compose', component: AdminPaperCompose, meta: { title: '组卷' } },
      { path: 'grading-center', component: AdminGradingCenter, meta: { title: '阅卷中心' } },
      { path: 'exam-statistics', component: AdminExamStatistics, meta: { title: '成绩统计' } },
      { path: 'notices-manage', component: MobileAdminNoticeManagement, meta: { title: '公告管理' } },
      { path: 'ai-interview-records', component: AdminAiInterviewRecords, meta: { title: 'AI 面试记录' } },
      { path: 'ai-interview-records/:id', component: AdminAiInterviewDetail, meta: { title: '面试详情' } },
      { path: 'notifications', component: MobileNotifications, meta: { title: '消息中心' } },
      { path: 'notices', component: MobileNotices, meta: { title: '公告中心' } },
      { path: 'profile', component: MobileProfile, meta: { title: '个人资料' } }
    ]
  },
  {
    path: '/m/student',
    component: MobileStudentLayout,
    meta: { requiresAuth: true, role: 'student' },
    children: [
      { path: '', redirect: '/m/student/dashboard' },
      { path: 'dashboard', component: MobileStudentDashboard, meta: { title: '学生首页' } },
      { path: 'labs', component: MobileStudentLabs, meta: { title: '实验室广场' } },
      { path: 'labs/:id', component: MobileStudentLabDetail, meta: { title: '实验室详情' } },
      { path: 'applications', component: MobileStudentApplications, meta: { title: '我的申请' } },
      { path: 'my-lab', component: MobileStudentMyLab, meta: { title: '我的实验室' } },
      { path: 'attendance', component: MobileStudentAttendance, meta: { title: '我的考勤' } },
      { path: 'check-in', redirect: '/m/student/attendance' },
      { path: 'exam-center', component: StudentExamCenter, meta: { title: '笔试中心' } },
      { path: 'exam-center/:examId', component: StudentExamDetail, meta: { title: '笔试详情' } },
      { path: 'exam-center/:examId/sign', component: StudentExamSign, meta: { title: '诚信签名' } },
      { path: 'exam-center/:examId/take', component: StudentExamTake, meta: { title: '在线答题', fullScreen: true } },
      { path: 'exam-center/:examId/result', component: StudentExamResult, meta: { title: '考试结果' } },
      { path: 'space', component: MobileStudentSpace, meta: { title: '资料空间' } },
      { path: 'notifications', component: MobileNotifications, meta: { title: '消息中心' } },
      { path: 'notices', component: MobileNotices, meta: { title: '公告通知' } },
      { path: 'ai-interview', component: AiInterviewHome, meta: { title: 'AI 智能面试' } },
      { path: 'ai-interview/mock', component: AiInterviewMock, meta: { title: '模拟面试' } },
      { path: 'ai-interview/formal', component: AiInterviewFormal, meta: { title: '正式 AI 面试' } },
      { path: 'ai-interview/session', component: AiInterviewSession, meta: { title: '面试进行中' } },
      { path: 'ai-interview/report', component: AiInterviewReport, meta: { title: '面试报告' } },
      { path: 'profile', component: MobileProfile, meta: { title: '个人资料' } }
    ]
  },
  {
    path: '/m/teacher',
    component: MobileTeacherLayout,
    meta: { requiresAuth: true, role: 'teacher' },
    children: [
      { path: '', redirect: '/m/teacher/dashboard' },
      { path: 'attendance', component: MobileTeacherAttendance, meta: { title: '考勤查看' } },
      { path: 'dashboard', component: MobileTeacherDashboard, meta: { title: '教师首页' } },
      { path: 'create-applies', component: MobileTeacherCreateApplies, meta: { title: '实验室创建申请' } },
      { path: 'exam-hub', component: AdminExamHub, meta: { title: '笔试中心' } },
      { path: 'exam-manage', component: AdminExamManage, meta: { title: '笔试管理' } },
      { path: 'question-bank', component: AdminQuestionBank, meta: { title: '题库管理' } },
      { path: 'paper-compose', component: AdminPaperCompose, meta: { title: '组卷' } },
      { path: 'grading-center', component: AdminGradingCenter, meta: { title: '阅卷中心' } },
      { path: 'exam-statistics', component: AdminExamStatistics, meta: { title: '成绩统计' } },
      { path: 'ai-interview-records', component: AdminAiInterviewRecords, meta: { title: 'AI 面试记录' } },
      { path: 'ai-interview-records/:id', component: AdminAiInterviewDetail, meta: { title: '面试详情' } },
      { path: 'notifications', component: MobileNotifications, meta: { title: '消息中心' } },
      { path: 'notices', component: MobileNotices, meta: { title: '公告通知' } },
      { path: 'profile', component: MobileProfile, meta: { title: '个人资料' } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: () => resolvePublicEntry({ surface: getPortalSurface() })
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

const resolveNavigationSurface = (path) => {
  if (path === '/m' || path.startsWith('/m/')) {
    return resolveRouteSurface(path)
  }

  const desktopPrefixes = ['/login', '/register', '/teacher-register', '/admin', '/student', '/teacher']
  if (desktopPrefixes.some((prefix) => path === prefix || path.startsWith(`${prefix}/`))) {
    return 'desktop'
  }

  return getPortalSurface()
}

const shouldPersistSurface = (path) =>
  path === '/m' ||
  path.startsWith('/m/') ||
  ['/login', '/register', '/teacher-register'].some((prefix) => path === prefix || path.startsWith(`${prefix}/`)) ||
  ['/admin', '/student', '/teacher'].some((prefix) => path === prefix || path.startsWith(`${prefix}/`))

const resolveForcedSurface = (to) => {
  const surface = Array.isArray(to.query?.surface) ? to.query.surface[0] : to.query?.surface
  return surface === 'desktop' || surface === 'mobile' ? surface : ''
}

const readStoredPermissions = (userInfo) => {
  const raw = localStorage.getItem(USER_PERMISSIONS_KEY)
  if (raw) {
    try {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) {
        return parsed
      }
    } catch (error) {
      // ignore invalid cached permissions
    }
  }

  return Array.isArray(userInfo?.permissions) ? userInfo.permissions : []
}

const hasAnyPermission = (permissions, expected = []) =>
  expected.length === 0 || expected.some((permission) => permissions.includes(permission))

const hasLabContext = (userInfo) => Boolean(userInfo?.managedLabId || userInfo?.labId)

const hasRouteFeatureAccess = (path, userInfo, permissions) => {
  if (path === '/admin/search') {
    return hasAnyPermission(permissions, ['search:global:view'])
  }
  if (path === '/admin/colleges') {
    return Boolean(userInfo?.schoolDirector || userInfo?.role === 'super_admin')
  }
  if (['/admin/labs', '/admin/plans', '/m/admin/labs'].includes(path)) {
    return hasAnyPermission(permissions, ['lab:manage'])
  }
  if (path === '/admin/lab-info') {
    return hasAnyPermission(permissions, ['workspace:lab:manage']) && hasLabContext(userInfo)
  }
  if (['/admin/create-applies', '/m/admin/create-applies'].includes(path)) {
    return hasAnyPermission(permissions, ['lab:create:audit'])
  }
  if (['/admin/teacher-register-applies', '/m/admin/teacher-register-applies'].includes(path)) {
    return hasAnyPermission(permissions, ['teacher:register:audit'])
  }
  if (['/admin/attendance-tasks', '/m/admin/attendance'].includes(path)) {
    return hasAnyPermission(permissions, ['attendance:task:manage', 'attendance:record:manage'])
  }
  if (['/admin/applications', '/m/admin/applications'].includes(path)) {
    return hasAnyPermission(permissions, ['lab:apply:audit'])
  }
  if (['/admin/members', '/m/admin/members'].includes(path)) {
    return hasAnyPermission(permissions, ['member:manage'])
  }
  if (path === '/admin/workspace') {
    return hasAnyPermission(permissions, ['workspace:school:view', 'workspace:college:view', 'workspace:lab:manage'])
  }
  if (['/admin/devices', '/m/admin/devices'].includes(path)) {
    return hasAnyPermission(permissions, ['device:manage'])
  }
  if (['/admin/profiles', '/m/admin/profiles'].includes(path)) {
    return hasAnyPermission(permissions, ['profile:review'])
  }
  if (path === '/admin/audit') {
    return hasAnyPermission(permissions, ['audit:view'])
  }
  if (['/admin/notices', '/m/admin/notices-manage'].includes(path)) {
    return hasAnyPermission(permissions, ['notice:manage'])
  }
  if (['/admin/statistics', '/m/admin/statistics'].includes(path)) {
    return hasAnyPermission(permissions, ['statistics:school:view', 'statistics:college:view', 'statistics:lab:view'])
  }
  if (/^\/(?:m\/)?(?:admin|teacher)\/ai-interview-records(?:\/[^/]+)?$/.test(path)) {
    return hasAnyPermission(permissions, ['ai-interview:record:view'])
  }
  if (/^\/(?:m\/)?(?:admin|teacher)\/(?:exam-hub|exam-manage|question-bank|paper-compose|grading-center|exam-statistics)(?:\/.*)?$/.test(path)) {
    return hasAnyPermission(permissions, ['exam:manage'])
  }

  if (['/student/applications', '/m/student/applications'].includes(path)) {
    return hasAnyPermission(permissions, ['lab:apply:self'])
  }
  if (/^\/(?:m\/)?student\/exam-center(?:\/[^/]+(?:\/(?:sign|take|result))?)?$/.test(path)) {
    return hasAnyPermission(permissions, ['exam:self:view'])
  }
  if (/^\/(?:m\/)?student\/ai-interview(?:\/(?:mock|formal|session|report))?$/.test(path)) {
    return hasAnyPermission(permissions, ['ai-interview:self:view'])
  }
  if (['/student/my-lab', '/student/attendance', '/student/space', '/m/student/my-lab', '/m/student/attendance', '/m/student/space'].includes(path)) {
    return hasLabContext(userInfo)
  }
  if (['/student/attendance', '/m/student/attendance'].includes(path)) {
    return hasAnyPermission(permissions, ['attendance:self:view']) && hasLabContext(userInfo)
  }
  if (['/student/space', '/m/student/space'].includes(path)) {
    return hasAnyPermission(permissions, ['workspace:self:view']) && hasLabContext(userInfo)
  }

  if (['/teacher/create-applies', '/m/teacher/create-applies'].includes(path)) {
    return hasAnyPermission(permissions, ['lab:create:apply'])
  }
  if (['/teacher/attendance', '/teacher/profiles', '/teacher/workspace', '/teacher/lab-info', '/m/teacher/attendance'].includes(path)) {
    return hasLabContext(userInfo)
  }
  if (['/teacher/attendance', '/m/teacher/attendance'].includes(path)) {
    return hasAnyPermission(permissions, ['attendance:view']) && hasLabContext(userInfo)
  }
  if (path === '/teacher/profiles') {
    return hasAnyPermission(permissions, ['profile:view']) && hasLabContext(userInfo)
  }
  if (path === '/teacher/workspace') {
    return hasAnyPermission(permissions, ['workspace:lab:manage', 'workspace:college:view', 'workspace:school:view']) && hasLabContext(userInfo)
  }
  if (path === '/teacher/lab-info') {
    return hasAnyPermission(permissions, ['lab:profile:submit', 'workspace:lab:manage']) && hasLabContext(userInfo)
  }

  return true
}

const buildSurfaceRedirect = (to, path) => {
  const query = { ...to.query }
  delete query.surface

  return {
    path,
    query,
    hash: to.hash,
    replace: true
  }
}

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - ${PLATFORM_TITLE}` : PLATFORM_TITLE

  const forcedSurface = resolveForcedSurface(to)
  if (!forcedSurface) {
    const mobileDevice = isMobilePortal()
    const mobileAuthPath = DESKTOP_AUTH_PATHS.get(to.path)
    const desktopAuthPath = MOBILE_AUTH_PATHS.get(to.path)

    if (mobileDevice && mobileAuthPath) {
      next(buildSurfaceRedirect(to, mobileAuthPath))
      return
    }

    if (!mobileDevice && desktopAuthPath) {
      next(buildSurfaceRedirect(to, desktopAuthPath))
      return
    }
  }

  const token = getToken()
  const portalRole = resolveCurrentPortalRole()
  const userInfo = getUserInfo() || {}
  const permissions = readStoredPermissions(userInfo)
  const navigationSurface = forcedSurface || resolveNavigationSurface(to.path)

  if (shouldPersistSurface(to.path)) {
    setPortalSurface(navigationSurface)
  }

  if (token && TOKEN_BYPASS_PATHS.has(to.path)) {
    next(resolvePortalHome(portalRole, { surface: navigationSurface }))
    return
  }

  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  if (!requiresAuth) {
    next()
    return
  }

  if (!token) {
    next(resolvePortalLogin({ surface: navigationSurface }))
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
    next(resolvePortalHome(portalRole, { surface: navigationSurface }))
    return
  }

  if (!hasRouteFeatureAccess(to.path, userInfo, permissions)) {
    next(resolvePortalHome(portalRole, { surface: navigationSurface }))
    return
  }

  next()
})

export default router
