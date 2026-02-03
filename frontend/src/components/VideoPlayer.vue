<template>
  <div class="video-player-container">
    <video
      ref="videoRef"
      class="video-js vjs-default-skin"
      controls
      preload="auto"
    ></video>

    <!-- 持久状态指示器 -->
    <div v-if="showStatusIndicator" class="status-indicator" :class="statusIndicatorClass">
      <el-icon :size="20">
        <CircleCheck v-if="detectionSummary?.status === 'normal'" />
        <Warning v-else-if="detectionSummary?.status === 'alert'" />
        <Loading v-else class="rotating" />
      </el-icon>
      <span class="status-text">{{ statusText }}</span>
    </div>

    <!-- 预警覆盖层 -->
    <div v-if="showAlertOverlay" class="alert-overlay">
      <div class="alert-card">
        <div class="alert-card-header">
          <el-icon :size="24" color="#F56C6C"><Warning /></el-icon>
          <span class="alert-card-title">安全预警</span>
        </div>

        <div class="alert-card-body">
          <div class="alert-section" v-if="parsedAlert.behavior">
            <div class="section-title">检测到异常行为</div>
            <div class="section-content">{{ parsedAlert.behavior }}</div>
          </div>

          <div class="alert-section" v-if="parsedAlert.analysis">
            <div class="section-title">分析</div>
            <div class="section-content">{{ parsedAlert.analysis }}</div>
          </div>

          <div class="alert-section" v-if="parsedAlert.suggestion">
            <div class="section-title">建议行为</div>
            <div class="section-content">{{ parsedAlert.suggestion }}</div>
          </div>

          <div class="alert-meta-row">
            <div class="alert-meta-item" v-if="parsedAlert.category">
              <span class="meta-label">类别:</span>
              <span class="meta-value">{{ parsedAlert.category }}</span>
            </div>
            <div class="alert-meta-item">
              <span class="meta-label">置信度:</span>
              <span class="meta-value">{{ (currentAlert?.confidence * 100).toFixed(2) }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import videojs from 'video.js'
import 'video.js/dist/video-js.css'
import { Warning, CircleCheck, Loading } from '@element-plus/icons-vue'
import type { Alert } from '@/types/alert'
import { useAuthStore } from '@/store/modules/auth'
import type { DetectionSummary } from '@/api/video'

interface ParsedAlert {
  behavior: string
  analysis?: string
  suggestion?: string
  category?: string
}

export interface DetectionStatusMessage {
  type: 'alert' | 'normal'
  videoId: number
  analyzedFrames?: number
  message?: string
  alertData?: Alert
}

const props = defineProps<{
  src?: string
  videoId?: number
  alert?: Alert | null
  detectionStatus?: DetectionStatusMessage | null
  detectionSummary?: DetectionSummary | null
}>()

const emit = defineEmits<{
  (e: 'playbackProgress', currentTime: number): void
  (e: 'stopPlayback'): void
}>()

const videoRef = ref<HTMLVideoElement>()
let player: any = null
const showAlertOverlay = ref(false)
const showNormalOverlay = ref(false)
const currentAlert = ref<Alert | null>(null)
const authStore = useAuthStore()
const isPlaying = ref(false)
const realtimeStatus = ref<'analyzing' | 'normal' | 'alert' | null>(null)
let progressInterval: number | null = null
let normalOverlayTimer: number | null = null

// 状态指示器
const showStatusIndicator = computed(() => {
  return isPlaying.value || props.detectionSummary != null || realtimeStatus.value != null
})

const statusIndicatorClass = computed(() => {
  // 优先使用实时状态
  if (realtimeStatus.value) {
    if (realtimeStatus.value === 'normal') return 'status-normal'
    if (realtimeStatus.value === 'alert') return 'status-alert'
    return 'status-analyzing'
  }
  if (isPlaying.value) return 'status-analyzing'
  if (!props.detectionSummary) return ''
  return `status-${props.detectionSummary.status}`
})

const statusText = computed(() => {
  // 优先使用实时状态
  if (realtimeStatus.value) {
    if (realtimeStatus.value === 'normal') return '画面正常'
    if (realtimeStatus.value === 'alert') return '检测到异常'
    return '实时检测中'
  }
  if (isPlaying.value) return '实时检测中'
  if (!props.detectionSummary) return ''
  if (props.detectionSummary.status === 'normal') {
    return '画面正常'
  } else if (props.detectionSummary.status === 'alert') {
    return `${props.detectionSummary.totalAlerts} 个异常`
  } else {
    return `分析中 ${props.detectionSummary.progress}%`
  }
})

const parsedAlert = computed(() => {
  if (!currentAlert.value) {
    return { behavior: '' }
  }

  try {
    const lines = currentAlert.value.description.split('\n')
    const result: ParsedAlert = { behavior: '' }

    lines.forEach(line => {
      const trimmed = line.trim()
      if (trimmed.startsWith('检测到异常行为:')) {
        result.behavior = trimmed.replace('检测到异常行为:', '').trim()
      } else if (trimmed.startsWith('分析:')) {
        result.analysis = trimmed.replace('分析:', '').trim()
      } else if (trimmed.startsWith('建议行为:')) {
        result.suggestion = trimmed.replace('建议行为:', '').trim()
      } else if (trimmed.startsWith('类别:')) {
        result.category = trimmed.replace('类别:', '').trim()
      }
    })

    if (!result.behavior) {
      result.behavior = currentAlert.value.description
    }

    return result
  } catch (error) {
    console.error('Failed to parse alert:', error)
    return { behavior: currentAlert.value.description }
  }
})

onMounted(() => {
  if (videoRef.value) {
    // 全局配置video.js的xhr请求，添加JWT认证
    const token = authStore.token

    // 配置videojs的xhr beforeRequest钩子
    if (token && (videojs as any).Vhs) {
      (videojs as any).Vhs.xhr.beforeRequest = function(options: any) {
        options.headers = options.headers || {}
        options.headers['Authorization'] = `Bearer ${token}`
        return options
      }
    }

    player = videojs(videoRef.value, {
      controls: true,
      autoplay: false,
      preload: 'auto',
      fluid: true,
      sources: props.src ? [{
        src: props.src,
        type: 'application/x-mpegURL'
      }] : []
    })

    // 监听播放事件
    player.on('play', () => {
      isPlaying.value = true
      startProgressReporting()
    })

    // 监听暂停事件
    player.on('pause', () => {
      isPlaying.value = false
      stopProgressReporting()
    })

    // 监听结束事件
    player.on('ended', () => {
      isPlaying.value = false
      stopProgressReporting()
      emit('stopPlayback')
    })
  }
})

onUnmounted(() => {
  stopProgressReporting()
  if (normalOverlayTimer) {
    clearTimeout(normalOverlayTimer)
  }
  emit('stopPlayback')
  if (player) {
    player.dispose()
  }
})

// 开始定时发送播放进度
function startProgressReporting() {
  if (progressInterval) return

  // 每秒发送一次播放进度
  progressInterval = window.setInterval(() => {
    if (player && !player.paused()) {
      const currentTime = player.currentTime()
      emit('playbackProgress', currentTime)
    }
  }, 1000)
}

// 停止发送播放进度
function stopProgressReporting() {
  if (progressInterval) {
    clearInterval(progressInterval)
    progressInterval = null
  }
}

// 监听src变化
watch(() => props.src, (newSrc) => {
  if (player && newSrc) {
    player.src({
      src: newSrc,
      type: 'application/x-mpegURL'
    })
  }
})

// 监听预警，暂停视频
watch(() => props.alert, (newAlert) => {
  if (newAlert && player) {
    currentAlert.value = newAlert
    showAlertOverlay.value = true
    showNormalOverlay.value = false

    // 暂停视频
    player.pause()

    // 3秒后恢复播放
    setTimeout(() => {
      showAlertOverlay.value = false
      player.play()
    }, 3000)
  }
})

// 监听检测状态消息
watch(() => props.detectionStatus, (newStatus) => {
  if (newStatus) {
    if (newStatus.type === 'alert' && newStatus.alertData) {
      // 预警消息
      currentAlert.value = newStatus.alertData
      showAlertOverlay.value = true
      showNormalOverlay.value = false
      realtimeStatus.value = 'alert'

      // 清除之前的正常状态定时器
      if (normalOverlayTimer) {
        clearTimeout(normalOverlayTimer)
        normalOverlayTimer = null
      }

      if (player) {
        player.pause()
        setTimeout(() => {
          showAlertOverlay.value = false
          realtimeStatus.value = 'analyzing'
          player.play()
        }, 3000)
      }
    } else if (newStatus.type === 'normal') {
      // 无异常消息 - 短暂显示"画面正常"
      showNormalOverlay.value = true
      realtimeStatus.value = 'normal'

      // 清除之前的定时器
      if (normalOverlayTimer) {
        clearTimeout(normalOverlayTimer)
      }

      // 3秒后恢复为检测中状态
      normalOverlayTimer = window.setTimeout(() => {
        showNormalOverlay.value = false
        if (isPlaying.value) {
          realtimeStatus.value = 'analyzing'
        } else {
          realtimeStatus.value = null
        }
      }, 3000)
    }
  }
})
</script>

<style scoped>
.video-player-container {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.video-player-container :deep(.video-js) {
  width: 100%;
  height: 100%;
}

.video-player-container :deep(.vjs-fluid) {
  padding-top: 0 !important;
  height: 100% !important;
}

/* 持久状态指示器 */
.status-indicator {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  backdrop-filter: blur(8px);
  transition: all 0.3s ease;
}

.status-indicator.status-normal {
  background: rgba(103, 194, 58, 0.9);
  color: white;
  box-shadow: 0 2px 12px rgba(103, 194, 58, 0.4);
}

.status-indicator.status-alert {
  background: rgba(245, 108, 108, 0.9);
  color: white;
  box-shadow: 0 2px 12px rgba(245, 108, 108, 0.4);
}

.status-indicator.status-analyzing {
  background: rgba(64, 158, 255, 0.9);
  color: white;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.4);
}

.status-text {
  white-space: nowrap;
}

.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.alert-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 1000;
  width: 80%;
  max-width: 500px;
}

.alert-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.alert-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #FEF0F0 0%, #FDE2E4 100%);
  border-bottom: 2px solid #F56C6C;
}

.alert-card-title {
  font-size: 18px;
  font-weight: 700;
  color: #F56C6C;
}

.alert-card-body {
  padding: 20px;
}

.alert-section {
  margin-bottom: 16px;
}

.alert-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 13px;
  font-weight: 700;
  color: #606266;
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.section-content {
  font-size: 15px;
  color: #303133;
  line-height: 1.6;
}

.alert-meta-row {
  display: flex;
  gap: 24px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #EBEEF5;
}

.alert-meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-label {
  font-size: 13px;
  font-weight: 600;
  color: #909399;
}

.meta-value {
  font-size: 14px;
  font-weight: 700;
  color: #409EFF;
}

</style>
