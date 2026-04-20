import { getAuthMenus, getAuthPermissions, getUserInfo } from '@/api/auth'

export async function ensureAuthContext(userStore, options = {}) {
  const { force = false, fallbackUserInfo = null } = options

  if (!userStore?.token) {
    return null
  }

  const shouldLoadProfile =
    force ||
    !userStore.userInfo?.id ||
    ((userStore.userInfo?.labId || userStore.userInfo?.managedLabId) && !userStore.userInfo?.labName)
  const shouldLoadMenus = force || !Array.isArray(userStore.menus) || userStore.menus.length === 0
  const shouldLoadPermissions =
    force || !Array.isArray(userStore.permissions) || userStore.permissions.length === 0

  if (!shouldLoadProfile && !shouldLoadMenus && !shouldLoadPermissions) {
    return {
      userInfo: userStore.userInfo,
      menus: userStore.menus,
      permissions: userStore.permissions
    }
  }

  const [profileRes, menusRes, permissionsRes] = await Promise.all([
    shouldLoadProfile ? getUserInfo().catch(() => null) : Promise.resolve(null),
    shouldLoadMenus ? getAuthMenus().catch(() => null) : Promise.resolve(null),
    shouldLoadPermissions ? getAuthPermissions().catch(() => null) : Promise.resolve(null)
  ])

  if (profileRes?.data) {
    userStore.setUserInfo(profileRes.data)
  } else if (fallbackUserInfo && !userStore.userInfo?.id) {
    userStore.setUserInfo(fallbackUserInfo)
  }

  if (menusRes?.data) {
    userStore.setMenus(menusRes.data)
  }

  if (permissionsRes?.data) {
    userStore.setPermissions(permissionsRes.data)
  }

  return {
    userInfo: userStore.userInfo,
    menus: userStore.menus,
    permissions: userStore.permissions
  }
}
