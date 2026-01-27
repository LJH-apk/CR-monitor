import { ref, onUnmounted } from 'vue'
import type { Alert } from '@/types/alert'
import { useAlertStore } from '@/store/modules/alert'

export function useWebSocket() {
  const ws = ref<WebSocket | null>(null)
  const connected = ref(false)
  const alertStore = useAlertStore()

  function connect(token: string) {
    if (ws.value) {
      return
    }

    const wsUrl = `ws://localhost:8080/api/ws/alerts?token=${token}`
    ws.value = new WebSocket(wsUrl)

    ws.value.onopen = () => {
      connected.value = true
      console.log('WebSocket connected')
    }

    ws.value.onmessage = (event) => {
      try {
        const alert: Alert = JSON.parse(event.data)
        alertStore.addRealtimeAlert(alert)
        console.log('Received alert:', alert)
      } catch (error) {
        console.error('Failed to parse alert:', error)
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
        if (token) {
          connect(token)
        }
      }, 5000)
    }
  }

  function disconnect() {
    if (ws.value) {
      ws.value.close()
      ws.value = null
      connected.value = false
    }
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    connected,
    connect,
    disconnect
  }
}
