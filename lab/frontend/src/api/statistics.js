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

function buildParams(params = {}) {
  const next = {}
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      next[key] = value
    }
  })
  return next
}

export function getStatisticsDashboard(params) {
  return request({
    url: '/api/statistics/dashboard',
    method: 'get',
    params: buildParams(params)
  })
}

export function getStatisticsLabs(params) {
  return request({
    url: '/api/statistics/labs',
    method: 'get',
    params: buildParams(params)
  })
}

export function getStatisticsMembers(params) {
  return request({
    url: '/api/statistics/members',
    method: 'get',
    params: buildParams(params)
  })
}

export function getStatisticsAttendance(params) {
  return request({
    url: '/api/statistics/attendance',
    method: 'get',
    params: buildParams(params)
  })
}

export function getStatisticsDevices(params) {
  return request({
    url: '/api/statistics/devices',
    method: 'get',
    params: buildParams(params)
  })
}

export function getStatisticsProfiles(params) {
  return request({
    url: '/api/statistics/profiles',
    method: 'get',
    params: buildParams(params)
  })
}
