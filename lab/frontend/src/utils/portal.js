const ADMIN_ROLES = new Set(['admin', 'super_admin'])

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
    return '/teacher/dashboard'
  }
  return '/student/dashboard'
}
