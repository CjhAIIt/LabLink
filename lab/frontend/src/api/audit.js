import request from '@/utils/request'

export function getAuditLogPage(params) {
  return request({
    url: '/api/audit/logs',
    method: 'get',
    params
  })
}
