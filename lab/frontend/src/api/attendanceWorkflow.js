import request from '@/utils/request'

export function getAttendanceTaskPage(params) {
  return request({
    url: '/api/attendance-workflow/tasks',
    method: 'get',
    params
  })
}

export function saveAttendanceTask(data) {
  return request({
    url: '/api/attendance-workflow/tasks',
    method: 'post',
    data
  })
}

export function publishAttendanceTask(taskId) {
  return request({
    url: `/api/attendance-workflow/tasks/${taskId}/publish`,
    method: 'post'
  })
}

export function getAttendanceTaskSchedules(taskId) {
  return request({
    url: `/api/attendance-workflow/tasks/${taskId}/schedules`,
    method: 'get'
  })
}

export function saveAttendanceTaskSchedules(taskId, data) {
  return request({
    url: `/api/attendance-workflow/tasks/${taskId}/schedules`,
    method: 'post',
    data
  })
}

export function getAttendanceWorkflowSummary(params) {
  return request({
    url: '/api/attendance-workflow/summary',
    method: 'get',
    params
  })
}

export function getCurrentLabAttendanceSession() {
  return request({
    url: '/api/attendance-workflow/lab/session/current',
    method: 'get'
  })
}

export function getAttendanceLeavePage(params) {
  return request({
    url: '/api/attendance-workflow/lab/leaves',
    method: 'get',
    params
  })
}

export function reviewAttendanceRecord(data) {
  return request({
    url: '/api/attendance-workflow/lab/records/review',
    method: 'post',
    data
  })
}

export function uploadAttendanceSessionPhoto(data) {
  return request({
    url: '/api/attendance-workflow/lab/session/current/photo',
    method: 'post',
    data
  })
}

export function approveAttendanceLeave(leaveId, data) {
  return request({
    url: `/api/attendance-workflow/lab/leaves/${leaveId}/approve`,
    method: 'post',
    data
  })
}

export function rejectAttendanceLeave(leaveId, data) {
  return request({
    url: `/api/attendance-workflow/lab/leaves/${leaveId}/reject`,
    method: 'post',
    data
  })
}

export function getCurrentStudentAttendanceSession() {
  return request({
    url: '/api/attendance-workflow/student/session/current',
    method: 'get'
  })
}

export function signInStudentAttendance(data) {
  return request({
    url: '/api/attendance-workflow/student/session/sign-in',
    method: 'post',
    data
  })
}

export function applyAttendanceLeave(data) {
  return request({
    url: '/api/attendance-workflow/student/session/leave',
    method: 'post',
    data
  })
}

export function requestAttendanceMakeup(data) {
  return request({
    url: '/api/attendance-workflow/student/session/makeup',
    method: 'post',
    data
  })
}

export function getStudentAttendanceHistory(params) {
  return request({
    url: '/api/attendance-workflow/student/history',
    method: 'get',
    params
  })
}
