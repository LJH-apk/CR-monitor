import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { User, LoginRequest } from '@/types/user'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const isAuthenticated = ref(false)

  // 初始化：从localStorage恢复状态
  function init() {
    const savedToken = localStorage.getItem('token')
    const savedUser = localStorage.getItem('user')

    if (savedToken && savedUser) {
      token.value = savedToken
      user.value = JSON.parse(savedUser)
      isAuthenticated.value = true
    }
  }

  // 登录
  async function login(credentials: LoginRequest) {
    try {
      const response = await authApi.login(credentials)
      const data = response.data

      token.value = data.token
      user.value = {
        id: data.userId,
        username: data.username,
        role: data.role
      }
      isAuthenticated.value = true

      // 保存到localStorage
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(user.value))

      return true
    } catch (error) {
      console.error('Login failed:', error)
      return false
    }
  }

  // 登出
  async function logout() {
    try {
      await authApi.logout()
    } catch (error) {
      console.error('Logout failed:', error)
    } finally {
      token.value = null
      user.value = null
      isAuthenticated.value = false
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }

  // 检查是否是管理员（包括超级管理员和开发者）
  function isAdmin() {
    return ['ADMIN', 'SUPER_ADMIN', 'DEVELOPER'].includes(user.value?.role || '')
  }

  // 检查是否是超级管理员
  function isSuperAdmin() {
    return user.value?.role === 'SUPER_ADMIN'
  }

  // 检查是否是开发者
  function isDeveloper() {
    return user.value?.role === 'DEVELOPER'
  }

  // 检查是否是管理员或以上级别
  function isAdminOrAbove() {
    return ['ADMIN', 'SUPER_ADMIN', 'DEVELOPER'].includes(user.value?.role || '')
  }

  return {
    user,
    token,
    isAuthenticated,
    init,
    login,
    logout,
    isAdmin,
    isSuperAdmin,
    isDeveloper,
    isAdminOrAbove
  }
})
