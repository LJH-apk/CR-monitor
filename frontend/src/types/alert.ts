export interface Alert {
  id: number
  videoId: number
  dangerBehaviorId: number
  timestampInVideo: number
  confidence: number
  severityLevel: number
  description: string
  frameSnapshotPath?: string
  isAcknowledged: boolean
  acknowledgedBy?: number
  acknowledgedAt?: string
  createdAt: string
}

export interface DangerBehavior {
  id: number
  name: string
  description?: string
  severityLevel: number
  colorCode: string
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface AlertThreshold {
  id: number
  dangerBehaviorId: number
  confidenceThreshold: number
  timeWindowSeconds: number
  maxAlertsPerWindow: number
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface DashboardStats {
  totalAlerts: number
  unacknowledgedAlerts: number
  totalVideos: number
  alertsByBehavior: Record<string, number>
  alertsBySeverity: Record<string, number>
  alertsByHour: Array<{
    hour: string
    count: number
  }>
}
