<template>
  <div class="video-cell">
    <div class="cell-header">
      <span class="cell-title">窗口 {{ cellIndex + 1 }}</span>
      <el-select
        v-model="localVideoId"
        placeholder="选择视频"
        size="small"
        class="video-select"
        clearable
        @change="handleVideoChange"
      >
        <el-option
          v-for="video in videos"
          :key="video.id"
          :label="video.originalFilename"
          :value="video.id"
          :disabled="isVideoUsedByOther(video.id)"
        />
      </el-select>
    </div>
    <div class="cell-body">
      <VideoPlayer
        v-if="selectedVideo"
        :src="videoSrc"
        :video-id="selectedVideo.id"
        :alert="currentAlert"
        :detection-status="latestStatus"
        @playback-progress="handlePlaybackProgress"
        @stop-playback="handleStopPlayback"
      />
      <div v-else class="empty-placeholder">
        <el-icon :size="48" color="#909399"><VideoCamera /></el-icon>
        <p>请选择视频</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { VideoCamera } from '@element-plus/icons-vue'
import VideoPlayer from './VideoPlayer.vue'
import type { Video } from '@/types/video'
import type { Alert } from '@/types/alert'
import type { DetectionStatusMessage } from '@/composables/useWebSocket'

const props = defineProps<{
  cellIndex: number
  videos: Video[]
  selectedVideoId: number | null
  latestStatus: DetectionStatusMessage | null
  usedVideoIds: number[]
}>()

const emit = defineEmits<{
  (e: 'update:selectedVideoId', videoId: number | null): void
  (e: 'playbackProgress', cellIndex: number, videoId: number, currentTime: number): void
  (e: 'stopPlayback', cellIndex: number, videoId: number): void
}>()

const localVideoId = ref<number | null>(props.selectedVideoId)
const currentAlert = ref<Alert | null>(null)

const selectedVideo = computed(() => {
  if (!localVideoId.value) return null
  return props.videos.find(v => v.id === localVideoId.value) || null
})

const videoSrc = computed(() => {
  if (!selectedVideo.value) return ''
  return `/api/files/videos/${selectedVideo.value.id}/playlist.m3u8`
})

function isVideoUsedByOther(videoId: number): boolean {
  return props.usedVideoIds.includes(videoId) && videoId !== localVideoId.value
}

function handleVideoChange(videoId: number | null) {
  emit('update:selectedVideoId', videoId)
}

function handlePlaybackProgress(currentTime: number) {
  if (localVideoId.value) {
    emit('playbackProgress', props.cellIndex, localVideoId.value, currentTime)
  }
}

function handleStopPlayback() {
  if (localVideoId.value) {
    emit('stopPlayback', props.cellIndex, localVideoId.value)
  }
}

watch(() => props.selectedVideoId, (newVal) => {
  localVideoId.value = newVal
})

watch(() => props.latestStatus, (newStatus) => {
  // 确保告警是针对当前窗口视频的
  if (newStatus?.type === 'alert' && newStatus.alertData && newStatus.videoId === localVideoId.value) {
    currentAlert.value = newStatus.alertData
  }
})
</script>

<style scoped>
.video-cell {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.cell-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 10px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.cell-title {
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.video-select {
  width: 180px;
}

.cell-body {
  position: relative;
  width: 100%;
  padding-top: 56.25%; /* 16:9 宽高比 */
  background: #000;
  overflow: hidden;
}

.cell-body :deep(.video-player-container) {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.cell-body :deep(.video-js) {
  width: 100%;
  height: 100%;
}

.cell-body :deep(.video-js video) {
  object-fit: contain;
}

.empty-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: #f5f7fa;
}

.empty-placeholder p {
  margin-top: 12px;
  font-size: 14px;
}
</style>
