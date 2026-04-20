import request from '@/utils/request'

export function getLabPage(params) {
  const rawParams = {
    ...(params || {}),
    _t: Date.now()
  }
  const cleanedParams = Object.fromEntries(
    Object.entries(rawParams).filter(([, value]) => value !== undefined && value !== null && value !== '')
  )
  return request({
    url: '/api/labs/list',
    method: 'get',
    params: cleanedParams
  })
}

export function getLabStats() {
  return request({
    url: '/api/labs/stats',
    method: 'get',
    params: {
      _t: Date.now()
    }
  })
}

export function getLabById(id) {
  return request({
    url: `/api/labs/${id}`,
    method: 'get',
    params: {
      _t: Date.now()
    }
  })
}

export function getAllLabs(params = {}) {
  return getLabPage({
    pageNum: 1,
    pageSize: 200,
    ...params
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
