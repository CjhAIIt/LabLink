import request from '@/utils/request'

export function getLabSpaceOverview(params) {
  return request({
    url: '/api/lab-space/overview',
    method: 'get',
    params
  })
}

export function getDailyAttendance(params) {
  return request({
    url: '/api/lab-space/attendance/daily',
    method: 'get',
    params
  })
}

export function confirmAttendance(data) {
  return request({
    url: '/api/lab-space/attendance/confirm',
    method: 'post',
    data
  })
}

export function getMyAttendance(params) {
  return request({
    url: '/api/lab-space/attendance/my',
    method: 'get',
    params
  })
}

export function getAttendanceSummary(params) {
  return request({
    url: '/api/lab-space/attendance/summary',
    method: 'get',
    params
  })
}

export function signInAttendance(data) {
  return request({
    url: '/api/lab-space/attendance/sign-in',
    method: 'post',
    data
  })
}

export function submitExitApplication(data) {
  return request({
    url: '/api/lab-space/exit-application',
    method: 'post',
    data
  })
}

export function getMyExitApplications(params) {
  return request({
    url: '/api/lab-space/exit-application/my',
    method: 'get',
    params
  })
}

export function getLabExitApplications(params) {
  return request({
    url: '/api/lab-space/exit-application/list',
    method: 'get',
    params
  })
}

export function auditExitApplication(data) {
  return request({
    url: '/api/lab-space/exit-application/audit',
    method: 'post',
    data
  })
}

export function getSpaceFolders(params) {
  return request({
    url: '/api/lab-space/folders',
    method: 'get',
    params
  })
}

export function createSpaceFolder(data) {
  return request({
    url: '/api/lab-space/folders',
    method: 'post',
    data
  })
}

export function updateSpaceFolder(id, data) {
  return request({
    url: `/api/lab-space/folders/${id}`,
    method: 'put',
    data
  })
}

export function getSpaceFiles(params) {
  return request({
    url: '/api/lab-space/files',
    method: 'get',
    params
  })
}

export function getRecentSpaceFiles(params) {
  return request({
    url: '/api/lab-space/files/recent',
    method: 'get',
    params
  })
}

export function uploadSpaceFile(formData) {
  return request({
    url: '/api/lab-space/files/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function updateSpaceFileArchive(id, data) {
  return request({
    url: `/api/lab-space/files/${id}/archive`,
    method: 'post',
    data
  })
}
