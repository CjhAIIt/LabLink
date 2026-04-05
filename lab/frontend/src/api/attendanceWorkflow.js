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
