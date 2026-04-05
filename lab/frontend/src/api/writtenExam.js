import request from '@/utils/request'

export function getStudentLabExamPage(params) {
  return request({
    url: '/api/written-exam/student/labs',
    method: 'get',
    params
  })
}

export function getStudentExam(labId) {
  return request({
    url: `/api/written-exam/student/exam/${labId}`,
    method: 'get'
  })
}

export function getStudentExamSubmission(labId) {
  return request({
    url: `/api/written-exam/student/submission/${labId}`,
    method: 'get'
  })
}

export function submitStudentExam(data) {
  return request({
    url: '/api/written-exam/student/submit',
    method: 'post',
    data
  })
}

export function getStudentExamNotifications() {
  return request({
    url: '/api/written-exam/student/notifications',
    method: 'get'
  })
}

export function markExamNotificationRead(notificationId) {
  return request({
    url: `/api/written-exam/student/notifications/read/${notificationId}`,
    method: 'post'
  })
}

export function getAdminExamConfig() {
  return request({
    url: '/api/written-exam/admin/config',
    method: 'get'
  })
}

export function saveAdminExamConfig(data) {
  return request({
    url: '/api/written-exam/admin/config',
    method: 'post',
    data
  })
}

export function getAdminExamSubmissions(params) {
  return request({
    url: '/api/written-exam/admin/submissions',
    method: 'get',
    params
  })
}

export function reviewAdminExamSubmission(data) {
  return request({
    url: '/api/written-exam/admin/review',
    method: 'post',
    data
  })
}
