import { createStore } from 'vuex'
import request from '@/utils/request'

export default createStore({
  state: {
    user: null,
    token: localStorage.getItem('token') || '',
    role: localStorage.getItem('userRole') || ''
  },
  
  getters: {
    isAuthenticated: state => !!state.token,
    userRole: state => state.role,
    currentUser: state => state.user
  },
  
  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    
    SET_USER(state, user) {
      state.user = user
    },
    
    SET_ROLE(state, role) {
      state.role = role
      localStorage.setItem('userRole', role)
    },
    
    CLEAR_AUTH(state) {
      state.token = ''
      state.user = null
      state.role = ''
      localStorage.removeItem('token')
      localStorage.removeItem('userRole')
    }
  },
  
  actions: {
    // 学生登录
    async studentLogin({ commit }, credentials) {
      try {
        const response = await request.post('/api/auth/student/login', credentials)
        const data = response.data
        
        if (data.token) {
          commit('SET_TOKEN', data.token)
          commit('SET_ROLE', 'USER')
          commit('SET_USER', {
            id: data.id,
            username: data.username,
            email: data.email,
            realName: data.realName,
            studentId: data.studentId,
            major: data.major
          })
          return { success: true }
        }
        return { success: false, message: '登录失败' }
      } catch (error) {
        return { 
          success: false, 
          message: error.response?.data?.message || '登录失败' 
        }
      }
    },
    
    // 管理员登录
    async adminLogin({ commit }, credentials) {
      try {
        const response = await request.post('/api/auth/admin/login', credentials)
        const data = response.data
        
        if (data.token) {
          commit('SET_TOKEN', data.token)
          commit('SET_ROLE', 'ADMIN')
          commit('SET_USER', {
            id: data.id,
            username: data.username,
            email: data.email,
            realName: data.realName
          })
          return { success: true }
        }
        return { success: false, message: '登录失败' }
      } catch (error) {
        return { 
          success: false, 
          message: error.response?.data?.message || '登录失败' 
        }
      }
    },
    
    // 学生注册
    async studentRegister(_, userData) {
      try {
        const response = await request.post('/api/auth/student/register', userData)
        return { success: true, data: response.data }
      } catch (error) {
        return { 
          success: false, 
          message: error.response?.data?.message || '注册失败' 
        }
      }
    },
    
    // 登出
    logout({ commit }) {
      commit('CLEAR_AUTH')
    },
    
    // 获取用户信息
    async fetchUserInfo({ commit, state }) {
      if (!state.token) return
      
      try {
        const endpoint = state.role === 'ADMIN' 
          ? '/api/auth/admin/info' 
          : '/api/auth/student/info'
          
        const response = await request.get(endpoint)
        const data = response.data
        
        commit('SET_USER', data)
        return { success: true }
      } catch (error) {
        // 如果获取用户信息失败，可能是token过期，清除认证信息
        commit('CLEAR_AUTH')
        return { 
          success: false, 
          message: error.response?.data?.message || '获取用户信息失败' 
        }
      }
    }
  }
})