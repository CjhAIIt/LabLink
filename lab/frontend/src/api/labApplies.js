import request from '@/utils/request'

export function createLabApply(data) {
  return request({
    url: '/api/lab-applies',
    method: 'post',
    data
  })
}

export function getLabApplyPage(params) {
  return request({
    url: '/api/lab-applies',
    method: 'get',
    params
  })
}

export function getMyLabApplyPage(params) {
  return request({
    url: '/api/lab-applies/my',
    method: 'get',
    params
  })
}

export function auditLabApply(id, data) {
  return request({
    url: `/api/lab-applies/${id}/audit`,
    method: 'post',
    data
  })
}

export function getLatestLabApplies(params) {
  return request({
    url: '/api/lab-applies/latest',
    method: 'get',
    params
  })
}
