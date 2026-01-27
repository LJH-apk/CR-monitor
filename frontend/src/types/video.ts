export interface Video {
  id: number
  userId: number
  originalFilename: string
  storedFilename: string
  fileSize: number
  duration?: number
  status: 'UPLOADING' | 'TRANSCODING' | 'READY' | 'FAILED'
  originalPath: string
  hlsPath?: string
  thumbnailPath?: string
  uploadTime: string
  transcodingStartedAt?: string
  transcodingCompletedAt?: string
  createdAt: string
  updatedAt: string
}

export interface VideoUploadResponse {
  videoId: number
  filename: string
  status: string
  message: string
}
