import request from '@/utils/request'

export function getCollegePage(params) {
  return request({
    url: '/api/colleges',
    method: 'get',
    params
  })
}

export function getCollegeOptions() {
  return request({
    url: '/api/colleges/options',
    method: 'get'
  })
}

export function createCollege(data) {
  return request({
    url: '/api/colleges',
    method: 'post',
    data
  })
}

export function updateCollege(id, data) {
  return request({
    url: `/api/colleges/${id}`,
    method: 'put',
    data
  })
}

export function deleteCollege(id) {
  return request({
    url: `/api/colleges/${id}`,
    method: 'delete'
  })
}
