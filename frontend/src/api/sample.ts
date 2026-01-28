import apiClient from './axios'
import type { TrainingSample, Annotation, SampleUploadResponse, CocoExport } from '@/types/sample'

export const sampleApi = {
  // 上传图片样本
  upload(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post<SampleUploadResponse>('/samples/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 获取样本列表
  getAll(status?: string) {
    return apiClient.get<TrainingSample[]>('/samples', {
      params: status ? { status } : {}
    })
  },

  // 获取样本详情
  getById(id: number) {
    return apiClient.get<TrainingSample>(`/samples/${id}`)
  },

  // 保存标注数据
  saveAnnotation(sampleId: number, annotation: Annotation) {
    return apiClient.post<Annotation>(`/samples/${sampleId}/annotations`, annotation)
  },

  // 删除样本
  delete(id: number) {
    return apiClient.delete(`/samples/${id}`)
  },

  // 更新样本状态（管理员）
  updateStatus(id: number, status: string) {
    return apiClient.put<TrainingSample>(`/samples/${id}/status`, { status })
  },

  // 导出COCO JSON格式（管理员）
  exportCoco() {
    return apiClient.get<CocoExport>('/samples/export/coco')
  }
}
