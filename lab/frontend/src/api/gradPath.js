import request from '@/utils/request'

export function getGradPathConfig() {
  return request({
    url: '/api/gradpath/config',
    method: 'get'
  })
}

export function getGradPathQuestions(params) {
  return request({
    url: '/api/gradpath/questions',
    method: 'get',
    params
  })
}

export function getGradPathQuestionDetail(questionId) {
  return request({
    url: `/api/gradpath/questions/${questionId}`,
    method: 'get'
  })
}

export function generateGradPathQuestion(keyword, trackCode) {
  return request({
    url: '/api/gradpath/questions/generate',
    method: 'post',
    params: { keyword, trackCode }
  })
}

export function debugGradPathCode(data) {
  return request({
    url: '/api/gradpath/judge/debug',
    method: 'post',
    data
  })
}

export function submitGradPathCode(data) {
  return request({
    url: '/api/gradpath/judge/submit',
    method: 'post',
    data
  })
}

export function analyzeGradPathCode(data) {
  return request({
    url: '/api/gradpath/judge/analyze',
    method: 'post',
    data
  })
}

export function completeGradPathExam(data) {
  return request({
    url: '/api/gradpath/exam/complete',
    method: 'post',
    data
  })
}
