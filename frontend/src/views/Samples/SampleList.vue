<template>
  <div class="sample-list-container">
    <el-page-header @back="goBack" title="返回">
      <template #content>
        <span class="page-title">训练样本列表</span>
      </template>
      <template #extra>
        <el-button type="primary" @click="router.push('/samples/upload')">
          上传样本
        </el-button>
      </template>
    </el-page-header>

    <!-- 筛选器 -->
    <el-card class="filter-card">
      <el-radio-group v-model="statusFilter" @change="loadSamples">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="PENDING">待标注</el-radio-button>
        <el-radio-button label="ANNOTATED">已标注</el-radio-button>
        <el-radio-button label="APPROVED">已审核</el-radio-button>
        <el-radio-button label="REJECTED">已拒绝</el-radio-button>
      </el-radio-group>
    </el-card>

    <!-- 样本列表 -->
    <div class="samples-grid" v-loading="loading">
      <el-card
        v-for="sample in samples"
        :key="sample.id"
        class="sample-card"
        :body-style="{ padding: '0' }"
      >
        <div class="sample-image-wrapper">
          <img
            :src="getSampleImageUrl(sample)"
            :alt="sample.originalFilename"
            class="sample-image"
            @click="viewSample(sample)"
          />
          <el-tag :type="getStatusType(sample.status)" class="status-tag">
            {{ getStatusText(sample.status) }}
          </el-tag>
        </div>
        <div class="sample-info">
          <h4 class="sample-filename">{{ sample.originalFilename }}</h4>
          <p class="sample-meta">
            <span>{{ sample.imageWidth }} x {{ sample.imageHeight }}</span>
            <span>{{ formatFileSize(sample.fileSize) }}</span>
          </p>
          <p class="sample-meta">
            <span>上传者: {{ sample.username || '未知' }}</span>
          </p>
          <p class="sample-meta">
            <span>标注数: {{ sample.annotations?.length || 0 }}</span>
          </p>
          <div class="sample-actions">
            <el-button
              v-if="sample.status === 'PENDING'"
              size="small"
              type="primary"
              @click="annotateSample(sample.id)"
            >
              开始标注
            </el-button>
            <el-button
              v-else
              size="small"
              @click="viewSample(sample)"
            >
              查看详情
            </el-button>
            <el-dropdown v-if="authStore.isAdmin()" @command="handleCommand">
              <el-button size="small">
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    :command="{ action: 'approve', id: sample.id }"
                    v-if="sample.status === 'ANNOTATED'"
                  >
                    审核通过
                  </el-dropdown-item>
                  <el-dropdown-item
                    :command="{ action: 'reject', id: sample.id }"
                    v-if="sample.status === 'ANNOTATED'"
                  >
                    拒绝
                  </el-dropdown-item>
                  <el-dropdown-item
                    :command="{ action: 'delete', id: sample.id }"
                    divided
                  >
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button
              v-else
              size="small"
              type="danger"
              @click="deleteSample(sample.id)"
            >
              删除
            </el-button>
          </div>
        </div>
      </el-card>

      <el-empty v-if="samples.length === 0 && !loading" description="暂无样本" />
    </div>

    <!-- 导出按钮（仅管理员） -->
    <el-card v-if="authStore.isAdmin()" class="export-card">
      <el-button type="success" @click="exportCoco" :loading="exporting">
        导出COCO JSON格式
      </el-button>
      <span class="export-tip">仅导出已审核通过的样本</span>
    </el-card>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="样本详情" width="800px">
      <div v-if="currentSample">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文件名">
            {{ currentSample.originalFilename }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentSample.status)">
              {{ getStatusText(currentSample.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="图片尺寸">
            {{ currentSample.imageWidth }} x {{ currentSample.imageHeight }}
          </el-descriptions-item>
          <el-descriptions-item label="文件大小">
            {{ formatFileSize(currentSample.fileSize) }}
          </el-descriptions-item>
          <el-descriptions-item label="上传者">
            {{ currentSample.username || '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="上传时间">
            {{ currentSample.uploadTime }}
          </el-descriptions-item>
        </el-descriptions>

        <h4 style="margin-top: 20px">标注列表 ({{ currentSample.annotations?.length || 0 }})</h4>
        <el-table :data="currentSample.annotations" style="width: 100%; margin-top: 10px">
          <el-table-column prop="dangerBehaviorName" label="危险行为" width="150" />
          <el-table-column label="位置" width="200">
            <template #default="{ row }">
              ({{ row.xMin }}, {{ row.yMin }}) - ({{ row.xMax }}, {{ row.yMax }})
            </template>
          </el-table-column>
          <el-table-column prop="notes" label="备注" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { sampleApi } from '@/api/sample'
import { useAuthStore } from '@/store/modules/auth'
import type { TrainingSample } from '@/types/sample'

const router = useRouter()
const authStore = useAuthStore()

const samples = ref<TrainingSample[]>([])
const loading = ref(false)
const statusFilter = ref('')
const exporting = ref(false)

const showDetailDialog = ref(false)
const currentSample = ref<TrainingSample | null>(null)

onMounted(() => {
  loadSamples()
})

const goBack = () => {
  router.push('/dashboard')
}

const loadSamples = async () => {
  loading.value = true
  try {
    const response = await sampleApi.getAll(statusFilter.value || undefined)
    samples.value = response.data
  } catch (error) {
    ElMessage.error('加载样本列表失败')
  } finally {
    loading.value = false
  }
}

const getSampleImageUrl = (sample: TrainingSample): string => {
  return `/api/storage/training-samples/${sample.userId}/${sample.storedFilename}`
}

const annotateSample = (id: number) => {
  router.push(`/samples/annotate/${id}`)
}

const viewSample = async (sample: TrainingSample) => {
  try {
    const response = await sampleApi.getById(sample.id)
    currentSample.value = response.data
    showDetailDialog.value = true
  } catch (error) {
    ElMessage.error('加载样本详情失败')
  }
}

const handleCommand = async (command: { action: string; id: number }) => {
  const { action, id } = command

  if (action === 'approve') {
    await updateStatus(id, 'APPROVED')
  } else if (action === 'reject') {
    await updateStatus(id, 'REJECTED')
  } else if (action === 'delete') {
    await deleteSample(id)
  }
}

const updateStatus = async (id: number, status: string) => {
  try {
    await sampleApi.updateStatus(id, status)
    ElMessage.success('状态已更新')
    loadSamples()
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const deleteSample = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个样本吗？', '提示', {
      type: 'warning'
    })
    await sampleApi.delete(id)
    ElMessage.success('删除成功')
    loadSamples()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const exportCoco = async () => {
  exporting.value = true
  try {
    const response = await sampleApi.exportCoco()
    const data = JSON.stringify(response.data, null, 2)
    const blob = new Blob([data], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `coco_export_${Date.now()}.json`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const getStatusType = (status: string) => {
  const map: Record<string, any> = {
    PENDING: 'info',
    ANNOTATED: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '待标注',
    ANNOTATED: '已标注',
    APPROVED: '已审核',
    REJECTED: '已拒绝'
  }
  return map[status] || status
}

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}
</script>

<style scoped>
.sample-list-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.filter-card {
  margin: 20px 0;
}

.samples-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.sample-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.sample-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.sample-image-wrapper {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: #f0f0f0;
}

.sample-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-tag {
  position: absolute;
  top: 10px;
  right: 10px;
}

.sample-info {
  padding: 15px;
}

.sample-filename {
  margin: 0 0 10px 0;
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sample-meta {
  margin: 5px 0;
  font-size: 12px;
  color: #999;
  display: flex;
  justify-content: space-between;
}

.sample-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.export-card {
  margin-top: 20px;
  text-align: center;
}

.export-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #999;
}
</style>
