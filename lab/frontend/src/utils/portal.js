const ADMIN_ROLES = new Set(['admin', 'super_admin'])
const PORTAL_SURFACE_KEY = 'lab_portal_surface'

export const PORTAL_SURFACES = {
  DESKTOP: 'desktop',
  MOBILE: 'mobile'
}

function normalizeSurface(surface) {
  return surface === PORTAL_SURFACES.MOBILE ? PORTAL_SURFACES.MOBILE : PORTAL_SURFACES.DESKTOP
}

export function isMobilePortal() {
  if (typeof window === 'undefined') {
    return false
  }

  try {
    const isNative = Boolean(window?.Capacitor?.isNativePlatform?.())
    if (isNative) {
      return true
    }
  } catch (error) {
    // ignore runtime detection failures
  }

  return window.matchMedia?.('(max-width: 768px)')?.matches ?? window.innerWidth <= 768
}

export function getPortalSurface() {
  if (typeof window === 'undefined') {
    return PORTAL_SURFACES.DESKTOP
  }

  const storedSurface = localStorage.getItem(PORTAL_SURFACE_KEY)
  if (storedSurface === PORTAL_SURFACES.MOBILE || storedSurface === PORTAL_SURFACES.DESKTOP) {
    return storedSurface
  }

  return isMobilePortal() ? PORTAL_SURFACES.MOBILE : PORTAL_SURFACES.DESKTOP
}

export function setPortalSurface(surface) {
  if (typeof window === 'undefined') {
    return PORTAL_SURFACES.DESKTOP
  }

  const normalizedSurface = normalizeSurface(surface)
  localStorage.setItem(PORTAL_SURFACE_KEY, normalizedSurface)
  return normalizedSurface
}

export function clearPortalSurface() {
  if (typeof window === 'undefined') {
    return
  }
  localStorage.removeItem(PORTAL_SURFACE_KEY)
}

export function resolvePortalSurface(options = {}) {
  if (typeof options === 'string') {
    return normalizeSurface(options)
  }

  if (options?.surface) {
    return normalizeSurface(options.surface)
  }

  return getPortalSurface()
}

export function resolveRouteSurface(path = '') {
  return path.startsWith('/m') ? PORTAL_SURFACES.MOBILE : PORTAL_SURFACES.DESKTOP
}

export function resolvePublicEntry(options = {}) {
  return resolvePortalSurface(options) === PORTAL_SURFACES.MOBILE ? '/m/login' : '/login'
}

export function resolvePortalLogin(options = {}) {
  return resolvePublicEntry(options)
}

export function resolvePortalRole(userInfo) {
  if (!userInfo) {
    return ''
  }

  if (ADMIN_ROLES.has(userInfo.role)) {
    return 'admin'
  }

  if (userInfo.role === 'teacher') {
    return 'teacher'
  }

  if (userInfo.primaryIdentity === 'teacher') {
    return 'teacher'
  }

  return 'student'
}

export function resolvePortalHome(userInfoOrRole, options = {}) {
  const portalRole =
    typeof userInfoOrRole === 'string' ? userInfoOrRole : resolvePortalRole(userInfoOrRole)
  const surface = resolvePortalSurface(options)
  const isMobileSurface = surface === PORTAL_SURFACES.MOBILE

  if (portalRole === 'admin') {
    return isMobileSurface ? '/m/admin/dashboard' : '/admin/dashboard'
  }
  if (portalRole === 'teacher') {
    return isMobileSurface ? '/m/teacher/dashboard' : '/teacher/dashboard'
  }
  return isMobileSurface ? '/m/student/dashboard' : '/student/dashboard'
}

export function resolveSurfacePathByRoute(routePath = '', desktopPath = '') {
  if (!desktopPath || typeof desktopPath !== 'string') {
    return desktopPath
  }

  const surface = routePath.startsWith('/m/') ? PORTAL_SURFACES.MOBILE : PORTAL_SURFACES.DESKTOP
  return mapPathToSurface(desktopPath, surface)
}

export function mapPathToSurface(path, surface = getPortalSurface()) {
  if (!path || typeof path !== 'string') {
    return path
  }

  const targetSurface = resolvePortalSurface(surface)

  if (targetSurface === PORTAL_SURFACES.MOBILE) {
    if (path.startsWith('/m/')) {
      return path
    }
    if (path.startsWith('/student')) {
      return path.replace(/^\/student/, '/m/student')
    }
    if (path.startsWith('/teacher')) {
      return path.replace(/^\/teacher/, '/m/teacher')
    }
    if (path.startsWith('/admin/labs')) {
      return '/m/admin/labs'
    }
    if (path.startsWith('/admin/applications')) {
      return '/m/admin/applications'
    }
    if (path.startsWith('/admin/create-applies')) {
      return '/m/admin/create-applies'
    }
    if (path.startsWith('/admin/teacher-register-applies')) {
      return '/m/admin/teacher-register-applies'
    }
    if (path.startsWith('/admin/attendance-tasks')) {
      return '/m/admin/attendance'
    }
    if (path.startsWith('/admin/members')) {
      return '/m/admin/members'
    }
    if (path.startsWith('/admin/devices')) {
      return '/m/admin/devices'
    }
    if (path.startsWith('/admin/profiles')) {
      return '/m/admin/profiles'
    }
    if (path.startsWith('/admin/notices')) {
      return '/m/admin/notices-manage'
    }
    if (path.startsWith('/admin/statistics')) {
      return '/m/admin/statistics'
    }
    if (path.startsWith('/admin/ai-interview-records')) {
      return path.replace(/^\/admin/, '/m/admin')
    }
    if (path.startsWith('/admin/profile')) {
      return '/m/admin/profile'
    }
    if (path.startsWith('/admin/notifications')) {
      return '/m/admin/notifications'
    }
    if (path.startsWith('/admin')) {
      return '/m/admin/dashboard'
    }
    return path
  }

  if (path.startsWith('/m/student')) {
    return path.replace(/^\/m\/student/, '/student')
  }
  if (path.startsWith('/m/teacher')) {
    return path.replace(/^\/m\/teacher/, '/teacher')
  }
  if (path.startsWith('/m/admin/labs')) {
    return '/admin/labs'
  }
  if (path.startsWith('/m/admin/applications')) {
    return '/admin/applications'
  }
  if (path.startsWith('/m/admin/create-applies')) {
    return '/admin/create-applies'
  }
  if (path.startsWith('/m/admin/teacher-register-applies')) {
    return '/admin/teacher-register-applies'
  }
  if (path.startsWith('/m/admin/attendance')) {
    return '/admin/attendance-tasks'
  }
  if (path.startsWith('/m/admin/members')) {
    return '/admin/members'
  }
  if (path.startsWith('/m/admin/devices')) {
    return '/admin/devices'
  }
  if (path.startsWith('/m/admin/profiles')) {
    return '/admin/profiles'
  }
  if (path.startsWith('/m/admin/notices-manage')) {
    return '/admin/notices'
  }
  if (path.startsWith('/m/admin/statistics')) {
    return '/admin/statistics'
  }
  if (path.startsWith('/m/admin/ai-interview-records')) {
    return path.replace(/^\/m\/admin/, '/admin')
  }
  if (path.startsWith('/m/admin/notices')) {
    return '/admin/notices'
  }
  if (path.startsWith('/m/admin/profile')) {
    return '/admin/profile'
  }
  if (path.startsWith('/m/admin/notifications')) {
    return '/admin/notifications'
  }
  if (path.startsWith('/m/admin')) {
    return '/admin/dashboard'
  }
  return path
}
