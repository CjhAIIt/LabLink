import request from '@/utils/request'

export function bindBusinessFiles(data) {
  return request({
    url: '/api/files/relations',
    method: 'post',
    data
  })
}

export function getFileMeta(fileId) {
  return request({
    url: `/api/files/${fileId}/meta`,
    method: 'get'
  })
}
