import request from '@/utils/request'

export function getGrowthCenterDashboard() {
  return request({
    url: '/api/growth-center/dashboard',
    method: 'get'
  })
}

export function getGrowthAssessmentQuestions() {
  return request({
    url: '/api/growth-center/assessment/questions',
    method: 'get'
  })
}

export function submitGrowthAssessment(data) {
  return request({
    url: '/api/growth-center/assessment/submit',
    method: 'post',
    data
  })
}

export function getGrowthTracks(params) {
  return request({
    url: '/api/growth-center/tracks',
    method: 'get',
    params
  })
}

export function getGrowthTrackDetail(code) {
  return request({
    url: `/api/growth-center/tracks/${code}`,
    method: 'get'
  })
}

export function getGrowthPracticeQuestions(params) {
  return request({
    url: '/api/growth-center/practice/questions',
    method: 'get',
    params
  })
}

export function getGrowthPracticeQuestionDetail(questionId) {
  return request({
    url: `/api/growth-center/practice/questions/${questionId}`,
    method: 'get'
  })
}

export function submitGrowthPracticeAnswer(data) {
  return request({
    url: '/api/growth-center/practice/submit',
    method: 'post',
    data
  })
}

export function getAdminGrowthQuestionBank(params) {
  return request({
    url: '/api/growth-center/admin/question-bank',
    method: 'get',
    params
  })
}

export function getAdminGrowthQuestionDetail(questionId) {
  return request({
    url: `/api/growth-center/admin/question-bank/${questionId}`,
    method: 'get'
  })
}

export function saveAdminGrowthQuestion(data) {
  return request({
    url: '/api/growth-center/admin/question-bank',
    method: 'post',
    data
  })
}

export function deleteAdminGrowthQuestion(questionId) {
  return request({
    url: `/api/growth-center/admin/question-bank/delete/${questionId}`,
    method: 'post'
  })
}
