import request from '@/utils/request'

export function getMyNotifications(params) {
  return request({
    url: '/api/notifications/my',
    method: 'get',
    params
  })
}

export function getUnreadNotificationCount() {
  return request({
    url: '/api/notifications/unread-count',
    method: 'get'
  })
}

export function markNotificationRead(notificationId) {
  return request({
    url: `/api/notifications/read/${notificationId}`,
    method: 'post'
  })
}

export function markAllNotificationsRead() {
  return request({
    url: '/api/notifications/read-all',
    method: 'post'
  })
}
