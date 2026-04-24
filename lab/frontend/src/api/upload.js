import request from '@/utils/request'

export function uploadFileWithScene(file, scene = 'image') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('scene', scene)

  return request({
    url: '/api/files/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
