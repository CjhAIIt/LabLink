import { defineStore } from 'pinia'
import {
  clearAuth,
  getToken,
  getUserInfo,
  setToken as setAuthToken,
  setUserInfo as setAuthUserInfo
} from '@/utils/auth'
import { resolvePortalRole } from '@/utils/portal'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: getUserInfo(),
    token: getToken()
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    userRole: (state) => state.userInfo?.role,
    userPortalRole: (state) => resolvePortalRole(state.userInfo),
    userName: (state) => state.userInfo?.username,
    realName: (state) => state.userInfo?.realName
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

    setToken(token) {
      this.token = token
      setAuthToken(token)
    },

    clearUserInfo() {
      this.userInfo = null
      this.token = null
      clearAuth()
      localStorage.removeItem('userRole')
      localStorage.removeItem('userPortalRole')
    }
  }
})
