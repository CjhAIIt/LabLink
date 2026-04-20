import request from '@/utils/request'

export function getLabMemberPage(params) {
  return request({
    url: '/api/lab-members',
    method: 'get',
    params
  })
}

export function getActiveLabMembers(params) {
  return request({
    url: '/api/lab-members/active',
    method: 'get',
    params
  })
}

export function appointLeader(id) {
  return request({
    url: `/api/lab-members/${id}/appoint-leader`,
    method: 'post'
  })
}

export function removeLabMember(id, params) {
  return request({
    url: `/api/lab-members/${id}/remove`,
    method: 'post',
    params
  })
}
