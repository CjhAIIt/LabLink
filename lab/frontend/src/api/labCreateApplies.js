import request from '@/utils/request'

export function getLabCreateApplyPage(params) {
  return request({
    url: '/api/lab-create-applies',
    method: 'get',
    params
  })
}

export function createLabCreateApply(data) {
  return request({
    url: '/api/lab-create-applies',
    method: 'post',
    data
  })
}

export function auditLabCreateApply(id, data) {
  return request({
    url: `/api/lab-create-applies/${id}/audit`,
    method: 'post',
    data
  })
}
