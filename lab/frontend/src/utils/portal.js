const ADMIN_ROLES = new Set(['admin', 'super_admin'])

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
    // ignore
  }

  return window.matchMedia?.('(max-width: 768px)')?.matches ?? window.innerWidth <= 768
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

export function resolvePortalHome(userInfoOrRole) {
  const portalRole =
    typeof userInfoOrRole === 'string' ? userInfoOrRole : resolvePortalRole(userInfoOrRole)

  if (portalRole === 'admin') {
    return '/admin/dashboard'
  }
  if (portalRole === 'teacher') {
    return isMobilePortal() ? '/m/teacher/dashboard' : '/teacher/dashboard'
  }
  return isMobilePortal() ? '/m/student/dashboard' : '/student/dashboard'
}
