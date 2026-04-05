import request from '@/utils/request'

export function getOverviewStatistics() {
  return request({
    url: '/api/statistics/overview',
    method: 'get'
  })
}

export function getLabStatistics(labId) {
  return request({
    url: `/api/statistics/lab/${labId}`,
    method: 'get'
  })
}
