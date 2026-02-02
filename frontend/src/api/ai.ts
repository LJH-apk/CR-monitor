import axios from 'axios'

// AI 服务独立的 axios 实例（直接连接 AI 服务）
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
  // 健康检查
  health() {
    return aiClient.get('/health')
  },

  // 获取所有模型版本
  getModelVersions() {
    return aiClient.get<ModelVersionsResponse>('/models/versions')
  },

  // 获取当前模型信息
  getCurrentModel() {
    return aiClient.get('/models/current')
  },

  // 热替换模型
  swapModel(version: string) {
    return aiClient.post('/models/swap', { version })
  },

  // 回滚模型
  rollbackModel(version?: string) {
    return aiClient.post('/models/rollback', { version })
  },

  // 开始训练
  startTraining(params: {
    epochs?: number
    batch_size?: number
    auto_swap?: boolean
  }) {
    return aiClient.post('/training/start', params)
  },

  // 获取训练状态
  getTrainingStatus() {
    return aiClient.get<{ code: number; data: TrainingStatus }>('/training/status')
  },

  // 取消训练
  cancelTraining() {
    return aiClient.post('/training/cancel')
  },

  // 获取训练历史
  getTrainingHistory() {
    return aiClient.get<{ code: number; data: TrainingHistory[] }>('/training/history')
  }
}
