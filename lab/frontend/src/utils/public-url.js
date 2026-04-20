import { Capacitor } from '@capacitor/core'

const ABSOLUTE_URL_PATTERN = /^https?:\/\//i
const CLOUD_API_ORIGIN = 'http://101.35.79.76'

function isNativePlatform() {
  try {
    return Boolean(Capacitor?.isNativePlatform?.())
  } catch (error) {
    return false
  }
}

function normalizeAbsoluteOrigin(rawValue) {
  if (!rawValue || !ABSOLUTE_URL_PATTERN.test(rawValue)) {
    return ''
  }
  return rawValue.replace(/\/api\/?$/i, '').replace(/\/$/, '')
}

export function resolvePublicAppOrigin() {
  const configuredOrigin = normalizeAbsoluteOrigin(import.meta.env.VITE_PUBLIC_APP_ORIGIN || import.meta.env.VITE_APP_ORIGIN)
  if (configuredOrigin) {
    return configuredOrigin
  }

  if (isNativePlatform()) {
    return normalizeAbsoluteOrigin(import.meta.env.VITE_API_BASE_URL) || CLOUD_API_ORIGIN
  }

  return window.location.origin
}

export function buildPublicAppUrl(path = '/') {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${resolvePublicAppOrigin()}${normalizedPath}`
}
