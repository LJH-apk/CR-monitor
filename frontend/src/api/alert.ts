import apiClient from './axios'
import type { Alert, DashboardStats } from '@/types/alert'

export const alertApi = {
  getAll() {
    return apiClient.get<Alert[]>('/alerts')
  },

  getByVideo(videoId: number) {
    return apiClient.get<Alert[]>(`/alerts/video/${videoId}`)
  },

  getUnacknowledged() {
    return apiClient.get<Alert[]>('/alerts/unacknowledged')
  },

  getRecent(hours: number = 24) {
    return apiClient.get<Alert[]>(`/alerts/recent?hours=${hours}`)
  },

  acknowledge(id: number) {
    return apiClient.put<Alert>(`/alerts/${id}/acknowledge`)
  },

  delete(id: number) {
    return apiClient.delete(`/alerts/${id}`)
  }
}

export const dashboardApi = {
  getStats(hours: number = 24) {
    return apiClient.get<DashboardStats>(`/dashboard/stats?hours=${hours}`)
  }
}
