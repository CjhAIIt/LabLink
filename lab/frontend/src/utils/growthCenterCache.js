import axios from 'axios'
import { getToken, getUserInfo } from '@/utils/auth'

const STORAGE_PREFIX = 'growth_dashboard_cache'
const CACHE_TTL_MS = 60 * 1000
const REQUEST_TIMEOUT_MS = 8000

let cachedDashboardEntry = null
let cachedDashboardKey = ''
let inflightDashboardPromise = null

const getStorageKey = () => {
  const userInfo = getUserInfo()
  const scope = userInfo?.id || userInfo?.username || 'anonymous'
  return `${STORAGE_PREFIX}:${scope}`
}

const readStoredEntry = (storageKey) => {
  if (typeof window === 'undefined' || !window.sessionStorage) {
    return null
  }

  try {
    const raw = window.sessionStorage.getItem(storageKey)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed.fetchedAt !== 'number' || typeof parsed.data !== 'object') {
      return null
    }
    return parsed
  } catch (error) {
    return null
  }
}

const writeStoredEntry = (storageKey, entry) => {
  if (typeof window === 'undefined' || !window.sessionStorage) {
    return
  }

  try {
    window.sessionStorage.setItem(storageKey, JSON.stringify(entry))
  } catch (error) {
    // ignore storage write failures
  }
}

const removeStoredEntry = (storageKey) => {
  if (typeof window === 'undefined' || !window.sessionStorage) {
    return
  }

  try {
    window.sessionStorage.removeItem(storageKey)
  } catch (error) {
    // ignore storage delete failures
  }
}

const resolveCacheEntry = () => {
  const storageKey = getStorageKey()
  if (cachedDashboardEntry && cachedDashboardKey === storageKey) {
    return cachedDashboardEntry
  }

  cachedDashboardKey = storageKey
  cachedDashboardEntry = readStoredEntry(storageKey)
  return cachedDashboardEntry
}

const isFreshEntry = (entry, maxAge) => {
  if (!entry) {
    return false
  }
  return Date.now() - entry.fetchedAt <= maxAge
}

const saveCacheEntry = (dashboardData) => {
  const storageKey = getStorageKey()
  const entry = {
    fetchedAt: Date.now(),
    data: dashboardData || {}
  }

  cachedDashboardKey = storageKey
  cachedDashboardEntry = entry
  writeStoredEntry(storageKey, entry)
  return entry.data
}

const fetchGrowthDashboard = async () => {
  const token = getToken()
  if (!token) {
    throw new Error('未登录')
  }

  const response = await axios.get(`${import.meta.env.VITE_API_BASE_URL || ''}/api/growth-center/dashboard`, {
    headers: {
      Authorization: `Bearer ${token}`
    },
    timeout: REQUEST_TIMEOUT_MS
  })

  if (response?.data?.code !== 200) {
    throw new Error(response?.data?.message || '成长中心加载失败')
  }

  return response.data?.data || {}
}

export function getGrowthDashboardSnapshot(options = {}) {
  const { allowExpired = true, maxAge = CACHE_TTL_MS } = options
  const entry = resolveCacheEntry()
  if (!entry) {
    return null
  }

  if (!allowExpired && !isFreshEntry(entry, maxAge)) {
    return null
  }

  return entry.data
}

export function clearGrowthDashboardCache() {
  const storageKey = getStorageKey()
  cachedDashboardEntry = null
  cachedDashboardKey = storageKey
  inflightDashboardPromise = null
  removeStoredEntry(storageKey)
}

export async function loadGrowthDashboard(options = {}) {
  const {
    force = false,
    maxAge = CACHE_TTL_MS,
    allowStaleOnError = true
  } = options

  if (!force) {
    const freshSnapshot = getGrowthDashboardSnapshot({ allowExpired: false, maxAge })
    if (freshSnapshot) {
      return freshSnapshot
    }
  }

  if (inflightDashboardPromise) {
    return inflightDashboardPromise
  }

  const staleSnapshot = allowStaleOnError
    ? getGrowthDashboardSnapshot({ allowExpired: true, maxAge })
    : null

  inflightDashboardPromise = fetchGrowthDashboard()
    .then((dashboardData) => saveCacheEntry(dashboardData))
    .catch((error) => {
      if (staleSnapshot) {
        return staleSnapshot
      }
      throw error
    })
    .finally(() => {
      inflightDashboardPromise = null
    })

  return inflightDashboardPromise
}
