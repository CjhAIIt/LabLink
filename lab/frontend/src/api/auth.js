import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

export function register(data) {
  return request({
    url: '/api/auth/register',
    method: 'post',
    data
  })
}

export function sendRegisterCode(data) {
  return request({
    url: '/api/auth/register/send-code',
    method: 'post',
    data
  })
}

export function sendTeacherRegisterCode(data) {
  return request({
    url: '/api/auth/teacher-register/send-code',
    method: 'post',
    data
  })
}

export function registerTeacher(data) {
  return request({
    url: '/api/auth/teacher-register',
    method: 'post',
    data
  })
}

export function sendPasswordResetCode(data) {
  return request({
    url: '/api/auth/password-reset/send-code',
    method: 'post',
    data
  })
}

export function resetPassword(data) {
  return request({
    url: '/api/auth/password-reset/confirm',
    method: 'post',
    data
  })
}

export function getUserInfo() {
  return request({
    url: '/api/auth/me',
    method: 'get'
  })
}

export function getAuthMenus() {
  return request({
    url: '/api/auth/menus',
    method: 'get'
  })
}

export function getAuthPermissions() {
  return request({
    url: '/api/auth/permissions',
    method: 'get'
  })
}

export function logout() {
  return request({
    url: '/api/user/logout',
    method: 'post'
  })
}
