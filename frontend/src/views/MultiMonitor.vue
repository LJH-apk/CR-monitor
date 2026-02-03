<template>
  <div class="multi-monitor-container">
    <!-- 顶部导航栏 -->
    <el-header class="monitor-header">
      <div class="header-left">
        <el-icon :size="32" color="#409EFF"><Monitor /></el-icon>
        <h1>多路视频监控</h1>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="router.push('/dashboard')">
          单路监控
        </el-button>
        <el-dropdown @command="handleUserCommand">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ authStore.user?.username }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <!-- 主内容区 -->
    <el-main class="monitor-main">
      <!-- 状态栏 -->
      <div class="status-bar">
        <el-tag :type="wsConnected ? 'success' : 'info'" size="large">
          <el-icon><Connection /></el-icon>
          {{ wsConnected ? 'WebSocket 已连接' : 'WebSocket 未连接' }}
        </el-tag>
        <el-tag type="primary" size="large">
          <el-icon><VideoPlay /></el-icon>
          可用视频: {{ readyVideos.length }}
        </el-tag>
        <el-tag type="warning" size="large">
          <el-icon><Warning /></el-icon>
          总告警: {{ totalAlerts }}
        </el-tag>
      </div>

      <!-- 2x3 视频网格 -->
      <div class="video-grid">
        <VideoCell
          v-for="(state, index) in cellStates"
          :key="index"
          :cell-index="index"
          :videos="readyVideos"
          :selected-video-id="state.videoId"
          :latest-status="state.latestStatus"
          :used-video-ids="usedVideoIds"
          @update:selected-video-id="(id) => handleVideoSelect(index, id)"
          @playback-progress="handlePlaybackProgress"
          @stop-playback="handleStopPlayback"
        />
      </div>

      <!-- 全局告警汇总面板 -->
      <el-card class="alert-summary-panel">
        <template #header>
          <div class="panel-header">
            <span>全局告警汇总</span>
            <el-badge :value="totalAlerts" :max="99" type="danger" />
          </div>
        </template>
        <div class="alert-list" v-if="allAlerts.length > 0">
          <div
            v-for="alert in allAlerts.slice(0, 10)"
            :key="alert.id"
            class="alert-item"
          >
            <el-tag type="danger" size="small">窗口 {{ getWindowForAlert(alert) }}</el-tag>
            <span class="alert-behavior">{{ parseBehavior(alert.description) }}</span>
            <span class="alert-confidence">{{ (alert.confidence * 100).toFixed(1) }}%</span>
            <span class="alert-time">{{ formatTime(alert.createdAt) }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无告警" :image-size="60" />
      </el-card>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Monitor,
  User,
  SwitchButton,
  Connection,
  VideoPlay,
  Warning
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/modules/auth'
import { useVideoStore } from '@/store/modules/video'
import { useWebSocket } from '@/composables/useWebSocket'
import { useMultiVideoState } from '@/composables/useMultiVideoState'
import VideoCell from '@/components/VideoCell.vue'
import type { Alert } from '@/types/alert'

const router = useRouter()
const authStore = useAuthStore()
const videoStore = useVideoStore()
const {
  connected: wsConnected,
  connect: wsConnect,
  disconnect: wsDisconnect,
  sendPlaybackProgress,
  sendStopPlayback,
  addMessageListener,
  removeMessageListener
} = useWebSocket()
const {
  cellStates,
  dispatchMessage,
  setVideoForCell,
  getAllAlerts
} = useMultiVideoState()

const isWsConnected = ref(false)

const readyVideos = computed(() => {
  return videoStore.videos.filter(v => v.status === 'READY')
})

const usedVideoIds = computed(() => {
  return cellStates.value
    .filter(s => s.videoId !== null)
    .map(s => s.videoId as number)
})

const allAlerts = computed(() => getAllAlerts())
const totalAlerts = computed(() => allAlerts.value.length)

function getWindowForAlert(alert: Alert): number {
  const cell = cellStates.value.find(c => c.videoId === alert.videoId)
  return cell ? cell.cellIndex + 1 : 0
}

function parseBehavior(description: string): string {
  const match = description.match(/检测到异常行为:\s*(.+?)(?:\n|$)/)
  return match ? match[1] : description.substring(0, 30)
}

function formatTime(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function handleVideoSelect(cellIndex: number, videoId: number | null) {
  setVideoForCell(cellIndex, videoId)

  // 确保 WebSocket 已连接
  if (videoId && !isWsConnected.value) {
    const token = authStore.token
    if (token) {
      wsConnect(token)
      isWsConnected.value = true
    }
  }
}

function handlePlaybackProgress(cellIndex: number, videoId: number, currentTime: number) {
  if (isWsConnected.value) {
    sendPlaybackProgress(videoId, currentTime)
  }
}

function handleStopPlayback(cellIndex: number, videoId: number) {
  if (isWsConnected.value) {
    sendStopPlayback(videoId)
  }
}

function handleUserCommand(command: string) {
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  }
}

// WebSocket 消息处理
function handleWsMessage(message: any) {
  dispatchMessage(message)
}

onMounted(async () => {
  await videoStore.fetchVideos()

  // 注册 WebSocket 消息监听器
  addMessageListener(handleWsMessage)
})

onUnmounted(() => {
  removeMessageListener(handleWsMessage)
  if (isWsConnected.value) {
    wsDisconnect()
  }
})
</script>

<style scoped>
.multi-monitor-container {
  min-height: 100vh;
  background: #f5f7fa;
}

.monitor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h1 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  color: #303133;
  transition: background 0.3s;
}

.user-info:hover {
  background: #f5f7fa;
}

.monitor-main {
  padding: 16px 20px;
}

.status-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.alert-summary-panel {
  margin-top: 16px;
}

.alert-summary-panel :deep(.el-card__header) {
  padding: 10px 16px;
}

.alert-summary-panel :deep(.el-card__body) {
  padding: 12px;
  max-height: 150px;
  overflow-y: auto;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #303133;
  font-weight: 600;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  color: #303133;
}

.alert-behavior {
  flex: 1;
  font-size: 14px;
}

.alert-confidence {
  color: #f56c6c;
  font-weight: 600;
}

.alert-time {
  color: #909399;
  font-size: 12px;
}

@media (max-width: 1200px) {
  .video-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .video-grid {
    grid-template-columns: 1fr;
  }
}
</style>
