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

// ==================== 笔试中心（学生端扩展） ====================

export function checkExamEligibility(examId) {
  return request({
    url: `/api/written-exam/student/eligibility/${examId}`,
    method: 'get'
  })
}

export function getExamDetail(examId) {
  return request({
    url: `/api/written-exam/student/detail/${examId}`,
    method: 'get'
  })
}

export function submitExamSignature(data) {
  return request({
    url: '/api/written-exam/student/signature',
    method: 'post',
    data
  })
}

export function startExam(examId) {
  return request({
    url: `/api/written-exam/student/start/${examId}`,
    method: 'post'
  })
}

export function saveExamAnswer(data) {
  return request({
    url: '/api/written-exam/student/save-answer',
    method: 'post',
    data
  })
}

export function submitExamPaper(data) {
  return request({
    url: '/api/written-exam/student/submit-paper',
    method: 'post',
    data
  })
}

export function getExamProgress(examId) {
  return request({
    url: `/api/written-exam/student/progress/${examId}`,
    method: 'get'
  })
}

export function reportCheatEvent(data) {
  return request({
    url: '/api/written-exam/student/cheat-report',
    method: 'post',
    data
  })
}

// ==================== 管理员端 - 笔试管理 ====================

export function getAdminExamList(params) {
  return request({ url: '/api/written-exam/admin/exams', method: 'get', params })
}

export function getAdminExamDetail(examId) {
  return request({ url: `/api/written-exam/admin/exams/${examId}`, method: 'get' })
}

export function createAdminExam(data) {
  return request({ url: '/api/written-exam/admin/exams', method: 'post', data })
}

export function updateAdminExam(examId, data) {
  return request({ url: `/api/written-exam/admin/exams/${examId}`, method: 'put', data })
}

export function deleteAdminExam(examId) {
  return request({ url: `/api/written-exam/admin/exams/${examId}`, method: 'delete' })
}

export function publishAdminExam(examId) {
  return request({ url: `/api/written-exam/admin/exams/${examId}/publish`, method: 'post' })
}

// ==================== 管理员端 - 题库管理 ====================

export function getAdminQuestionList(params) {
  return request({ url: '/api/written-exam/admin/questions', method: 'get', params })
}

export function getAdminQuestionDetail(questionId) {
  return request({ url: `/api/written-exam/admin/questions/${questionId}`, method: 'get' })
}

export function createAdminQuestion(data) {
  return request({ url: '/api/written-exam/admin/questions', method: 'post', data })
}

export function updateAdminQuestion(questionId, data) {
  return request({ url: `/api/written-exam/admin/questions/${questionId}`, method: 'put', data })
}

export function deleteAdminQuestion(questionId) {
  return request({ url: `/api/written-exam/admin/questions/${questionId}`, method: 'delete' })
}

// ==================== 管理员端 - 组卷 ====================

export function getAdminPaperQuestions(examId) {
  return request({ url: `/api/written-exam/admin/exams/${examId}/paper`, method: 'get' })
}

export function saveAdminPaperQuestions(examId, data) {
  return request({ url: `/api/written-exam/admin/exams/${examId}/paper`, method: 'post', data })
}

// ==================== 管理员端 - 阅卷 ====================

export function getAdminGradingList(params) {
  return request({ url: '/api/written-exam/admin/grading', method: 'get', params })
}

export function getAdminStudentAnswer(attemptId) {
  return request({ url: `/api/written-exam/admin/grading/${attemptId}`, method: 'get' })
}

export function submitAdminGrading(attemptId, data) {
  return request({ url: `/api/written-exam/admin/grading/${attemptId}`, method: 'post', data })
}

export function publishAdminScores(examId) {
  return request({ url: `/api/written-exam/admin/exams/${examId}/publish-scores`, method: 'post' })
}

export function sendInterviewInvitations(data) {
  return request({ url: '/api/written-exam/admin/interview-invitations', method: 'post', data })
}

// ==================== 管理员端 - 统计 ====================

export function getAdminExamStatistics(examId) {
  return request({ url: `/api/written-exam/admin/exams/${examId}/statistics`, method: 'get' })
}

export function getAdminExamCheatLogs(params) {
  return request({ url: '/api/written-exam/admin/cheat-logs', method: 'get', params })
}
