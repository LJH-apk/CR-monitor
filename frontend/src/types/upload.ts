export type UploadStatus =
  | 'pending'      // 等待上传
  | 'uploading'    // 上传中
  | 'uploaded'     // 上传完成，等待转码
  | 'transcoding'  // 转码中
  | 'completed'    // 全部完成
  | 'failed'       // 失败
  | 'cancelled'    // 已取消

export interface UploadTask {
  id: string
  file: File
  filename: string
  fileSize: number
  status: UploadStatus
  progress: number
  videoId?: number
  error?: string
  abortController?: AbortController
}
