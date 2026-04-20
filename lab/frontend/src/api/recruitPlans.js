import request from '@/utils/request'

export function getRecruitPlanPage(params) {
  return request({
    url: '/api/recruit-plans',
    method: 'get',
    params
  })
}

export function getActiveRecruitPlans(params) {
  return request({
    url: '/api/recruit-plans/active',
    method: 'get',
    params
  })
}

export function createRecruitPlan(data) {
  return request({
    url: '/api/recruit-plans',
    method: 'post',
    data
  })
}

export function updateRecruitPlan(id, data) {
  return request({
    url: `/api/recruit-plans/${id}`,
    method: 'put',
    data
  })
}

export function deleteRecruitPlan(id) {
  return request({
    url: `/api/recruit-plans/${id}`,
    method: 'delete'
  })
}
