import { ref } from 'vue'
import type { Alert } from '@/types/alert'
import type { DetectionStatusMessage } from './useWebSocket'

export interface VideoCellState {
  cellIndex: number
  videoId: number | null
  alerts: Alert[]
  latestStatus: DetectionStatusMessage | null
}

export function useMultiVideoState() {
  const cellStates = ref<VideoCellState[]>([
    { cellIndex: 0, videoId: null, alerts: [], latestStatus: null },
    { cellIndex: 1, videoId: null, alerts: [], latestStatus: null },
    { cellIndex: 2, videoId: null, alerts: [], latestStatus: null },
    { cellIndex: 3, videoId: null, alerts: [], latestStatus: null },
    { cellIndex: 4, videoId: null, alerts: [], latestStatus: null },
    { cellIndex: 5, videoId: null, alerts: [], latestStatus: null },
  ])

  function findCellByVideoId(videoId: number): VideoCellState | undefined {
    return cellStates.value.find(cell => cell.videoId === videoId)
  }

  function dispatchMessage(message: DetectionStatusMessage) {
    const cell = findCellByVideoId(message.videoId)
    if (cell) {
      cell.latestStatus = message
      if (message.type === 'alert' && message.alertData) {
        cell.alerts.unshift(message.alertData)
        if (cell.alerts.length > 20) {
          cell.alerts = cell.alerts.slice(0, 20)
        }
      }
    }
  }

  function setVideoForCell(cellIndex: number, videoId: number | null) {
    const cell = cellStates.value[cellIndex]
    if (cell) {
      cell.videoId = videoId
      cell.alerts = []
      cell.latestStatus = null
    }
  }

  function getStateForCell(cellIndex: number): VideoCellState | undefined {
    return cellStates.value[cellIndex]
  }

  function clearAlertsForCell(cellIndex: number) {
    const cell = cellStates.value[cellIndex]
    if (cell) {
      cell.alerts = []
    }
  }

  function getAllAlerts(): Alert[] {
    return cellStates.value.flatMap(cell => cell.alerts)
  }

  return {
    cellStates,
    findCellByVideoId,
    dispatchMessage,
    setVideoForCell,
    getStateForCell,
    clearAlertsForCell,
    getAllAlerts
  }
}
