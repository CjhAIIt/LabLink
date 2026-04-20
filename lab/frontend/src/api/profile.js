import request from '@/utils/request'

export function getMyStudentProfile() {
  return request.get('/api/profiles/me')
}

export function saveMyStudentProfile(data) {
  return request.put('/api/profiles/me', data)
}

export function submitMyStudentProfile() {
  return request.post('/api/profiles/me/submit')
}

export function getProfilePage(params) {
  return request.get('/api/profiles', { params })
}

export function getPendingProfileReviewPage(params) {
  return request.get('/api/profile-reviews/pending', { params })
}

export function getProfileDetail(profileId) {
  return request.get(`/api/profiles/${profileId}`)
}

export function getProfileReviews(profileId) {
  return request.get(`/api/profiles/${profileId}/reviews`)
}

export function getProfileArchives(profileId) {
  return request.get(`/api/profiles/${profileId}/archives`)
}

export function approveProfileReview(reviewId, data) {
  return request.post(`/api/profile-reviews/${reviewId}/approve`, data)
}

export function rejectProfileReview(reviewId, data) {
  return request.post(`/api/profile-reviews/${reviewId}/reject`, data)
}
