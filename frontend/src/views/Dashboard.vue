<template>
  <div class="dashboard-container">
    <!-- 顶部导航栏 -->
    <el-header class="dashboard-header">
      <div class="header-left">
        <el-icon :size="32" color="#409EFF"><VideoCamera /></el-icon>
        <h1>智能安全监控系统</h1>
      </div>
      <div class="header-right">
        <!-- 多路监控按钮 -->
        <el-button
          type="primary"
          @click="router.push('/multi-monitor')"
        >
          多路监控
        </el-button>

        <!-- 样本上传按钮（所有用户可见） -->
        <el-button
          type="success"
          @click="router.push('/samples/upload')"
        >
          样本上传
        </el-button>

        <!-- 样本列表按钮（所有用户可见） -->
        <el-button
          type="info"
          @click="router.push('/samples/list')"
        >
          样本列表
        </el-button>

        <!-- 视频管理按钮（管理员可见） -->
        <el-button
          v-if="authStore.isAdmin()"
          type="primary"
          @click="router.push('/admin/videos')"
        >
          视频管理
        </el-button>

        <!-- 管理后台按钮（管理员可见） -->
        <el-button
          v-if="authStore.isAdmin()"
          type="warning"
          :icon="Setting"
          @click="router.push('/admin/behaviors')"
        >
          管理后台
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
    <el-main class="dashboard-main">
      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <el-card class="stat-card">
            <el-statistic title="总预警数" :value="stats?.totalAlerts || 0">
              <template #prefix>
                <el-icon color="#F56C6C"><Warning /></el-icon>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <el-statistic title="未确认预警" :value="stats?.unacknowledgedAlerts || 0">
              <template #prefix>
                <el-icon color="#E6A23C"><Bell /></el-icon>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <el-statistic title="视频总数" :value="stats?.totalVideos || 0">
              <template #prefix>
                <el-icon color="#409EFF"><VideoPlay /></el-icon>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <el-statistic title="连接状态" :value="wsConnected ? '已连接' : '未连接'">
              <template #prefix>
                <el-icon :color="wsConnected ? '#67C23A' : '#909399'">
                  <Connection />
                </el-icon>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
      </el-row>

      <!-- 视频和预警区域 -->
      <el-row :gutter="20" class="content-row">
        <el-col :span="16">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>视频监控</span>
                <el-select
                  v-model="selectedVideoId"
                  placeholder="选择视频"
                  style="width: 200px"
                  @change="handleVideoChange"
                >
                  <el-option
                    v-for="video in videoStore.videos"
                    :key="video.id"
                    :label="video.originalFilename"
                    :value="video.id"
                    :disabled="video.status !== 'READY'"
                  >
                    <span>{{ video.originalFilename }}</span>
                    <el-tag
                      v-if="video.status !== 'READY'"
                      size="small"
                      type="warning"
                      style="margin-left: 8px"
                    >
                      {{ video.status }}
                    </el-tag>
                  </el-option>
                </el-select>
              </div>
            </template>

            <VideoPlayer
              v-if="currentVideoSrc"
              :src="currentVideoSrc"
              :video-id="selectedVideoId"
              :alert="latestAlert"
              :detection-status="currentDetectionStatus"
              :detection-summary="detectionSummary"
              @playback-progress="handlePlaybackProgress"
              @stop-playback="handleStopPlayback"
            />
            <el-empty v-else description="请选择视频" />
          </el-card>
        </el-col>

        <el-col :span="8">
          <AlertPanel :alerts="filteredRealtimeAlerts" :detection-status="currentDetectionStatus" />
        </el-col>
      </el-row>

      <!-- 图表区域 -->
      <AlertChart :stats="stats" />

      <!-- 底部备案信息 -->
      <div class="footer-info">
        <span>© 2026 智能监控系统 V1.5.3 版权所有</span>
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">京ICP备XXXXXXXX号-1</a>
      </div>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  VideoCamera,
  User,
  Setting,
  SwitchButton,
  Warning,
  Bell,
  VideoPlay,
  Connection
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/modules/auth'
import { useVideoStore } from '@/store/modules/video'
import { useAlertStore } from '@/store/modules/alert'
import { useWebSocket } from '@/composables/useWebSocket'
import { dashboardApi } from '@/api/alert'
import { videoApi, type DetectionSummary } from '@/api/video'
import VideoPlayer from '@/components/VideoPlayer.vue'
import AlertPanel from '@/components/AlertPanel.vue'
import AlertChart from '@/components/AlertChart.vue'
import type { DashboardStats } from '@/types/alert'

const router = useRouter()
const authStore = useAuthStore()
const videoStore = useVideoStore()
const alertStore = useAlertStore()
const { connected: wsConnected, latestStatus, connect: wsConnect, disconnect: wsDisconnect, sendPlaybackProgress, sendStopPlayback } = useWebSocket()

const selectedVideoId = ref<number>()
const stats = ref<DashboardStats | null>(null)
const isWsConnected = ref(false)
const detectionSummary = ref<DetectionSummary | null>(null)
let summaryInterval: number | null = null

// 当前视频的检测状态（用于传递给 VideoPlayer）
const currentDetectionStatus = computed(() => {
  if (!selectedVideoId.value || !latestStatus.value) return null
  // 使用 == 宽松比较，避免类型不匹配问题（后端 Long vs 前端 number）
  if (latestStatus.value.videoId == selectedVideoId.value) {
    return latestStatus.value
  }
  return null
})

const currentVideoSrc = computed(() => {
  if (!selectedVideoId.value) return null
  const video = videoStore.videos.find(v => v.id === selectedVideoId.value)
  if (!video || !video.hlsPath) return null
  return `/api/files/videos/${video.id}/playlist.m3u8`
})

// 过滤当前视频的实时预警
const filteredRealtimeAlerts = computed(() => {
  if (!selectedVideoId.value) return []
  return alertStore.realtimeAlerts.filter(alert => alert.videoId === selectedVideoId.value)
})

const latestAlert = computed(() => {
  return filteredRealtimeAlerts.value[0] || null
})

onMounted(async () => {
  // 加载视频列表
  await videoStore.fetchVideos()

  // 加载预警
  await alertStore.fetchRecent(24)

  // 加载统计数据
  await loadStats()

  // 不再自动连接WebSocket，改为在选择视频后连接

  // 定时刷新统计数据（每1秒刷新）
  const interval = setInterval(loadStats, 1000)

  onUnmounted(() => {
    clearInterval(interval)
    if (summaryInterval) {
      clearInterval(summaryInterval)
    }
    if (isWsConnected.value) {
      wsDisconnect()
    }
  })
})

const loadStats = async () => {
  try {
    const response = await dashboardApi.getStats(24)
    stats.value = response.data
  } catch (error) {
    console.error('Failed to load stats:', error)
  }
}

const handleVideoChange = (videoId: number) => {
  const video = videoStore.videos.find(v => v.id === videoId)
  if (video) {
    videoStore.setCurrentVideo(video)

    // 加载该视频的历史预警
    alertStore.fetchByVideo(videoId)

    // 开始轮询检测摘要
    startSummaryPolling(videoId)

    // 视频就绪后连接WebSocket
    if (video.status === 'READY') {
      connectWebSocketForVideo(videoId)
    }
  }
}

const startSummaryPolling = async (videoId: number) => {
  // 清除之前的轮询
  if (summaryInterval) {
    clearInterval(summaryInterval)
  }

  // 立即获取一次
  await loadDetectionSummary(videoId)

  // 每3秒轮询一次
  summaryInterval = setInterval(async () => {
    await loadDetectionSummary(videoId)
  }, 3000)
}

const loadDetectionSummary = async (videoId: number) => {
  try {
    const response = await videoApi.getDetectionSummary(videoId)
    detectionSummary.value = response.data
  } catch (error) {
    console.error('Failed to load detection summary:', error)
  }
}

const connectWebSocketForVideo = (videoId: number) => {
  // 如果已连接，先断开
  if (isWsConnected.value) {
    wsDisconnect()
  }

  // 连接WebSocket
  const token = authStore.token
  if (token) {
    wsConnect(token)
    isWsConnected.value = true
    console.log('WebSocket connected for video:', videoId)
  }
}

// 处理播放进度事件，发送给后端进行实时分析
const handlePlaybackProgress = (currentTime: number) => {
  if (selectedVideoId.value && isWsConnected.value) {
    sendPlaybackProgress(selectedVideoId.value, currentTime)
  }
}

// 处理停止播放事件
const handleStopPlayback = () => {
  if (selectedVideoId.value && isWsConnected.value) {
    sendStopPlayback(selectedVideoId.value)
  }
}

const handleUserCommand = async (command: string) => {
  if (command === 'logout') {
    await authStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  }
}
</script>

<style scoped>
.dashboard-container {
  min-height: 100vh;
  background: #f5f7fa;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 0 20px;
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
  transition: background 0.3s;
}

.user-info:hover {
  background: #f5f7fa;
}

.dashboard-main {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.content-row {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-info {
  margin-top: 40px;
  padding: 20px 0;
  text-align: center;
  color: #909399;
  font-size: 12px;
  border-top: 1px solid #EBEEF5;
}

.footer-info span {
  margin-right: 16px;
}

.footer-info a {
  color: #909399;
  text-decoration: none;
  transition: color 0.3s;
}

.footer-info a:hover {
  color: #409EFF;
}
</style>
