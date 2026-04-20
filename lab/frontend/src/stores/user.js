import { defineStore } from 'pinia'
import {
  clearAuth,
  getToken,
  getUserInfo,
  setToken as setAuthToken,
  setUserInfo as setAuthUserInfo
} from '@/utils/auth'
import { resolvePortalRole } from '@/utils/portal'

const USER_MENUS_KEY = 'lab_user_menus'
const USER_PERMISSIONS_KEY = 'lab_user_permissions'

function readJsonArray(key) {
  const raw = localStorage.getItem(key)
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    return []
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: getUserInfo(),
    token: getToken(),
    menus: readJsonArray(USER_MENUS_KEY),
    permissions: readJsonArray(USER_PERMISSIONS_KEY)
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    userRole: (state) => state.userInfo?.role,
    userPortalRole: (state) => resolvePortalRole(state.userInfo),
    userName: (state) => state.userInfo?.username,
    realName: (state) => state.userInfo?.realName,
    hasPermission: (state) => (permission) => state.permissions.includes(permission)
  },

  actions: {
    setUserInfo(userInfo) {
      if (!userInfo) {
        this.userInfo = null
        localStorage.removeItem('userRole')
        localStorage.removeItem('userPortalRole')
        clearAuth()
        return
      }

      const mergedUserInfo = {
        ...(this.userInfo || {}),
        ...userInfo
      }

      this.userInfo = mergedUserInfo

      if (mergedUserInfo.role) {
        localStorage.setItem('userRole', mergedUserInfo.role)
      }
      localStorage.setItem('userPortalRole', resolvePortalRole(mergedUserInfo))
      setAuthUserInfo(mergedUserInfo)
    },

    setMenus(menus) {
      const nextMenus = Array.isArray(menus) ? menus : []
      this.menus = nextMenus
      localStorage.setItem(USER_MENUS_KEY, JSON.stringify(nextMenus))
    },

    setPermissions(permissions) {
      const nextPermissions = Array.isArray(permissions) ? permissions : []
      this.permissions = nextPermissions
      localStorage.setItem(USER_PERMISSIONS_KEY, JSON.stringify(nextPermissions))
    },

    setToken(token) {
      this.token = token
      setAuthToken(token)
    },

    clearUserInfo() {
      this.userInfo = null
      this.token = null
      this.menus = []
      this.permissions = []
      clearAuth()
      localStorage.removeItem('userRole')
      localStorage.removeItem('userPortalRole')
      localStorage.removeItem(USER_MENUS_KEY)
      localStorage.removeItem(USER_PERMISSIONS_KEY)
    }
  }
})
