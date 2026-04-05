import request from '@/utils/request'

export function getLabPage(params) {
  return request({
    url: '/api/labs/list',
    method: 'get',
    params
  })
}

export function getLabStats() {
  return request({
    url: '/api/labs/stats',
    method: 'get'
  })
}

export function getLabById(id) {
  return request({
    url: `/api/labs/${id}`,
    method: 'get'
  })
}

export function createLab(data) {
  return request({
    url: '/api/labs',
    method: 'post',
    data
  })
}

export function updateLab(id, data) {
  return request({
    url: `/api/labs/${id}`,
    method: 'put',
    data
  })
}

export function updateManagedLab(data) {
  return request({
    url: '/api/labs/update-info',
    method: 'put',
    data
  })
}

export function deleteLab(id) {
  return request({
    url: `/api/labs/${id}`,
    method: 'delete'
  })
}
