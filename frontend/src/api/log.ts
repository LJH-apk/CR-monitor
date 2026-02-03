import apiClient from './axios'

export interface SystemLog {
  id: number
  level: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'
  type: 'LOGIN' | 'LOGOUT' | 'UPLOAD' | 'DELETE' | 'USER_MGMT' | 'CONFIG' | 'ERROR' | 'OPERATION' | 'SYSTEM'
  message: string
  details?: string
  userId?: number
  username?: string
  ipAddress?: string
  userAgent?: string
  location?: string
  loginSuccess?: boolean
  requestUri?: string
  requestMethod?: string
  createdAt: string
}

export interface LogStats {
  levelCounts: Record<string, number>
  loginSuccessCount: number
  loginFailCount: number
  logoutCount: number
  totalCount: number
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface LogQueryParams {
  level?: string
  type?: string
  userId?: number
  startTime?: string
  endTime?: string
  keyword?: string
  page?: number
  size?: number
  sortBy?: string
  sortDir?: string
}

export const logApi = {
  getLogs(params: LogQueryParams) {
    return apiClient.get<PageResponse<SystemLog>>('/developer/logs', { params })
  },

  getStats(hours: number = 24) {
    return apiClient.get<LogStats>('/developer/logs/stats', { params: { hours } })
  }
}
