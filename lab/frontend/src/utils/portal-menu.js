import {
  Bell,
  Calendar,
  CircleCheck,
  ChatDotRound,
  DataBoard,
  EditPen,
  Files,
  FolderOpened,
  Message,
  Monitor,
  OfficeBuilding,
  School,
  Search,
  Tickets,
  TrendCharts,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import { mapPathToSurface } from '@/utils/portal'

const MENU_ICON_MAP = {
  Bell,
  Calendar,
  CircleCheck,
  ChatDotRound,
  DataBoard,
  EditPen,
  Files,
  FolderOpened,
  Message,
  Monitor,
  OfficeBuilding,
  School,
  Search,
  Tickets,
  TrendCharts,
  User,
  UserFilled
}

function normalizeMenuPath(path) {
  if (path === '/student/check-in') {
    return '/student/attendance'
  }
  if (path === '/m/student/check-in') {
    return '/m/student/attendance'
  }
  return path
}

function normalizeMenuLabel(label, path) {
  if ((path === '/student/check-in' || path === '/m/student/check-in') && (!label || label === '拍照打卡')) {
    return '我的考勤'
  }
  return label
}

function isNotificationPath(path = '') {
  return /\/notifications(?:$|\/)/.test(path)
}

function normalizeItems(items = [], pathKey) {
  const seenPaths = new Set()
  return items
    .map((item) => {
      const rawPath = item?.[pathKey] || (pathKey === 'mobilePath' ? mapPathToSurface(item?.path || '', 'mobile') : item?.path)
      const path = normalizeMenuPath(rawPath)
      if (!item || !path) {
        return null
      }
      if (isNotificationPath(path)) {
        return null
      }
      if (seenPaths.has(path)) {
        return null
      }
      seenPaths.add(path)
      return {
        ...item,
        path,
        label: normalizeMenuLabel(item.label, rawPath),
        icon: MENU_ICON_MAP[item.icon] || DataBoard
      }
    })
    .filter(Boolean)
}

const STUDENT_EXAM_ITEM = { path: '/student/exam-center', label: '笔试中心', icon: 'EditPen' }
const STUDENT_AI_INTERVIEW_ITEM = { path: '/student/ai-interview', label: 'AI 智能面试', icon: 'ChatDotRound' }
const TEACHER_EXAM_HUB = { path: '/teacher/exam-hub', label: '笔试中心', icon: 'EditPen' }
const TEACHER_AI_RECORDS = { path: '/teacher/ai-interview-records', label: 'AI 面试记录', icon: 'ChatDotRound' }
const ADMIN_EXAM_HUB = { path: '/admin/exam-hub', label: '笔试中心', icon: 'EditPen' }
const ADMIN_AI_RECORDS = { path: '/admin/ai-interview-records', label: 'AI 面试记录', icon: 'ChatDotRound' }
const ADMIN_STATISTICS = { path: '/admin/statistics', label: '统计分析', icon: 'TrendCharts' }

export function resolveDesktopMenuItems(items = [], fallbackItems = []) {
  const normalizedItems = normalizeItems(items, 'path')
  const result = normalizedItems.length ? normalizedItems : normalizeItems(fallbackItems, 'path')
  // 确保学生端始终包含笔试中心
  const isStudentMenu = result.some(i => i.path?.startsWith('/student/'))
  if (isStudentMenu) {
    if (!result.some(i => i.path === STUDENT_EXAM_ITEM.path)) {
      const attendanceIdx = result.findIndex(i => i.path === '/student/attendance')
      const insertIdx = attendanceIdx >= 0 ? attendanceIdx + 1 : result.length
      result.splice(insertIdx, 0, { ...STUDENT_EXAM_ITEM, icon: MENU_ICON_MAP.EditPen })
    }
    if (!result.some(i => i.path === STUDENT_AI_INTERVIEW_ITEM.path)) {
      const examIdx = result.findIndex(i => i.path === '/student/exam-center')
      const insertIdx = examIdx >= 0 ? examIdx + 1 : result.length
      result.splice(insertIdx, 0, { ...STUDENT_AI_INTERVIEW_ITEM, icon: MENU_ICON_MAP.ChatDotRound })
    }
  }
  const isAdminMenu = result.some(i => i.path?.startsWith('/admin/'))
  if (isAdminMenu) {
    if (!result.some(i => i.path === ADMIN_EXAM_HUB.path)) {
      const attendanceIdx = result.findIndex(i => i.path === '/admin/attendance-tasks')
      const insertIdx = attendanceIdx >= 0 ? attendanceIdx + 1 : result.length
      result.splice(insertIdx, 0, { ...ADMIN_EXAM_HUB, icon: MENU_ICON_MAP.EditPen })
    }
    if (!result.some(i => i.path === ADMIN_AI_RECORDS.path)) {
      const examIdx = result.findIndex(i => i.path === '/admin/exam-hub')
      const insertIdx = examIdx >= 0 ? examIdx + 1 : result.length
      result.splice(insertIdx, 0, { ...ADMIN_AI_RECORDS, icon: MENU_ICON_MAP.ChatDotRound })
    }
    if (!result.some(i => i.path === ADMIN_STATISTICS.path)) {
      const workspaceIdx = result.findIndex(i => i.path === '/admin/workspace')
      const insertIdx = workspaceIdx >= 0 ? workspaceIdx + 1 : result.length
      result.splice(insertIdx, 0, { ...ADMIN_STATISTICS, icon: MENU_ICON_MAP.TrendCharts })
    }
  }
  // 确保教师端包含笔试中心（单入口）
  const isTeacherMenu = result.some(i => i.path?.startsWith('/teacher/'))
  if (isTeacherMenu && !result.some(i => i.path === TEACHER_EXAM_HUB.path)) {
    result.push({ ...TEACHER_EXAM_HUB, icon: MENU_ICON_MAP.EditPen })
  }
  if (isTeacherMenu && !result.some(i => i.path === TEACHER_AI_RECORDS.path)) {
    result.push({ ...TEACHER_AI_RECORDS, icon: MENU_ICON_MAP.ChatDotRound })
  }
  return result
}

export function resolveMobileMenuItems(items = [], fallbackItems = []) {
  const normalizedItems = normalizeItems(items, 'mobilePath')
  const result = normalizedItems.length ? normalizedItems : normalizeItems(fallbackItems, 'mobilePath')
  const isStudentMenu = result.some((item) => item.path?.startsWith('/m/student/'))
  if (isStudentMenu && !result.some((item) => item.path === '/m/student/exam-center')) {
    const attendanceIdx = result.findIndex((item) => item.path === '/m/student/attendance')
    const insertIdx = attendanceIdx >= 0 ? attendanceIdx + 1 : result.length
    result.splice(insertIdx, 0, {
      path: '/m/student/exam-center',
      label: '笔试中心',
      icon: EditPen
    })
  }
  if (isStudentMenu && !result.some((item) => item.path === '/m/student/ai-interview')) {
    const examIdx = result.findIndex((item) => item.path === '/m/student/exam-center')
    const insertIdx = examIdx >= 0 ? examIdx + 1 : result.length
    result.splice(insertIdx, 0, {
      path: '/m/student/ai-interview',
      label: 'AI 智能面试',
      icon: ChatDotRound
    })
  }
  const isAdminMenu = result.some((item) => item.path?.startsWith('/m/admin/'))
  if (isAdminMenu && !result.some((item) => item.path === '/m/admin/statistics')) {
    const insertIdx = result.findIndex((item) => item.path === '/m/admin/profile')
    result.splice(insertIdx >= 0 ? insertIdx : result.length, 0, {
      path: '/m/admin/statistics',
      label: '统计分析',
      icon: TrendCharts
    })
  }
  const isTeacherMenu = result.some((item) => item.path?.startsWith('/m/teacher/'))
  if (isTeacherMenu && !result.some((item) => item.path === '/m/teacher/ai-interview-records')) {
    const insertIdx = result.findIndex((item) => item.path === '/m/teacher/notices')
    result.splice(insertIdx >= 0 ? insertIdx : result.length, 0, {
      path: '/m/teacher/ai-interview-records',
      label: 'AI 面试记录',
      icon: ChatDotRound
    })
  }
  return result
}

const GROUP_ORDER = {
  student: ['工作区', '实验室相关', '考核与招聘', '资料与通知', '个人中心'],
  admin: ['工作区', '实验室相关', '考核与招聘', '资料与通知', '系统管理', '个人中心'],
  teacher: ['工作区', '实验室相关', '考核与招聘', '资料与通知', '个人中心']
}

function inferMenuRole(items = []) {
  if (items.some((item) => item.path?.startsWith('/admin/'))) return 'admin'
  if (items.some((item) => item.path?.startsWith('/teacher/'))) return 'teacher'
  return 'student'
}

function inferGroupTitle(item, role) {
  const path = item?.path || ''
  if (/\/dashboard$/.test(path)) return '工作区'
  if (/\/profile$/.test(path)) return '个人中心'
  if (/\/notice|\/workspace|\/space|\/profiles|\/audit/.test(path)) return path.includes('/audit') ? '系统管理' : '资料与通知'
  if (/\/attendance|\/exam|\/grading|\/question-bank|\/paper-compose|\/ai-interview/.test(path)) return '考核与招聘'
  if (role === 'admin' && /\/search|\/colleges|\/devices|\/statistics/.test(path)) return '系统管理'
  if (/\/labs|\/lab-info|\/applications|\/members|\/create-applies|\/teacher-register-applies|\/my-lab/.test(path)) return '实验室相关'
  return '实验室相关'
}

export function resolveDesktopMenuGroups(items = [], portalRole) {
  const role = portalRole || inferMenuRole(items)
  const buckets = new Map()
  for (const item of items) {
    const title = inferGroupTitle(item, role)
    if (!buckets.has(title)) {
      buckets.set(title, [])
    }
    buckets.get(title).push(item)
  }
  const order = GROUP_ORDER[role] || GROUP_ORDER.student
  const grouped = order
    .map((title) => ({ title, items: buckets.get(title) || [] }))
    .filter((group) => group.items.length)

  for (const [title, groupItems] of buckets.entries()) {
    if (!order.includes(title) && groupItems.length) {
      grouped.push({ title, items: groupItems })
    }
  }
  return grouped
}
