import {
  Bell,
  Calendar,
  Camera,
  ChatDotRound,
  DataBoard,
  EditPen,
  Files,
  FolderOpened,
  Monitor,
  OfficeBuilding,
  Search,
  Tickets,
  TrendCharts,
  User,
  UserFilled
} from '@element-plus/icons-vue'

const MENU_ICON_MAP = {
  Bell,
  Calendar,
  Camera,
  ChatDotRound,
  DataBoard,
  EditPen,
  Files,
  FolderOpened,
  Monitor,
  OfficeBuilding,
  Search,
  Tickets,
  TrendCharts,
  User,
  UserFilled
}

function normalizeItems(items = [], pathKey) {
  return items
    .map((item) => {
      const path = item?.[pathKey]
      if (!item || !path) {
        return null
      }
      return {
        ...item,
        path,
        icon: MENU_ICON_MAP[item.icon] || DataBoard
      }
    })
    .filter(Boolean)
}

const STUDENT_CHECKIN_ITEM = { path: '/student/check-in', label: '拍照打卡', icon: 'Camera' }
const STUDENT_EXAM_ITEM = { path: '/student/exam-center', label: '笔试中心', icon: 'EditPen' }
const STUDENT_AI_INTERVIEW_ITEM = { path: '/student/ai-interview', label: 'AI 智能面试', icon: 'ChatDotRound' }
const ADMIN_EXAM_HUB = { path: '/admin/exam-hub', label: '笔试中心', icon: 'EditPen' }
const ADMIN_AI_MODULES = { path: '/admin/ai-interview-modules', label: 'AI 面试模块', icon: 'ChatDotRound' }
const ADMIN_AI_RECORDS = { path: '/admin/ai-interview-records', label: 'AI 面试记录', icon: 'DataBoard' }
const TEACHER_EXAM_HUB = { path: '/teacher/exam-hub', label: '笔试中心', icon: 'EditPen' }

export function resolveDesktopMenuItems(items = [], fallbackItems = []) {
  const normalizedItems = normalizeItems(items, 'path')
  const result = normalizedItems.length ? normalizedItems : normalizeItems(fallbackItems, 'path')
  // 确保学生端始终包含拍照打卡和笔试中心
  const isStudentMenu = result.some(i => i.path?.startsWith('/student/'))
  if (isStudentMenu) {
    if (!result.some(i => i.path === STUDENT_CHECKIN_ITEM.path)) {
      const attendanceIdx = result.findIndex(i => i.path === '/student/attendance')
      const insertIdx = attendanceIdx >= 0 ? attendanceIdx + 1 : result.length
      result.splice(insertIdx, 0, { ...STUDENT_CHECKIN_ITEM, icon: MENU_ICON_MAP.Camera })
    }
    if (!result.some(i => i.path === STUDENT_EXAM_ITEM.path)) {
      const checkInIdx = result.findIndex(i => i.path === '/student/check-in')
      const insertIdx = checkInIdx >= 0 ? checkInIdx + 1 : result.length
      result.splice(insertIdx, 0, { ...STUDENT_EXAM_ITEM, icon: MENU_ICON_MAP.EditPen })
    }
    if (!result.some(i => i.path === STUDENT_AI_INTERVIEW_ITEM.path)) {
      const examIdx = result.findIndex(i => i.path === '/student/exam-center')
      const insertIdx = examIdx >= 0 ? examIdx + 1 : result.length
      result.splice(insertIdx, 0, { ...STUDENT_AI_INTERVIEW_ITEM, icon: MENU_ICON_MAP.ChatDotRound })
    }
  }
  // 确保管理员端包含笔试中心（单入口）
  const isAdminMenu = result.some(i => i.path?.startsWith('/admin/'))
  if (isAdminMenu && !result.some(i => i.path === ADMIN_EXAM_HUB.path)) {
    result.push({ ...ADMIN_EXAM_HUB, icon: MENU_ICON_MAP.EditPen })
  }
  if (isAdminMenu && !result.some(i => i.path === ADMIN_AI_MODULES.path)) {
    result.push({ ...ADMIN_AI_MODULES, icon: MENU_ICON_MAP.ChatDotRound })
  }
  if (isAdminMenu && !result.some(i => i.path === ADMIN_AI_RECORDS.path)) {
    result.push({ ...ADMIN_AI_RECORDS, icon: MENU_ICON_MAP.DataBoard })
  }
  // 确保教师端包含笔试中心（单入口）
  const isTeacherMenu = result.some(i => i.path?.startsWith('/teacher/'))
  if (isTeacherMenu && !result.some(i => i.path === TEACHER_EXAM_HUB.path)) {
    result.push({ ...TEACHER_EXAM_HUB, icon: MENU_ICON_MAP.EditPen })
  }
  return result
}

export function resolveMobileMenuItems(items = [], fallbackItems = []) {
  const normalizedItems = normalizeItems(items, 'mobilePath')
  const result = normalizedItems.length ? normalizedItems : normalizeItems(fallbackItems, 'mobilePath')
  const mobileCheckIn = { mobilePath: '/m/student/check-in', path: '/m/student/check-in', label: '拍照打卡', icon: 'Camera' }
  const isStudentMenu = result.some(i => (i.mobilePath || i.path)?.includes('/student/'))
  if (isStudentMenu && !result.some(i => (i.mobilePath || i.path)?.includes('/student/check-in'))) {
    const attendanceIdx = result.findIndex(i => (i.mobilePath || i.path)?.includes('/student/attendance'))
    const insertIdx = attendanceIdx >= 0 ? attendanceIdx + 1 : result.length
    result.splice(insertIdx, 0, { ...mobileCheckIn, icon: MENU_ICON_MAP.Camera })
  }
  return result
}
