const ABSOLUTE_URL_PATTERN = /^https?:\/\//i
const PRIVATE_HOST_PATTERN = /^(localhost|127(?:\.\d{1,3}){3}|0(?:\.0){3})$/i

function getBackendOrigin() {
  const configuredBase = import.meta.env.VITE_API_BASE_URL || ''

  if (ABSOLUTE_URL_PATTERN.test(configuredBase)) {
    return configuredBase.replace(/\/api\/?$/i, '').replace(/\/$/, '')
  }

  return window.location.origin
}

export function resolveFileUrl(rawUrl) {
  if (!rawUrl) {
    return ''
  }

  if (ABSOLUTE_URL_PATTERN.test(rawUrl)) {
    return rawUrl
  }

  const normalizedPath = rawUrl.startsWith('/') ? rawUrl : `/${rawUrl}`
  if (normalizedPath.startsWith('/uploads/')) {
    return `${getBackendOrigin()}/api/file/view?path=${encodeURIComponent(normalizedPath)}`
  }
  return `${getBackendOrigin()}${normalizedPath}`
}

export function getFileNameFromUrl(rawUrl, fallback = 'attachment') {
  if (!rawUrl) {
    return fallback
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
