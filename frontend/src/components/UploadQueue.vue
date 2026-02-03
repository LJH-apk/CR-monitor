<template>
  <el-card v-if="tasks.length > 0" class="upload-queue">
    <template #header>
      <div class="queue-header">
        <span>上传队列 ({{ tasks.length }})</span>
        <el-button
          v-if="hasCompletedTasks"
          type="primary"
          link
          size="small"
          @click="$emit('clearCompleted')"
        >
          清空已完成
        </el-button>
      </div>
    </template>

    <div class="task-list">
      <div
        v-for="task in tasks"
        :key="task.id"
        class="task-item"
      >
        <div class="task-info">
          <el-icon class="file-icon"><VideoCamera /></el-icon>
          <span class="filename" :title="task.filename">{{ task.filename }}</span>
          <span class="filesize">{{ formatFileSize(task.fileSize) }}</span>
        </div>

        <div class="task-progress">
          <el-progress
            :percentage="task.progress"
            :status="getProgressStatus(task.status)"
            :stroke-width="6"
            :show-text="false"
          />
        </div>

        <div class="task-status">
          <el-tag :type="getStatusType(task.status)" size="small">
            {{ getStatusText(task.status) }}
          </el-tag>
        </div>

        <div class="task-actions">
          <el-button
            v-if="task.status === 'pending' || task.status === 'uploading'"
            type="danger"
            link
            size="small"
            @click="$emit('cancel', task.id)"
          >
            取消
          </el-button>
          <el-button
            v-if="task.status === 'failed' || task.status === 'cancelled'"
            type="primary"
            link
            size="small"
            @click="$emit('retry', task.id)"
          >
            重试
          </el-button>
          <el-button
            v-if="task.status === 'completed' || task.status === 'failed' || task.status === 'cancelled'"
            type="info"
            link
            size="small"
            @click="$emit('remove', task.id)"
          >
            移除
          </el-button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { VideoCamera } from '@element-plus/icons-vue'
import type { UploadTask, UploadStatus } from '@/types/upload'

const props = defineProps<{
  tasks: UploadTask[]
}>()

defineEmits<{
  (e: 'cancel', taskId: string): void
  (e: 'retry', taskId: string): void
  (e: 'remove', taskId: string): void
  (e: 'clearCompleted'): void
}>()

const hasCompletedTasks = computed(() =>
  props.tasks.some(t => t.status === 'completed' || t.status === 'failed' || t.status === 'cancelled')
)

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

function getStatusType(status: UploadStatus): string {
  const map: Record<UploadStatus, string> = {
    pending: 'info',
    uploading: 'primary',
    uploaded: 'primary',
    transcoding: 'warning',
    completed: 'success',
    failed: 'danger',
    cancelled: 'info'
  }
  return map[status]
}

function getStatusText(status: UploadStatus): string {
  const map: Record<UploadStatus, string> = {
    pending: '等待中',
    uploading: '上传中',
    uploaded: '已上传',
    transcoding: '转码中',
    completed: '完成',
    failed: '失败',
    cancelled: '已取消'
  }
  return map[status]
}

function getProgressStatus(status: UploadStatus): '' | 'success' | 'exception' | 'warning' {
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'exception'
  if (status === 'transcoding') return 'warning'
  return ''
}
</script>

<style scoped>
.upload-queue {
  margin-bottom: 20px;
}

.queue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-list {
  max-height: 300px;
  overflow-y: auto;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.task-item:last-child {
  border-bottom: none;
}

.task-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.file-icon {
  color: #409eff;
  flex-shrink: 0;
}

.filename {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.filesize {
  color: #909399;
  font-size: 12px;
  flex-shrink: 0;
}

.task-progress {
  width: 150px;
  flex-shrink: 0;
}

.task-status {
  width: 70px;
  flex-shrink: 0;
  text-align: center;
}

.task-actions {
  width: 60px;
  flex-shrink: 0;
  text-align: right;
}
</style>
