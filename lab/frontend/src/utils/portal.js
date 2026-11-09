const ADMIN_ROLES = new Set(['admin', 'super_admin'])

/**
 * 是否运行在原生壳里（桌面端 Tauri / 安卓端 Capacitor）。
 *
 * 原生壳的 WebView 没有前端的开发代理，接口地址必须是绝对地址；
 * 路由也要走 hash 模式，深链与刷新才不会 404。
 */
export function isNativeApp() {
  if (typeof window === 'undefined') {
    return false
  }

  try {
    if (window?.Capacitor?.isNativePlatform?.()) {
      return true
    }
  } catch (error) {
    // ignore
  }

  const protocol = window.location?.protocol || ''
  return protocol === 'tauri:' || protocol === 'capacitor:'
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
    return isMobilePortal() ? '/m/admin/dashboard' : '/admin/dashboard'
  }
  if (portalRole === 'teacher') {
    return isMobilePortal() ? '/m/teacher/dashboard' : '/teacher/dashboard'
  }
  return isMobilePortal() ? '/m/student/dashboard' : '/student/dashboard'
}
