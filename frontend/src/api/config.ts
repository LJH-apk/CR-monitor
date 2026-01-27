import apiClient from './axios'
import type { DangerBehavior, AlertThreshold } from '@/types/alert'

export const dangerBehaviorApi = {
  getAll() {
    return apiClient.get<DangerBehavior[]>('/admin/danger-behaviors')
  },

  getActive() {
    return apiClient.get<DangerBehavior[]>('/admin/danger-behaviors/active')
  },

  getById(id: number) {
    return apiClient.get<DangerBehavior>(`/admin/danger-behaviors/${id}`)
  },

  create(data: Partial<DangerBehavior>) {
    return apiClient.post<DangerBehavior>('/admin/danger-behaviors', data)
  },

  update(id: number, data: Partial<DangerBehavior>) {
    return apiClient.put<DangerBehavior>(`/admin/danger-behaviors/${id}`, data)
  },

  delete(id: number) {
    return apiClient.delete(`/admin/danger-behaviors/${id}`)
  }
}

export const thresholdApi = {
  getAll() {
    return apiClient.get<AlertThreshold[]>('/admin/thresholds')
  },

  getById(id: number) {
    return apiClient.get<AlertThreshold>(`/admin/thresholds/${id}`)
  },

  getByBehavior(behaviorId: number) {
    return apiClient.get<AlertThreshold>(`/admin/thresholds/behavior/${behaviorId}`)
  },

  create(data: Partial<AlertThreshold>) {
    return apiClient.post<AlertThreshold>('/admin/thresholds', data)
  },

  update(id: number, data: Partial<AlertThreshold>) {
    return apiClient.put<AlertThreshold>(`/admin/thresholds/${id}`, data)
  },

  delete(id: number) {
    return apiClient.delete(`/admin/thresholds/${id}`)
  }
}
