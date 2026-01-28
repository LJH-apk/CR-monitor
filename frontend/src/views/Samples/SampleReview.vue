<template>
  <div class="review-container">
    <el-page-header @back="goBack" title="返回">
      <template #content>
        <span class="page-title">样本审核</span>
      </template>
      <template #extra>
        <el-button type="primary" @click="handleSave" :loading="saving">
          保存并继续
        </el-button>
      </template>
    </el-page-header>

    <div class="review-content">
      <!-- 左侧：画布区域（只读） -->
      <el-card class="canvas-card">
        <template #header>
          <div class="card-header">
            <span>标注预览</span>
            <el-tag :type="getStatusType(currentSample?.status)">
              {{ getStatusText(currentSample?.status) }}
            </el-tag>
          </div>
        </template>
        <div class="canvas-wrapper">
          <canvas ref="canvasRef" id="review-canvas"></canvas>
        </div>
        <div class="canvas-controls">
          <el-checkbox v-model="showLabels">显示标签</el-checkbox>
        </div>
      </el-card>

      <!-- 右侧：信息和操作 -->
      <div class="info-panel">
        <!-- 样本信息 -->
        <el-card class="info-card">
          <template #header>样本信息</template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="文件名">
              {{ currentSample?.originalFilename }}
            </el-descriptions-item>
            <el-descriptions-item label="图片尺寸">
              {{ currentSample?.imageWidth }} x {{ currentSample?.imageHeight }}
            </el-descriptions-item>
            <el-descriptions-item label="文件大小">
              {{ formatFileSize(currentSample?.fileSize || 0) }}
            </el-descriptions-item>
            <el-descriptions-item label="上传者">
              {{ currentSample?.username }}
            </el-descriptions-item>
            <el-descriptions-item label="上传时间">
              {{ formatDateTime(currentSample?.uploadTime) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 标注列表 -->
        <el-card class="annotations-card">
          <template #header>
            标注列表 ({{ annotations.length }})
          </template>
          <div class="annotation-list">
            <div
              v-for="(ann, index) in annotations"
              :key="index"
              class="annotation-item"
              :class="{ active: highlightedIndex === index }"
              @mouseenter="highlightAnnotation(index)"
              @mouseleave="unhighlightAnnotation()"
            >
              <div class="annotation-info">
                <el-tag size="small" type="success">
                  {{ ann.dangerBehaviorName }}
                </el-tag>
                <p class="annotation-coords">
                  ({{ ann.xMin }}, {{ ann.yMin }}) - ({{ ann.xMax }}, {{ ann.yMax }})
                </p>
                <p v-if="ann.notes" class="annotation-notes">{{ ann.notes }}</p>
              </div>
              <el-button
                size="small"
                type="primary"
                link
                @click="locateAnnotation(index)"
              >
                定位
              </el-button>
            </div>
            <el-empty v-if="annotations.length === 0" description="暂无标注" />
          </div>
        </el-card>

        <!-- 审核操作 -->
        <el-card class="action-card">
          <template #header>审核操作</template>
          <div class="review-actions">
            <el-button
              type="success"
              size="large"
              @click="handleApprove"
              :loading="submitting"
            >
              ✓ 通过
            </el-button>
            <el-button
              type="danger"
              size="large"
              @click="showRejectDialog = true"
              :loading="submitting"
            >
              ✗ 拒绝
            </el-button>
            <el-button
              type="warning"
              size="large"
              @click="handleReAnnotate"
              :loading="submitting"
            >
              ↻ 重新标注
            </el-button>
          </div>

          <!-- 拒绝原因输入 -->
          <div v-if="showRejectDialog" class="reject-reason">
            <el-input
              v-model="rejectReason"
              type="textarea"
              :rows="3"
              placeholder="请输入拒绝原因（可选）"
            />
            <div class="reject-actions">
              <el-button @click="showRejectDialog = false">取消</el-button>
              <el-button type="danger" @click="handleReject">确认拒绝</el-button>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 导航控制 -->
    <div class="navigation-bar">
      <el-button
        :disabled="!hasPrevious"
        @click="navigateToPrevious"
      >
        ← 上一个
      </el-button>
      <span class="navigation-info">
        {{ currentIndex + 1 }} / {{ totalSamples }}
      </span>
      <el-button
        :disabled="!hasNext"
        @click="navigateToNext"
      >
        下一个 →
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Canvas, Rect, FabricImage } from 'fabric'
import * as fabric from 'fabric'
import { sampleApi } from '@/api/sample'
import type { TrainingSample, Annotation } from '@/types/sample'

const route = useRoute()
const router = useRouter()

// 状态管理
const canvasRef = ref<HTMLCanvasElement | null>(null)
const canvas = ref<Canvas | null>(null)
const currentSample = ref<TrainingSample | null>(null)
const annotations = ref<Annotation[]>([])
const canvasScale = ref(1)

// 待审核样本列表
const pendingSamples = ref<TrainingSample[]>([])
const currentIndex = ref(0)

// UI 状态
const showLabels = ref(true)
const highlightedIndex = ref<number | null>(null)
const showRejectDialog = ref(false)
const rejectReason = ref('')
const submitting = ref(false)
const saving = ref(false)

// 计算属性
const hasPrevious = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value < pendingSamples.value.length - 1)
const totalSamples = computed(() => pendingSamples.value.length)

onMounted(async () => {
  await loadPendingSamples()

  // 如果 URL 中有 ID，加载指定样本
  const sampleId = route.params.id
  if (sampleId) {
    const index = pendingSamples.value.findIndex(s => s.id === Number(sampleId))
    if (index !== -1) {
      currentIndex.value = index
    }
  }

  await loadCurrentSample()
  initCanvas()
})

onUnmounted(() => {
  if (canvas.value) {
    canvas.value.dispose()
  }
})

// 监听标签显示切换
watch(showLabels, () => {
  if (canvas.value) {
    canvas.value.forEachObject((obj: any) => {
      if (obj.data?.type === 'annotation-group') {
        const label = obj._objects[1]  // Group中的第二个对象是标签
        if (label) {
          label.set({ visible: showLabels.value })
        }
      }
    })
    canvas.value.renderAll()
  }
})

const goBack = () => {
  router.push('/samples/list')
}

// 加载待审核样本列表
const loadPendingSamples = async () => {
  try {
    const response = await sampleApi.getAll('ANNOTATED')
    pendingSamples.value = response.data

    if (pendingSamples.value.length === 0) {
      ElMessage.info('暂无待审核样本')
      router.push('/samples/list')
    }
  } catch (error) {
    ElMessage.error('加载待审核样本失败')
  }
}

// 加载当前样本
const loadCurrentSample = async () => {
  if (pendingSamples.value.length === 0) return

  const sample = pendingSamples.value[currentIndex.value]
  try {
    const response = await sampleApi.getById(sample.id)
    currentSample.value = response.data
    annotations.value = response.data.annotations || []

    // 更新 URL
    router.replace(`/samples/review/${sample.id}`)
  } catch (error) {
    ElMessage.error('加载样本详情失败')
  }
}

// 初始化画布（只读模式）
const initCanvas = () => {
  if (!canvasRef.value || !currentSample.value) return

  const imgUrl = `/api/storage/training-samples/${currentSample.value.userId}/${currentSample.value.storedFilename}`

  FabricImage.fromURL(imgUrl).then((img) => {
    if (!canvasRef.value) return

    const imgWidth = img.width || 800
    const imgHeight = img.height || 600

    const maxWidth = 900
    const maxHeight = 700

    let canvasWidth = imgWidth
    let canvasHeight = imgHeight
    let scale = 1

    if (imgWidth > maxWidth || imgHeight > maxHeight) {
      scale = Math.min(maxWidth / imgWidth, maxHeight / imgHeight)
      canvasWidth = imgWidth * scale
      canvasHeight = imgHeight * scale
    }

    canvasScale.value = scale

    // 清理旧画布
    if (canvas.value) {
      canvas.value.dispose()
    }

    canvas.value = new Canvas('review-canvas', {
      width: canvasWidth,
      height: canvasHeight,
      backgroundColor: '#f0f0f0',
      selection: false  // 只读模式：禁用选择
    })

    img.scale(scale)
    img.set({
      left: 0,
      top: 0,
      originX: 'left',
      originY: 'top',
      selectable: false,
      evented: false
    })

    canvas.value.add(img)
    canvas.value.sendObjectToBack(img)

    // 渲染标注框（只读）
    renderAnnotations()
    canvas.value.renderAll()
  }).catch((error) => {
    console.error('Failed to load image:', error)
    ElMessage.error('图片加载失败')
  })
}

// 渲染标注框（只读模式，使用 Group）
const renderAnnotations = () => {
  if (!canvas.value || annotations.value.length === 0) return

  const scale = canvasScale.value

  annotations.value.forEach((annotation, index) => {
    const left = annotation.xMin * scale
    const top = annotation.yMin * scale
    const width = (annotation.xMax - annotation.xMin) * scale
    const height = (annotation.yMax - annotation.yMin) * scale

    // 创建矩形框
    const rect = new Rect({
      left: 0,
      top: 0,
      width,
      height,
      fill: 'rgba(0, 255, 0, 0.15)',
      stroke: 'green',
      strokeWidth: 2
    })

    // 创建标签
    const label = new fabric.Text(annotation.dangerBehaviorName || '未知', {
      left: 0,
      top: -20,
      fontSize: 14,
      fill: 'green',
      backgroundColor: 'rgba(255, 255, 255, 0.9)'
    })

    // 使用 Group 组合
    const group = new fabric.Group([rect, label], {
      left,
      top,
      selectable: false,
      evented: false,
      data: { annotationIndex: index, type: 'annotation-group' }
    })

    canvas.value!.add(group)
  })
}

// 高亮标注
const highlightAnnotation = (index: number) => {
  highlightedIndex.value = index

  if (!canvas.value) return

  const objects = canvas.value.getObjects()
  const group = objects.find((obj: any) =>
    obj.data?.type === 'annotation-group' && obj.data?.annotationIndex === index
  )

  if (group) {
    const rect = (group as any)._objects[0]
    rect.set({ stroke: 'yellow', strokeWidth: 4 })
    canvas.value.renderAll()
  }
}

const unhighlightAnnotation = () => {
  if (highlightedIndex.value === null || !canvas.value) return

  const objects = canvas.value.getObjects()
  const group = objects.find((obj: any) =>
    obj.data?.type === 'annotation-group' && obj.data?.annotationIndex === highlightedIndex.value
  )

  if (group) {
    const rect = (group as any)._objects[0]
    rect.set({ stroke: 'green', strokeWidth: 2 })
    canvas.value.renderAll()
  }

  highlightedIndex.value = null
}

const locateAnnotation = (index: number) => {
  if (!canvas.value) return

  const objects = canvas.value.getObjects()
  const group = objects.find((obj: any) =>
    obj.data?.type === 'annotation-group' && obj.data?.annotationIndex === index
  )

  if (group) {
    const rect = (group as any)._objects[0]
    const originalStroke = rect.stroke
    rect.set({ stroke: 'yellow', strokeWidth: 4 })
    canvas.value.renderAll()

    setTimeout(() => {
      rect.set({ stroke: originalStroke, strokeWidth: 2 })
      canvas.value?.renderAll()
    }, 2000)

    ElMessage.success('已定位到标注框')
  }
}

// 审核操作
const handleApprove = async () => {
  if (!currentSample.value) return

  try {
    submitting.value = true
    await sampleApi.updateStatus(currentSample.value.id, 'APPROVED')
    ElMessage.success('审核通过')
    await navigateToNext()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const handleReject = async () => {
  if (!currentSample.value) return

  try {
    submitting.value = true
    await sampleApi.updateStatus(currentSample.value.id, 'REJECTED')
    ElMessage.success('已拒绝')
    showRejectDialog.value = false
    rejectReason.value = ''
    await navigateToNext()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const handleReAnnotate = async () => {
  if (!currentSample.value) return

  try {
    await ElMessageBox.confirm(
      '确定要求重新标注吗？样本状态将变为待标注。',
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    submitting.value = true
    await sampleApi.updateStatus(currentSample.value.id, 'PENDING')
    ElMessage.success('已要求重新标注')
    await navigateToNext()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  } finally {
    submitting.value = false
  }
}

// 导航操作
const navigateToPrevious = async () => {
  if (hasPrevious.value) {
    currentIndex.value--
    await loadCurrentSample()
    initCanvas()
  }
}

const navigateToNext = async () => {
  if (hasNext.value) {
    currentIndex.value++
    await loadCurrentSample()
    initCanvas()
  } else {
    ElMessage.info('已经是最后一个样本')
    router.push('/samples/list')
  }
}

const handleSave = async () => {
  await navigateToNext()
}

// 辅助函数
const getStatusType = (status: string | undefined) => {
  const types: Record<string, any> = {
    PENDING: 'info',
    ANNOTATED: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return types[status || 'PENDING']
}

const getStatusText = (status: string | undefined) => {
  const texts: Record<string, string> = {
    PENDING: '待标注',
    ANNOTATED: '已标注',
    APPROVED: '已审核',
    REJECTED: '已拒绝'
  }
  return texts[status || 'PENDING']
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const formatDateTime = (dateTime: string | undefined) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}
</script>

<style scoped>
.review-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.review-content {
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: 20px;
  margin-top: 20px;
}

.canvas-card {
  height: fit-content;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.canvas-wrapper {
  background: #fff;
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 4px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
}

.canvas-controls {
  margin-top: 15px;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 4px;
}

.info-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-card,
.annotations-card,
.action-card {
  height: fit-content;
}

.annotation-list {
  max-height: 300px;
  overflow-y: auto;
}

.annotation-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px;
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 4px;
  transition: all 0.3s;
  cursor: pointer;
}

.annotation-item:hover,
.annotation-item.active {
  background: #f0f9ff;
  border-color: #91d5ff;
}

.annotation-info {
  flex: 1;
}

.annotation-coords {
  margin: 5px 0;
  font-size: 12px;
  color: #999;
}

.annotation-notes {
  margin: 5px 0;
  font-size: 13px;
  color: #666;
}

.review-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.review-actions .el-button {
  width: 100%;
}

.reject-reason {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.reject-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.navigation-bar {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 15px 30px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.navigation-info {
  font-size: 14px;
  color: #666;
  min-width: 80px;
  text-align: center;
}
</style>
