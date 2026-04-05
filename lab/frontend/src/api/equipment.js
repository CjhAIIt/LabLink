import request from '@/utils/request'

export function getEquipmentList(params) {
  return request({
    url: '/api/equipment/list',
    method: 'get',
    params
  })
}

export function addEquipment(data) {
  return request({
    url: '/api/equipment/add',
    method: 'post',
    data
  })
}

export function updateEquipment(data) {
  return request({
    url: '/api/equipment/update',
    method: 'put',
    data
  })
}

export function deleteEquipment(id) {
  return request({
    url: `/api/equipment/${id}`,
    method: 'delete'
  })
}

export function borrowEquipment(data) {
  return request({
    url: '/api/equipment/borrow',
    method: 'post',
    data
  })
}

export function getBorrowList(params) {
  return request({
    url: '/api/equipment/borrow/list',
    method: 'get',
    params
  })
}

export function getMyBorrowList(params) {
  return request({
    url: '/api/equipment/borrow/my',
    method: 'get',
    params
  })
}

export function auditBorrow(data) {
  return request({
    url: '/api/equipment/borrow/audit',
    method: 'post',
    data
  })
}

export function returnEquipment(data) {
  return request({
    url: '/api/equipment/borrow/return',
    method: 'post',
    data
  })
}
