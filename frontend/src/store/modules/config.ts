import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DangerBehavior, AlertThreshold } from '@/types/alert'
import { dangerBehaviorApi, thresholdApi } from '@/api/config'

export const useConfigStore = defineStore('config', () => {
  const dangerBehaviors = ref<DangerBehavior[]>([])
  const thresholds = ref<AlertThreshold[]>([])
  const loading = ref(false)

  // 获取危险行为列表
  async function fetchDangerBehaviors() {
    loading.value = true
    try {
      const response = await dangerBehaviorApi.getAll()
      dangerBehaviors.value = response.data
    } catch (error) {
      console.error('Failed to fetch danger behaviors:', error)
    } finally {
      loading.value = false
    }
  }

  // 获取阈值列表
  async function fetchThresholds() {
    loading.value = true
    try {
      const response = await thresholdApi.getAll()
      thresholds.value = response.data
    } catch (error) {
      console.error('Failed to fetch thresholds:', error)
    } finally {
      loading.value = false
    }
  }

  // 创建危险行为
  async function createDangerBehavior(data: Partial<DangerBehavior>) {
    try {
      const response = await dangerBehaviorApi.create(data)
      dangerBehaviors.value.push(response.data)
      return response.data
    } catch (error) {
      console.error('Failed to create danger behavior:', error)
      throw error
    }
  }

  // 更新危险行为
  async function updateDangerBehavior(id: number, data: Partial<DangerBehavior>) {
    try {
      const response = await dangerBehaviorApi.update(id, data)
      const index = dangerBehaviors.value.findIndex(b => b.id === id)
      if (index !== -1) {
        dangerBehaviors.value[index] = response.data
      }
      return response.data
    } catch (error) {
      console.error('Failed to update danger behavior:', error)
      throw error
    }
  }

  // 删除危险行为
  async function deleteDangerBehavior(id: number) {
    try {
      await dangerBehaviorApi.delete(id)
      dangerBehaviors.value = dangerBehaviors.value.filter(b => b.id !== id)
    } catch (error) {
      console.error('Failed to delete danger behavior:', error)
      throw error
    }
  }

  // 更新阈值
  async function updateThreshold(id: number, data: Partial<AlertThreshold>) {
    try {
      const response = await thresholdApi.update(id, data)
      const index = thresholds.value.findIndex(t => t.id === id)
      if (index !== -1) {
        thresholds.value[index] = response.data
      }
      return response.data
    } catch (error) {
      console.error('Failed to update threshold:', error)
      throw error
    }
  }

  // 根据行为ID获取危险行为名称
  function getBehaviorName(id: number): string {
    return dangerBehaviors.value.find(b => b.id === id)?.name || 'Unknown'
  }

  // 根据行为ID获取颜色
  function getBehaviorColor(id: number): string {
    return dangerBehaviors.value.find(b => b.id === id)?.colorCode || '#999999'
  }

  return {
    dangerBehaviors,
    thresholds,
    loading,
    fetchDangerBehaviors,
    fetchThresholds,
    createDangerBehavior,
    updateDangerBehavior,
    deleteDangerBehavior,
    updateThreshold,
    getBehaviorName,
    getBehaviorColor
  }
})
