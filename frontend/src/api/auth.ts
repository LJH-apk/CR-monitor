import apiClient from './axios'
import type { LoginRequest, LoginResponse } from '@/types/user'

export const authApi = {
  login(data: LoginRequest) {
    return apiClient.post<LoginResponse>('/auth/login', data)
  },

  logout() {
    return apiClient.post('/auth/logout')
  }
}
