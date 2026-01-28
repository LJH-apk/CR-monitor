<template>
  <div class="annotation-container">
    <el-page-header @back="goBack" title="返回">
      <template #content>
        <span class="page-title">标注训练样本</span>
      </template>
      <template #extra>
        <el-button type="primary" @click="handleSave" :loading="saving">
          保存标注
        </el-button>
      </template>
    </el-page-header>

    <div class="annotation-content">
      <!-- 左侧：画布区域 -->
      <el-card class="canvas-card">
        <template #header>
          <div class="card-header">
            <span>图片标注</span>
            <el-button size="small" @click="clearCurrentRect">清除当前框</el-button>
          </div>
        </template>
        <div class="canvas-wrapper">
          <canvas ref="canvasRef" id="annotation-canvas"></canvas>
        </div>
        <div class="canvas-tips">
          <p>操作提示：</p>
          <ul>
            <li>按住鼠标左键拖拽绘制矩形框</li>
            <li>点击已有矩形框可以选中并移动</li>
            <li>选中矩形框后可以拖动边角调整大小</li>
          </ul>
        </div>
      </el-card>

      <!-- 右侧：标注列表 -->
      <el-card class="annotations-card">
        <template #header>
          <span>标注列表 ({{ annotations.length }})</span>
        </template>

        <!-- 当前标注框设置 -->
        <div v-if="currentRect" class="current-annotation">
          <h4>当前标注框</h4>
          <el-form label-width="100px" size="small">
            <el-form-item label="危险行为">
              <el-select v-model="currentBehaviorId" placeholder="选择危险行为">
                <el-option
                  v-for="behavior in behaviors"
                  :key="behavior.id"
                  :label="behavior.name"
                  :value="behavior.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="备注">
              <el-input
                v-model="currentNotes"
                type="textarea"
                :rows="2"
                placeholder="可选"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" @click="addAnnotation">
                添加到列表
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 已添加的标注列表 -->
        <div class="annotation-list">
          <div
            v-for="(ann, index) in annotations"
            :key="index"
            class="annotation-item"
          >
            <div class="annotation-info">
              <strong>{{ getBehaviorName(ann.dangerBehaviorId) }}</strong>
              <p class="annotation-coords">
                位置: ({{ ann.xMin }}, {{ ann.yMin }}) - ({{ ann.xMax }}, {{ ann.yMax }})
              </p>
              <p v-if="ann.notes" class="annotation-notes">{{ ann.notes }}</p>
            </div>
            <el-button
              type="danger"
              size="small"
              @click="removeAnnotation(index)"
            >
              删除
            </el-button>
          </div>
          <el-empty v-if="annotations.length === 0" description="暂无标注" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Canvas, Rect, FabricImage } from 'fabric'
import { sampleApi } from '@/api/sample'
import { dangerBehaviorApi } from '@/api/config'
import type { Annotation } from '@/types/sample'
import type { DangerBehavior } from '@/types/alert'

const route = useRoute()
const router = useRouter()

const sampleId = ref<number>(Number(route.params.id))
const canvasRef = ref<HTMLCanvasElement | null>(null)
const canvas = ref<Canvas | null>(null)
const currentRect = ref<Rect | null>(null)
const isDrawing = ref(false)
const startX = ref(0)
const startY = ref(0)

const behaviors = ref<DangerBehavior[]>([])
const currentBehaviorId = ref<number | null>(null)
const currentNotes = ref('')
const annotations = ref<Annotation[]>([])
const saving = ref(false)

const imageWidth = ref(0)
const imageHeight = ref(0)
const currentSample = ref<any>(null)
const canvasScale = ref(1) // 画布缩放比例

onMounted(async () => {
  await loadBehaviors()
  await loadSample()
  initCanvas()
})

onUnmounted(() => {
  if (canvas.value) {
    canvas.value.dispose()
  }
})

const goBack = () => {
  router.push('/samples/list')
}

const loadBehaviors = async () => {
  try {
    const response = await dangerBehaviorApi.getAll()
    behaviors.value = response.data.filter(b => b.isActive)
  } catch (error) {
    ElMessage.error('加载危险行为列表失败')
  }
}

const loadSample = async () => {
  try {
    const response = await sampleApi.getById(sampleId.value)
    const sample = response.data
    currentSample.value = sample
    imageWidth.value = sample.imageWidth
    imageHeight.value = sample.imageHeight

    // 加载已有标注
    if (sample.annotations && sample.annotations.length > 0) {
      annotations.value = sample.annotations
    }
  } catch (error) {
    ElMessage.error('加载样本失败')
  }
}

const initCanvas = () => {
  if (!canvasRef.value || !currentSample.value) return

  // 加载图片 - 使用正确的路径
  const imgUrl = `/api/storage/training-samples/${currentSample.value.userId}/${currentSample.value.storedFilename}`

  // 先加载图片获取尺寸，然后创建匹配的画布
  FabricImage.fromURL(imgUrl).then((img) => {
    if (!canvasRef.value) return

    // 获取图片原始尺寸
    const imgWidth = img.width || 800
    const imgHeight = img.height || 600

    // 设置最大画布尺寸限制（考虑容器宽度）
    const maxWidth = 900
    const maxHeight = 700

    // 计算画布尺寸（如果图片超过最大尺寸，按比例缩小）
    let canvasWidth = imgWidth
    let canvasHeight = imgHeight
    let scale = 1

    if (imgWidth > maxWidth || imgHeight > maxHeight) {
      scale = Math.min(maxWidth / imgWidth, maxHeight / imgHeight)
      canvasWidth = imgWidth * scale
      canvasHeight = imgHeight * scale
    }

    // 保存缩放比例，用于计算标注坐标
    canvasScale.value = scale

    // 创建与图片尺寸匹配的画布
    canvas.value = new Canvas('annotation-canvas', {
      width: canvasWidth,
      height: canvasHeight,
      backgroundColor: '#f0f0f0'
    })

    // 设置图片缩放和位置
    img.scale(scale)
    img.set({
      left: 0,
      top: 0,
      originX: 'left',
      originY: 'top',
      selectable: false
    })

    // 将图片添加到画布
    canvas.value.add(img)
    canvas.value.sendObjectToBack(img)
    canvas.value.renderAll()

    // 绑定鼠标事件
    canvas.value.on('mouse:down', handleMouseDown)
    canvas.value.on('mouse:move', handleMouseMove)
    canvas.value.on('mouse:up', handleMouseUp)
  }).catch((error) => {
    console.error('Failed to load image:', error)
    ElMessage.error('图片加载失败')
  })
}

const handleMouseDown = (e: any) => {
  if (!canvas.value) return

  const pointer = canvas.value.getViewportPoint(e.e)
  isDrawing.value = true
  startX.value = pointer.x
  startY.value = pointer.y

  // 创建新矩形
  currentRect.value = new Rect({
    left: startX.value,
    top: startY.value,
    width: 0,
    height: 0,
    fill: 'rgba(255, 0, 0, 0.2)',
    stroke: 'red',
    strokeWidth: 2,
    selectable: true
  })

  canvas.value.add(currentRect.value)
  canvas.value.bringObjectToFront(currentRect.value)
}

const handleMouseMove = (e: any) => {
  if (!isDrawing.value || !currentRect.value || !canvas.value) return

  const pointer = canvas.value.getViewportPoint(e.e)
  const width = pointer.x - startX.value
  const height = pointer.y - startY.value

  currentRect.value.set({
    width: Math.abs(width),
    height: Math.abs(height),
    left: width > 0 ? startX.value : pointer.x,
    top: height > 0 ? startY.value : pointer.y
  })

  canvas.value.renderAll()
}

const handleMouseUp = () => {
  isDrawing.value = false

  // 如果矩形太小，删除它
  if (currentRect.value && (currentRect.value.width! < 10 || currentRect.value.height! < 10)) {
    canvas.value?.remove(currentRect.value)
    currentRect.value = null
  }
}

const clearCurrentRect = () => {
  if (currentRect.value && canvas.value) {
    canvas.value.remove(currentRect.value)
    currentRect.value = null
    currentBehaviorId.value = null
    currentNotes.value = ''
  }
}

const addAnnotation = () => {
  if (!currentRect.value) {
    ElMessage.warning('请先绘制标注框')
    return
  }

  if (!currentBehaviorId.value) {
    ElMessage.warning('请选择危险行为类型')
    return
  }

  // 计算实际坐标（考虑缩放）
  const scale = canvasScale.value
  const xMin = Math.round((currentRect.value.left || 0) / scale)
  const yMin = Math.round((currentRect.value.top || 0) / scale)
  const xMax = Math.round(((currentRect.value.left || 0) + (currentRect.value.width || 0)) / scale)
  const yMax = Math.round(((currentRect.value.top || 0) + (currentRect.value.height || 0)) / scale)

  const annotation: Annotation = {
    sampleId: sampleId.value,
    dangerBehaviorId: currentBehaviorId.value,
    xMin,
    yMin,
    xMax,
    yMax,
    notes: currentNotes.value
  }

  annotations.value.push(annotation)
  ElMessage.success('标注已添加')

  // 清除当前矩形
  clearCurrentRect()
}

const removeAnnotation = (index: number) => {
  annotations.value.splice(index, 1)
  ElMessage.success('标注已删除')
}

const getBehaviorName = (behaviorId: number): string => {
  const behavior = behaviors.value.find(b => b.id === behaviorId)
  return behavior?.name || '未知'
}

const handleSave = async () => {
  if (annotations.value.length === 0) {
    ElMessage.warning('请至少添加一个标注')
    return
  }

  saving.value = true
  try {
    // 保存所有标注
    for (const annotation of annotations.value) {
      if (!annotation.id) {
        await sampleApi.saveAnnotation(sampleId.value, annotation)
      }
    }

    ElMessage.success('标注保存成功')
    router.push('/samples/list')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.annotation-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.annotation-content {
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

.canvas-tips {
  margin-top: 15px;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 4px;
  font-size: 14px;
}

.canvas-tips p {
  margin: 0 0 10px 0;
  font-weight: 600;
}

.canvas-tips ul {
  margin: 0;
  padding-left: 20px;
}

.canvas-tips li {
  margin: 5px 0;
  color: #666;
}

.annotations-card {
  height: fit-content;
}

.current-annotation {
  padding: 15px;
  background: #f0f9ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  margin-bottom: 20px;
}

.current-annotation h4 {
  margin: 0 0 15px 0;
  font-size: 14px;
  color: #1890ff;
}

.annotation-list {
  max-height: 500px;
  overflow-y: auto;
}

.annotation-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 15px;
  margin-bottom: 10px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 4px;
}

.annotation-info {
  flex: 1;
}

.annotation-info strong {
  display: block;
  margin-bottom: 5px;
  color: #333;
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
</style>
