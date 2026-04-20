import request from '@/utils/request'

export function createAttendanceSession(data) {
  return request({
    url: '/api/attendance/session/create',
    method: 'post',
    data
  })
}

export function getActiveAttendanceSession(params) {
  return request({
    url: '/api/attendance/session/active',
    method: 'get',
    params
  })
}

export function getAttendanceSessionRecords(params) {
  return request({
    url: '/api/attendance/session/records',
    method: 'get',
    params
  })
}

export function expireAttendanceSession(data) {
  return request({
    url: '/api/attendance/session/expire',
    method: 'post',
    data
  })
}

export function finalizeAttendanceSession(data) {
  return request({
    url: '/api/attendance/session/finalize',
    method: 'post',
    data
  })
}

export function signAttendanceSession(data) {
  return request({
    url: '/api/attendance/session/sign',
    method: 'post',
    data
  })
}

export function signOutAttendance(data) {
  return request({
    url: '/api/attendance/session/sign-out',
    method: 'post',
    data
  })
}

export function photoCheckIn(data) {
  return request({
    url: '/api/attendance/check-in/photo',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getCheckInRecords(params) {
  return request({
    url: '/api/attendance/check-in/records',
    method: 'get',
    params
  })
}

export function getAttendanceManageList(params) {
  return request({
    url: '/api/attendance/manage/list',
    method: 'get',
    params
  })
}

export function tagAttendanceManage(data) {
  return request({
    url: '/api/attendance/manage/tag',
    method: 'post',
    data
  })
}

export function getAttendanceManageStat(params) {
  return request({
    url: '/api/attendance/manage/stat',
    method: 'get',
    params
  })
}

export function exportAttendanceManage(params) {
  return request({
    url: '/api/attendance/manage/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

// ==================== 今日出勤看板 ====================

export function getTodayDashboard(params) {
  return request({
    url: '/api/attendance/dashboard/today',
    method: 'get',
    params
  })
}

// ==================== 出勤统计报表 ====================

export function getAttendanceStatsSummary(params) {
  return request({
    url: '/api/attendance/stats/summary',
    method: 'get',
    params
  })
}

export function getAttendanceStatsBatch(params) {
  return request({
    url: '/api/attendance/stats/batch',
    method: 'get',
    params
  })
}

export function exportAttendanceStats(params) {
  return request({
    url: '/api/attendance/stats/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

// ==================== 异常出勤处理 ====================

export function getAttendanceAnomalyList(params) {
  return request({
    url: '/api/attendance/anomaly/list',
    method: 'get',
    params
  })
}

export function handleAttendanceAnomaly(data) {
  return request({
    url: '/api/attendance/anomaly/handle',
    method: 'post',
    data
  })
}

// ==================== 请假批量审批 ====================

export function batchApproveLeave(data) {
  return request({
    url: '/api/attendance-workflow/lab/leaves/batch-approve',
    method: 'post',
    data
  })
}
