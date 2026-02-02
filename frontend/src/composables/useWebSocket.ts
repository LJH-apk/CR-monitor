import { ref, onUnmounted } from 'vue'
import type { Alert } from '@/types/alert'
import { useAlertStore } from '@/store/modules/alert'

export interface DetectionStatusMessage {
  type: 'alert' | 'normal'
  videoId: number
  analyzedFrames?: number
  message?: string
  alertData?: Alert
}

export function useWebSocket() {
  const ws = ref<WebSocket | null>(null)
  const connected = ref(false)
  const latestStatus = ref<DetectionStatusMessage | null>(null)
  const alertStore = useAlertStore()
  let currentToken = ''

  function connect(token: string) {
    if (ws.value) {
      return
    }

    currentToken = token
    const wsUrl = `ws://localhost:8080/api/ws/alerts?token=${token}`
    ws.value = new WebSocket(wsUrl)

    ws.value.onopen = () => {
      connected.value = true
      console.log('WebSocket connected')
    }

    ws.value.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)

        // 检查是否是新格式的消息（包含 type 字段）
        if (message.type) {
          const statusMessage: DetectionStatusMessage = message
          latestStatus.value = statusMessage

          if (statusMessage.type === 'alert' && statusMessage.alertData) {
            // 预警消息
            alertStore.addRealtimeAlert(statusMessage.alertData)
            console.log('Received alert:', statusMessage.alertData)
          } else if (statusMessage.type === 'normal') {
            // 无异常状态消息
            console.log('Received normal status:', statusMessage.message, 'frames:', statusMessage.analyzedFrames)
          }
        } else {
          // 兼容旧格式（直接是 Alert 对象）
          const alert: Alert = message
          alertStore.addRealtimeAlert(alert)
          latestStatus.value = {
            type: 'alert',
            videoId: alert.videoId,
            alertData: alert
          }
          console.log('Received alert (legacy format):', alert)
        }
      } catch (error) {
        console.error('Failed to parse message:', error)
      }
    }

    ws.value.onerror = (error) => {
      console.error('WebSocket error:', error)
    }

    ws.value.onclose = () => {
      connected.value = false
      console.log('WebSocket disconnected')
      ws.value = null

      // 5秒后尝试重连
      setTimeout(() => {
        if (currentToken) {
          connect(currentToken)
        }
      }, 5000)
    }
  }

  function disconnect() {
    currentToken = ''
    if (ws.value) {
      ws.value.close()
      ws.value = null
      connected.value = false
    }
  }

  /**
   * 发送播放进度到后端，触发实时分析
   */
  function sendPlaybackProgress(videoId: number, currentTime: number) {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      const message = {
        type: 'playback_progress',
        videoId,
        currentTime,
        timestamp: Date.now()
      }
      ws.value.send(JSON.stringify(message))
    }
  }

  /**
   * 通知后端停止播放，重置分析状态
   */
  function sendStopPlayback(videoId: number) {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      const message = {
        type: 'stop_playback',
        videoId
      }
      ws.value.send(JSON.stringify(message))
    }
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    connected,
    latestStatus,
    connect,
    disconnect,
    sendPlaybackProgress,
    sendStopPlayback
  }
}
