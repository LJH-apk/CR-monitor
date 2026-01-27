import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Alert } from '@/types/alert'
import { alertApi } from '@/api/alert'

export const useAlertStore = defineStore('alert', () => {
  const alerts = ref<Alert[]>([])
  const realtimeAlerts = ref<Alert[]>([])
  const unacknowledgedCount = ref(0)
  const loading = ref(false)

  // 获取所有预警
  async function fetchAlerts() {
    loading.value = true
    try {
      const response = await alertApi.getAll()
      alerts.value = response.data
      updateUnacknowledgedCount()
    } catch (error) {
      console.error('Failed to fetch alerts:', error)
    } finally {
      loading.value = false
    }
  }

  // 获取未确认的预警
  async function fetchUnacknowledged() {
    try {
      const response = await alertApi.getUnacknowledged()
      return response.data
    } catch (error) {
      console.error('Failed to fetch unacknowledged alerts:', error)
      return []
    }
  }

  // 获取最近的预警
  async function fetchRecent(hours: number = 24) {
    try {
      const response = await alertApi.getRecent(hours)
      alerts.value = response.data
      updateUnacknowledgedCount()
    } catch (error) {
      console.error('Failed to fetch recent alerts:', error)
    }
  }

  // 获取特定视频的预警
  async function fetchByVideo(videoId: number) {
    try {
      const response = await alertApi.getByVideo(videoId)
      // 将视频的预警添加到realtimeAlerts中
      realtimeAlerts.value = response.data
      return response.data
    } catch (error) {
      console.error('Failed to fetch video alerts:', error)
      return []
    }
  }

  // 确认预警
  async function acknowledgeAlert(id: number) {
    try {
      await alertApi.acknowledge(id)
      const alert = alerts.value.find(a => a.id === id)
      if (alert) {
        alert.isAcknowledged = true
      }
      updateUnacknowledgedCount()
    } catch (error) {
      console.error('Failed to acknowledge alert:', error)
      throw error
    }
  }

  // 添加实时预警（从WebSocket接收）
  function addRealtimeAlert(alert: Alert) {
    // 检查是否已存在（避免重复）
    const existsInRealtime = realtimeAlerts.value.some(a => a.id === alert.id)
    const existsInAlerts = alerts.value.some(a => a.id === alert.id)

    if (!existsInRealtime) {
      realtimeAlerts.value.unshift(alert)
      // 限制实时预警列表长度
      if (realtimeAlerts.value.length > 50) {
        realtimeAlerts.value = realtimeAlerts.value.slice(0, 50)
      }
    }

    if (!existsInAlerts) {
      alerts.value.unshift(alert)
    }

    updateUnacknowledgedCount()
  }

  // 更新未确认数量
  function updateUnacknowledgedCount() {
    unacknowledgedCount.value = alerts.value.filter(a => !a.isAcknowledged).length
  }

  // 清空实时预警
  function clearRealtimeAlerts() {
    realtimeAlerts.value = []
  }

  return {
    alerts,
    realtimeAlerts,
    unacknowledgedCount,
    loading,
    fetchAlerts,
    fetchUnacknowledged,
    fetchRecent,
    fetchByVideo,
    acknowledgeAlert,
    addRealtimeAlert,
    clearRealtimeAlerts
  }
})
