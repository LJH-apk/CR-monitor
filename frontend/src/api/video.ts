import apiClient from './axios'
import type { Video, VideoUploadResponse } from '@/types/video'

export interface DetectionSummary {
  totalAlerts: number
  isAnalyzing: boolean
  progress: number
  hasAlerts: boolean
  status: 'normal' | 'alert' | 'analyzing'
  message: string
}

export const videoApi = {
  upload(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post<VideoUploadResponse>('/videos/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  uploadWithProgress(
    file: File,
    onProgress: (progress: number) => void,
    abortController?: AbortController
  ) {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post<VideoUploadResponse>('/videos/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      signal: abortController?.signal,
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      }
    })
  },

  getList() {
    return apiClient.get<Video[]>('/videos')
  },

  getById(id: number) {
    return apiClient.get<Video>(`/videos/${id}`)
  },

  delete(id: number) {
    return apiClient.delete(`/videos/${id}`)
  },

  getAnalysisProgress(id: number) {
    return apiClient.get<{ progress: number; status: string; analyzing: boolean }>(`/videos/${id}/analysis-progress`)
  },

  getDetectionSummary(id: number) {
    return apiClient.get<DetectionSummary>(`/videos/${id}/detection-summary`)
  }
}
