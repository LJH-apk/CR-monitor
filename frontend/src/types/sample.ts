export interface TrainingSample {
  id: number
  userId: number
  username?: string
  originalFilename: string
  storedFilename: string
  filePath: string
  fileSize: number
  imageWidth: number
  imageHeight: number
  status: 'PENDING' | 'ANNOTATED' | 'APPROVED' | 'REJECTED'
  uploadTime: string
  createdAt: string
  updatedAt: string
  annotations?: Annotation[]
}

export interface Annotation {
  id?: number
  sampleId: number
  dangerBehaviorId: number
  dangerBehaviorName?: string
  xMin: number
  yMin: number
  xMax: number
  yMax: number
  notes?: string
  createdAt?: string
  updatedAt?: string
}

export interface SampleUploadResponse {
  sampleId: number
  filename: string
  status: string
  message: string
  imageWidth: number
  imageHeight: number
}

export interface CocoExport {
  images: CocoImage[]
  annotations: CocoAnnotation[]
  categories: CocoCategory[]
}

export interface CocoImage {
  id: number
  fileName: string
  width: number
  height: number
}

export interface CocoAnnotation {
  id: number
  imageId: number
  categoryId: number
  bbox: number[]  // [x, y, width, height]
  area: number
}

export interface CocoCategory {
  id: number
  name: string
}
