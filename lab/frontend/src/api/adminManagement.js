import request from '@/utils/request'

// API版本: 2025-12-23-01 - 修复API路径缺少/api前缀的问题

// 为实验室指定管理员
export function assignAdminToLab(labId, userId) {
  return request({
    url: '/api/admin-management/assign',
    method: 'post',
    data: {
      labId,
      userId
    }
  })
}

// 移除实验室管理员
export function removeAdminFromLab(labId) {
  return request({
    url: `/api/admin-management/remove/${labId}`,
    method: 'delete'
  })
}

// 获取实验室管理员
export function getLabAdmin(labId) {
  return request({
    url: `/api/admin-management/lab/${labId}`,
    method: 'get'
  })
}

// 获取所有管理员及其所属实验室
export function getAllAdminsWithLabs() {
  return request({
    url: '/api/admin-management/all',
    method: 'get'
  })
}

// 检查用户是否可以被设置为管理员
export function canUserBeAdmin(userId) {
  return request({
    url: `/api/admin-management/can-be-admin/${userId}`,
    method: 'get'
  })
}

// 获取实验室列表（包含管理员信息）
export function getLabsWithAdmin() {
  return request({
    url: '/api/labs/list-with-admin',
    method: 'get'
  })
}
