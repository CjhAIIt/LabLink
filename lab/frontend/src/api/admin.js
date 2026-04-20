import request from '@/utils/request'

// 获取所有管理员
export function getAllAdmins() {
  return request({
    url: '/api/admin/list',
    method: 'get'
  })
}

// 添加管理员
export function addAdmin(data) {
  return request({
    url: '/api/user/admin/add',
    method: 'post',
    data
  })
}

// 更新管理员信息
export function updateAdmin(data) {
  return request({
    url: `/api/user/admin/${data.id}`,
    method: 'put',
    data
  })
}

// 删除管理员
export function deleteAdmin(id) {
  return request({
    url: `/api/admin/users/${id}`,
    method: 'delete'
  })
}

// 获取学生列表
export function getStudentList() {
  return request({
    url: '/api/user/student/list',
    method: 'get'
  })
}

export function getAdminStudentPage(params) {
  return request({
    url: '/api/admin/users',
    method: 'get',
    params
  })
}
