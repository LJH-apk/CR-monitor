import { ref, computed } from 'vue'
import type { UploadTask, UploadStatus } from '@/types/upload'
import { videoApi } from '@/api/video'

export function useUploadQueue() {
  const tasks = ref<UploadTask[]>([])
  const maxConcurrent = 3

  const pendingTasks = computed(() => tasks.value.filter(t => t.status === 'pending'))
  const uploadingTasks = computed(() => tasks.value.filter(t => t.status === 'uploading'))
  const hasActiveTasks = computed(() => tasks.value.some(t =>
    t.status === 'pending' || t.status === 'uploading' || t.status === 'transcoding'
  ))

  function generateId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
  }

  function addFiles(files: File[]) {
    const newTasks: UploadTask[] = files.map(file => ({
      id: generateId(),
      file,
      filename: file.name,
      fileSize: file.size,
      status: 'pending' as UploadStatus,
      progress: 0
    }))
    tasks.value.push(...newTasks)
    processQueue()
  }

  function processQueue() {
    const availableSlots = maxConcurrent - uploadingTasks.value.length
    if (availableSlots <= 0) return

    const tasksToStart = pendingTasks.value.slice(0, availableSlots)
    tasksToStart.forEach(task => {
      uploadTask(task)
    })
  }

  async function uploadTask(task: UploadTask) {
    const taskIndex = tasks.value.findIndex(t => t.id === task.id)
    if (taskIndex === -1) return

    const abortController = new AbortController()
    tasks.value[taskIndex].status = 'uploading'
    tasks.value[taskIndex].abortController = abortController

    try {
      const response = await videoApi.uploadWithProgress(
        task.file,
        (progress) => {
          const idx = tasks.value.findIndex(t => t.id === task.id)
          if (idx !== -1) {
            tasks.value[idx].progress = progress
          }
        },
        abortController
      )

      const idx = tasks.value.findIndex(t => t.id === task.id)
      if (idx !== -1) {
        tasks.value[idx].status = 'uploaded'
        tasks.value[idx].videoId = response.data.videoId
        tasks.value[idx].progress = 100

        // 开始轮询转码状态
        pollTranscodingStatus(task.id, response.data.videoId)
      }
    } catch (error: any) {
      const idx = tasks.value.findIndex(t => t.id === task.id)
      if (idx !== -1) {
        if (error.name === 'CanceledError' || error.code === 'ERR_CANCELED') {
          tasks.value[idx].status = 'cancelled'
        } else {
          tasks.value[idx].status = 'failed'
          tasks.value[idx].error = error.message || '上传失败'
        }
      }
    } finally {
      processQueue()
    }
  }

  async function pollTranscodingStatus(taskId: string, videoId: number) {
    const idx = tasks.value.findIndex(t => t.id === taskId)
    if (idx === -1) return

    tasks.value[idx].status = 'transcoding'

    const poll = async () => {
      try {
        const response = await videoApi.getById(videoId)
        const currentIdx = tasks.value.findIndex(t => t.id === taskId)
        if (currentIdx === -1) return

        const status = response.data.status
        if (status === 'READY') {
          tasks.value[currentIdx].status = 'completed'
        } else if (status === 'FAILED') {
          tasks.value[currentIdx].status = 'failed'
          tasks.value[currentIdx].error = '转码失败'
        } else {
          // 继续轮询
          setTimeout(poll, 3000)
        }
      } catch (error) {
        const currentIdx = tasks.value.findIndex(t => t.id === taskId)
        if (currentIdx !== -1) {
          tasks.value[currentIdx].status = 'failed'
          tasks.value[currentIdx].error = '获取状态失败'
        }
      }
    }

    poll()
  }

  function cancelTask(taskId: string) {
    const task = tasks.value.find(t => t.id === taskId)
    if (!task) return

    if (task.status === 'uploading' && task.abortController) {
      task.abortController.abort()
    } else if (task.status === 'pending') {
      task.status = 'cancelled'
    }
  }

  function removeTask(taskId: string) {
    const idx = tasks.value.findIndex(t => t.id === taskId)
    if (idx !== -1) {
      const task = tasks.value[idx]
      if (task.status === 'uploading' && task.abortController) {
        task.abortController.abort()
      }
      tasks.value.splice(idx, 1)
    }
  }

  function retryTask(taskId: string) {
    const task = tasks.value.find(t => t.id === taskId)
    if (!task || (task.status !== 'failed' && task.status !== 'cancelled')) return

    task.status = 'pending'
    task.progress = 0
    task.error = undefined
    task.abortController = undefined
    processQueue()
  }

  function clearCompleted() {
    tasks.value = tasks.value.filter(t =>
      t.status !== 'completed' && t.status !== 'failed' && t.status !== 'cancelled'
    )
  }

  function clearAll() {
    tasks.value.forEach(task => {
      if (task.status === 'uploading' && task.abortController) {
        task.abortController.abort()
      }
    })
    tasks.value = []
  }

  return {
    tasks,
    hasActiveTasks,
    addFiles,
    cancelTask,
    removeTask,
    retryTask,
    clearCompleted,
    clearAll
  }
}
