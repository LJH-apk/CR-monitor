<template>
  <el-card class="alert-panel">
    <template #header>
      <div class="panel-header">
        <span>实时信息</span>
        <el-badge :value="unacknowledgedCount" :max="99" type="danger" />
      </div>
    </template>

    <div class="alert-list">
      <!-- 无异常状态提示 -->
      <div v-if="showNormalStatus" class="normal-status-card">
        <div class="normal-status-icon">
          <el-icon :size="24" color="#67C23A"><CircleCheck /></el-icon>
        </div>
        <div class="normal-status-content">
          <div class="normal-status-title">检测周期完成，无异常</div>
          <div class="normal-status-detail">已分析 {{ normalFrameCount }} 帧</div>
        </div>
      </div>

      <el-empty v-if="alerts.length === 0 && !showNormalStatus" description="当前情况正常" />

      <div
        v-for="alert in alerts"
        :key="alert.id"
        class="alert-item"
        :class="{ acknowledged: alert.isAcknowledged }"
      >
        <div class="alert-header">
          <el-tag
            :type="getSeverityType(alert.severityLevel)"
            size="small"
          >
            等级 {{ alert.severityLevel }}
          </el-tag>
          <span class="alert-time">{{ formatTime(alert.timestampInVideo) }}</span>
        </div>

        <!-- 新增：结构化显示 -->
        <div class="alert-content-structured">
          <div class="alert-section" v-if="parseAlert(alert).behavior">
            <div class="section-title">检测到异常行为</div>
            <div class="section-content">{{ parseAlert(alert).behavior }}</div>
          </div>

          <div class="alert-section" v-if="parseAlert(alert).analysis">
            <div class="section-title">分析</div>
            <div class="section-content">{{ parseAlert(alert).analysis }}</div>
          </div>

          <div class="alert-section" v-if="parseAlert(alert).suggestion">
            <div class="section-title">建议行为</div>
            <div class="section-content">{{ parseAlert(alert).suggestion }}</div>
          </div>

          <div class="alert-meta-row">
            <div class="alert-section-inline" v-if="parseAlert(alert).category">
              <span class="section-title-inline">类别:</span>
              <span class="section-content-inline">{{ parseAlert(alert).category }}</span>
            </div>
            <div class="alert-section-inline">
              <span class="section-title-inline">置信度:</span>
              <span class="section-content-inline">{{ (alert.confidence * 100).toFixed(2) }}%</span>
            </div>
          </div>
        </div>

        <div class="alert-actions">
          <el-button
            v-if="!alert.isAcknowledged"
            type="primary"
            size="small"
            @click="handleAcknowledge(alert.id)"
          >
            确认
          </el-button>
          <el-tag v-else type="success" size="small">已确认</el-tag>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import type { Alert } from '@/types/alert'
import { useAlertStore } from '@/store/modules/alert'

interface ParsedAlert {
  behavior: string
  analysis?: string
  suggestion?: string
  category?: string
  severity?: string
}

export interface DetectionStatusMessage {
  type: 'alert' | 'normal'
  videoId: number
  analyzedFrames?: number
  message?: string
  alertData?: Alert
}

const props = defineProps<{
  alerts: Alert[]
  detectionStatus?: DetectionStatusMessage | null
}>()

const alertStore = useAlertStore()

// 显示无异常状态
const showNormalStatus = ref(false)
const normalFrameCount = ref(0)

// 监听检测状态
watch(() => props.detectionStatus, (newStatus) => {
  if (newStatus && newStatus.type === 'normal') {
    normalFrameCount.value = newStatus.analyzedFrames || 0
    showNormalStatus.value = true

    // 5秒后隐藏
    setTimeout(() => {
      showNormalStatus.value = false
    }, 5000)
  }
}, { immediate: true })

const unacknowledgedCount = computed(() => {
  return props.alerts.filter(a => !a.isAcknowledged).length
})

const parseAlert = (alert: Alert): ParsedAlert => {
  try {
    const lines = alert.description.split('\n')
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
      } else if (trimmed.startsWith('严重等级:')) {
        result.severity = trimmed.replace('严重等级:', '').trim()
      }
    })

    // 解析失败时使用原始description
    if (!result.behavior) {
      result.behavior = alert.description
    }

    return result
  } catch (error) {
    console.error('Failed to parse alert description:', error)
    return { behavior: alert.description }
  }
}

const getSeverityType = (level: number) => {
  if (level >= 4) return 'danger'
  if (level >= 3) return 'warning'
  return 'info'
}

const formatTime = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

const handleAcknowledge = async (id: number) => {
  try {
    await alertStore.acknowledgeAlert(id)
    ElMessage.success('预警已确认')
  } catch (error) {
    ElMessage.error('确认失败')
  }
}
</script>

<style scoped>
.alert-panel {
  height: 100%;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alert-list {
  max-height: 600px;
  overflow-y: auto;
}

.alert-item {
  padding: 12px;
  margin-bottom: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fff;
  transition: all 0.3s;
}

.alert-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.alert-item.acknowledged {
  opacity: 0.6;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.alert-time {
  font-size: 12px;
  color: #909399;
}

/* 新增：结构化显示样式 */
.alert-content-structured {
  margin-bottom: 12px;
}

.alert-section {
  margin-bottom: 10px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 4px;
}

.section-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
}

.alert-meta-row {
  display: flex;
  gap: 20px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.alert-section-inline {
  display: flex;
  align-items: center;
  gap: 6px;
}

.section-title-inline {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
}

.section-content-inline {
  font-size: 12px;
  color: #303133;
}

.alert-actions {
  display: flex;
  justify-content: flex-end;
}

/* 无异常状态卡片样式 */
.normal-status-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #f0f9eb 0%, #e1f3d8 100%);
  border: 1px solid #67C23A;
  border-radius: 8px;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.normal-status-icon {
  flex-shrink: 0;
}

.normal-status-content {
  flex: 1;
}

.normal-status-title {
  font-size: 14px;
  font-weight: 600;
  color: #67C23A;
  margin-bottom: 4px;
}

.normal-status-detail {
  font-size: 12px;
  color: #909399;
}
</style>
