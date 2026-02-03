import axios from 'axios'
import apiClient from './axios'

// AI 服务独立的 axios 实例（直接连接 AI 服务，用于健康检查等）
const aiClient = axios.create({
  baseURL: 'http://localhost:5001/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 添加JWT token
aiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

export interface ModelVersion {
  name: string
  description: string
  created_at: string
  model_file: string
  metrics: {
    mAP50?: number
    'mAP50-95'?: number
    precision?: number
    recall?: number
  }
  training_samples: number
  is_active: boolean
}

export interface ModelVersionsResponse {
  code: number
  message: string
  data: {
    current_version: string
    versions: Record<string, ModelVersion>
  }
}

export interface TrainingStatus {
  is_training: boolean
  progress: number
  status: string
  task_id: string | null
}

export interface TrainingHistory {
  task_id: string
  version: string
  samples: number
  epochs: number
  metrics: Record<string, number>
  auto_swapped: boolean
  timestamp: string
}

export const aiApi = {
  // 健康检查 - 直接调用 AI 服务
  health() {
    return aiClient.get('/health')
  },

  // 获取所有模型版本 - 通过后端代理
  getModelVersions() {
    return apiClient.get<ModelVersionsResponse>('/admin/models/versions')
  },

  // 获取当前模型信息 - 通过后端代理
  getCurrentModel() {
    return apiClient.get('/admin/models/current')
  },

  // 热替换模型 - 通过后端代理（记录日志）
  swapModel(version: string) {
    return apiClient.post('/admin/models/swap', { version })
  },

  // 回滚模型 - 通过后端代理（记录日志）
  rollbackModel(version?: string) {
    return apiClient.post('/admin/models/rollback', { version })
  },

  // 开始训练 - 通过后端代理（记录日志）
  startTraining(params: {
    epochs?: number
    batch_size?: number
    auto_swap?: boolean
  }) {
    return apiClient.post('/admin/models/training/start', params)
  },

  // 获取训练状态 - 通过后端代理
  getTrainingStatus() {
    return apiClient.get<{ code: number; data: TrainingStatus }>('/admin/models/training/status')
  },

  // 取消训练 - 通过后端代理（记录日志）
  cancelTraining() {
    return apiClient.post('/admin/models/training/cancel')
  },

  // 获取训练历史 - 通过后端代理
  getTrainingHistory() {
    return apiClient.get<{ code: number; data: TrainingHistory[] }>('/admin/models/training/history')
  }
}
