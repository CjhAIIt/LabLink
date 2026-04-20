import { getToken } from '@/utils/auth'
import { Capacitor } from '@capacitor/core'

const ABSOLUTE_URL_PATTERN = /^https?:\/\//i
const PRIVATE_HOST_PATTERN = /^(localhost|127(?:\.\d{1,3}){3}|0(?:\.0){3})$/i
const CLOUD_API_ORIGIN = 'http://101.35.79.76'
const FILE_ID_PATTERN = /^file-id:(\d+)$/i

function isNativePlatform() {
  try {
    return Boolean(Capacitor?.isNativePlatform?.())
  } catch (error) {
    return false
  }
}

function resolveApiBaseUrl() {
  const configuredBase = import.meta.env.VITE_API_BASE_URL
  if (configuredBase) {
    return configuredBase.replace(/\/$/, '')
  }

  if (isNativePlatform()) {
    return CLOUD_API_ORIGIN
  }

  return ''
}

function getBackendOrigin() {
  const configuredBase = resolveApiBaseUrl()

  if (ABSOLUTE_URL_PATTERN.test(configuredBase)) {
    return configuredBase.replace(/\/api\/?$/i, '').replace(/\/$/, '')
  }

  return window.location.origin
}

export function resolveFileUrl(rawUrl) {
  if (!rawUrl) {
    return ''
  }

  const fileId = resolveManagedFileId(rawUrl)
  if (fileId) {
    return buildManagedFileViewUrl(fileId)
  }

  if (ABSOLUTE_URL_PATTERN.test(rawUrl)) {
    return rawUrl
  }

  if (rawUrl.startsWith('protected:')) {
    return buildSecuredFileViewUrl(rawUrl)
  }

  const normalizedPath = rawUrl.startsWith('/') ? rawUrl : `/${rawUrl}`
  if (normalizedPath.startsWith('/uploads/')) {
    return buildSecuredFileViewUrl(normalizedPath)
  }
  return `${getBackendOrigin()}${normalizedPath}`
}

function resolveManagedFileId(rawUrl) {
  const normalized = String(rawUrl || '').trim()
  const match = normalized.match(FILE_ID_PATTERN)
  return match ? Number(match[1]) : null
}

function buildManagedFileViewUrl(fileId) {
  const params = new URLSearchParams()
  const token = getToken()
  if (token) {
    params.set('token', token)
  }
  const suffix = params.toString() ? `?${params.toString()}` : ''
  return `${getBackendOrigin()}/api/files/${fileId}/preview${suffix}`
}

function buildSecuredFileViewUrl(path) {
  const params = new URLSearchParams({ path })
  const token = getToken()
  if (token) {
    params.set('token', token)
  }
  return `${getBackendOrigin()}/api/file/view?${params.toString()}`
}

export function getFileNameFromUrl(rawUrl, fallback = 'attachment') {
  if (!rawUrl) {
    return fallback
  }

  const fileId = resolveManagedFileId(rawUrl)
  if (fileId) {
    return `file-${fileId}`
  }

  const lastSegment = rawUrl.split('/').pop()
  if (!lastSegment) {
    return fallback
  }

  try {
    return decodeURIComponent(lastSegment)
  } catch (error) {
    return lastSegment
  }
}

export function getFileExtension(rawUrl) {
  if (!rawUrl) {
    return ''
  }

  if (resolveManagedFileId(rawUrl)) {
    return ''
  }

  const cleanUrl = rawUrl.split('?')[0].split('#')[0]
  const lastSegment = cleanUrl.split('/').pop() || ''
  const lastDot = lastSegment.lastIndexOf('.')
  return lastDot >= 0 ? lastSegment.slice(lastDot).toLowerCase() : ''
}

export function getFilePreviewType(rawUrl) {
  const extension = getFileExtension(rawUrl)

  if (['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp'].includes(extension)) {
    return 'image'
  }

  if (extension === '.pdf') {
    return 'pdf'
  }

  if (['.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx'].includes(extension)) {
    return 'office'
  }

  return 'download'
}

export function buildOfficePreviewUrl(rawUrl) {
  const absoluteUrl = resolveFileUrl(rawUrl)

  try {
    const url = new URL(absoluteUrl)
    if (url.searchParams.has('token')) {
      return ''
    }
    if (PRIVATE_HOST_PATTERN.test(url.hostname)) {
      return ''
    }
    return `https://view.officeapps.live.com/op/view.aspx?src=${encodeURIComponent(url.toString())}`
  } catch (error) {
    return ''
  }
}

export function buildAttachmentList(rawUrls) {
  if (!rawUrls) {
    return []
  }

  return rawUrls
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => ({
      name: getFileNameFromUrl(item),
      url: resolveFileUrl(item),
      previewType: getFilePreviewType(item),
      rawUrl: item
    }))
}
