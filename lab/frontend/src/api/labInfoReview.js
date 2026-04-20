import request from '@/utils/request'

export function getPendingLabInfoReviewPage(params) {
  return request.get('/api/lab-info-reviews/pending', { params })
}

export function getLabInfoReviewHistory(params) {
  return request.get('/api/lab-info-reviews/history', { params })
}

export function approveLabInfoReview(reviewId, data) {
  return request.post(`/api/lab-info-reviews/${reviewId}/approve`, data)
}

export function rejectLabInfoReview(reviewId, data) {
  return request.post(`/api/lab-info-reviews/${reviewId}/reject`, data)
}
