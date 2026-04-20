import request from '@/utils/request'

export function getGraduateList(params) {
  return request({
    url: '/api/graduate/list',
    method: 'get',
    params
  })
}

export function addGraduate(data) {
  return request({
    url: '/api/graduate/add',
    method: 'post',
    data
  })
}

export function updateGraduate(data) {
  return request({
    url: '/api/graduate/update',
    method: 'put',
    data
  })
}

export function deleteGraduate(id) {
  return request({
    url: `/api/graduate/${id}`,
    method: 'delete'
  })
}
