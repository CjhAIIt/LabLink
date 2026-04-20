import request from '@/utils/request'

export function getNoticePage(params) {
  return request({
    url: '/api/notices',
    method: 'get',
    params
  })
}

export function getLatestNotices(params) {
  return request({
    url: '/api/notices/latest',
    method: 'get',
    params
  })
}

export function createNotice(data) {
  return request({
    url: '/api/notices',
    method: 'post',
    data
  })
}

export function updateNotice(id, data) {
  return request({
    url: `/api/notices/${id}`,
    method: 'put',
    data
  })
}

export function deleteNotice(id) {
  return request({
    url: `/api/notices/${id}`,
    method: 'delete'
  })
}
