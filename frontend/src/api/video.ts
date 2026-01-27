import apiClient from './axios'
import type { Video, VideoUploadResponse } from '@/types/video'

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
  }
}
