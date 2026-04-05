import request from '@/utils/request'

export function getTeacherRegisterApplyPage(params) {
  return request({
    url: '/api/teacher-register-applies',
    method: 'get',
    params
  })
}

export function auditTeacherRegisterApply(id, data) {
  return request({
    url: `/api/teacher-register-applies/${id}/audit`,
    method: 'post',
    data
  })
}
