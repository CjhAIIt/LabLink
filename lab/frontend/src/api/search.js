import request from '@/utils/request'

export function globalSearch(params) {
  return request({
    url: '/api/search/global',
    method: 'get',
    params
  })
}

export function searchFiles(params) {
  return request({
    url: '/api/search/files',
    method: 'get',
    params
  })
}
