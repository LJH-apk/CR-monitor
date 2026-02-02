<template>
  <div class="admin-container">
    <el-page-header @back="goBack" title="返回">
      <template #content>
        <span class="page-title">管理后台</span>
      </template>
    </el-page-header>

    <!-- 导航菜单 -->
    <el-menu
      :default-active="'/admin/models'"
      mode="horizontal"
      :router="true"
      class="admin-menu"
    >
      <el-menu-item index="/admin/videos">视频管理</el-menu-item>
      <el-menu-item index="/admin/behaviors">危险行为管理</el-menu-item>
      <el-menu-item index="/admin/thresholds">告警阈值配置</el-menu-item>
      <el-menu-item index="/admin/models">模型管理</el-menu-item>
      <el-menu-item v-if="authStore.isSuperAdmin()" index="/admin/users">用户管理</el-menu-item>
    </el-menu>

    <!-- 当前模型状态 -->
    <el-row :gutter="20" class="status-row">
      <el-col :span="8">
        <el-card class="status-card">
          <template #header>
            <div class="card-header">
              <span>当前模型</span>
              <el-tag :type="serviceStatus === 'healthy' ? 'success' : 'danger'">
                {{ serviceStatus === 'healthy' ? '服务正常' : '服务异常' }}
              </el-tag>
            </div>
          </template>
          <div class="model-info">
            <p><strong>版本:</strong> {{ currentVersion }}</p>
            <p><strong>名称:</strong> {{ currentModelInfo?.name || '-' }}</p>
            <p><strong>训练样本:</strong> {{ currentModelInfo?.training_samples || 0 }}</p>
            <p><strong>创建时间:</strong> {{ formatTime(currentModelInfo?.created_at) }}</p>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="status-card">
          <template #header>
            <div class="card-header">
              <span>模型指标</span>
            </div>
          </template>
          <div class="metrics-info" v-if="currentModelInfo?.metrics">
            <el-row :gutter="10">
              <el-col :span="12">
                <el-statistic title="mAP50" :value="formatMetric(currentModelInfo.metrics.mAP50)" />
              </el-col>
              <el-col :span="12">
                <el-statistic title="mAP50-95" :value="formatMetric(currentModelInfo.metrics['mAP50-95'])" />
              </el-col>
            </el-row>
            <el-row :gutter="10" style="margin-top: 15px;">
              <el-col :span="12">
                <el-statistic title="精确率" :value="formatMetric(currentModelInfo.metrics.precision)" />
              </el-col>
              <el-col :span="12">
                <el-statistic title="召回率" :value="formatMetric(currentModelInfo.metrics.recall)" />
              </el-col>
            </el-row>
          </div>
          <div v-else class="no-metrics">
            <p>基础模型无训练指标</p>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="status-card training-card">
          <template #header>
            <div class="card-header">
              <span>增量训练</span>
              <el-tag v-if="trainingStatus.is_training" type="warning">训练中</el-tag>
            </div>
          </template>
          <div v-if="trainingStatus.is_training" class="training-progress">
            <p><strong>任务ID:</strong> {{ trainingStatus.task_id }}</p>
            <p><strong>状态:</strong> {{ getTrainingStatusText(trainingStatus.status) }}</p>
            <el-progress
              :percentage="trainingStatus.progress"
              :status="trainingStatus.status === 'completed' ? 'success' : ''"
            />
            <el-button
              type="danger"
              size="small"
              @click="handleCancelTraining"
              style="margin-top: 10px;"
            >
              取消训练
            </el-button>
          </div>
          <div v-else class="start-training">
            <el-form :model="trainingForm" label-width="80px" size="small">
              <el-form-item label="训练轮数">
                <el-input-number v-model="trainingForm.epochs" :min="1" :max="100" />
              </el-form-item>
              <el-form-item label="批次大小">
                <el-input-number v-model="trainingForm.batch_size" :min="1" :max="64" />
              </el-form-item>
              <el-form-item label="自动替换">
                <el-switch v-model="trainingForm.auto_swap" />
              </el-form-item>
            </el-form>
            <el-button type="primary" @click="handleStartTraining" :loading="startingTraining">
              开始增量训练
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 模型版本列表 -->
    <el-card class="versions-card">
      <template #header>
        <div class="card-header">
          <span>模型版本历史</span>
          <el-button type="warning" size="small" @click="handleRollback" :disabled="!canRollback">
            回滚到上一版本
          </el-button>
        </div>
      </template>
      <el-table :data="versionList" style="width: 100%">
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="训练样本" width="100">
          <template #default="{ row }">
            {{ row.training_samples || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="mAP50" width="100">
          <template #default="{ row }">
            {{ formatMetric(row.metrics?.mAP50) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.is_active ? 'success' : 'info'">
              {{ row.is_active ? '使用中' : '未激活' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.created_at) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.is_active"
              type="primary"
              size="small"
              @click="handleSwapModel(row.version)"
            >
              切换
            </el-button>
            <span v-else class="current-label">当前版本</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 训练历史 -->
    <el-card class="history-card">
      <template #header>
        <span>训练历史记录</span>
      </template>
      <el-table :data="trainingHistory" style="width: 100%">
        <el-table-column prop="task_id" label="任务ID" min-width="180" />
        <el-table-column prop="version" label="生成版本" width="100" />
        <el-table-column prop="samples" label="样本数" width="100" />
        <el-table-column prop="epochs" label="训练轮数" width="100" />
        <el-table-column label="mAP50" width="100">
          <template #default="{ row }">
            {{ formatMetric(row.metrics?.mAP50) }}
          </template>
        </el-table-column>
        <el-table-column label="自动替换" width="100">
          <template #default="{ row }">
            <el-tag :type="row.auto_swapped ? 'success' : 'info'" size="small">
              {{ row.auto_swapped ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="timestamp" label="训练时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.timestamp) }}
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="trainingHistory.length === 0" description="暂无训练记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/store/modules/auth'
import { aiApi, type ModelVersion, type TrainingStatus, type TrainingHistory } from '@/api/ai'

const router = useRouter()
const authStore = useAuthStore()

// 状态
const serviceStatus = ref<'healthy' | 'error'>('error')
const currentVersion = ref('v0')
const versions = ref<Record<string, ModelVersion>>({})
const trainingStatus = ref<TrainingStatus>({
  is_training: false,
  progress: 0,
  status: 'idle',
  task_id: null
})
const trainingHistory = ref<TrainingHistory[]>([])
const startingTraining = ref(false)

// 训练表单
const trainingForm = reactive({
  epochs: 10,
  batch_size: 16,
  auto_swap: true
})

// 轮询定时器
let statusTimer: number | null = null

// 计算属性
const currentModelInfo = computed(() => {
  return versions.value[currentVersion.value]
})

const versionList = computed(() => {
  return Object.entries(versions.value)
    .map(([version, info]) => ({ version, ...info }))
    .sort((a, b) => {
      const numA = parseInt(a.version.replace('v', ''))
      const numB = parseInt(b.version.replace('v', ''))
      return numB - numA
    })
})

const canRollback = computed(() => {
  const versionNum = parseInt(currentVersion.value.replace('v', ''))
  return versionNum > 0
})

// 方法
const goBack = () => {
  router.push('/dashboard')
}

const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const formatMetric = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return (value * 100).toFixed(1) + '%'
}

const getTrainingStatusText = (status: string) => {
  const map: Record<string, string> = {
    idle: '空闲',
    preparing: '准备中',
    fetching_samples: '获取样本',
    preparing_dataset: '准备数据集',
    loading_model: '加载模型',
    training: '训练中',
    registering: '注册版本',
    swapping: '热替换中',
    completed: '已完成',
    failed: '失败',
    cancelling: '取消中'
  }
  if (status.startsWith('failed:')) {
    return '失败: ' + status.substring(7)
  }
  return map[status] || status
}

// 加载数据
const loadServiceHealth = async () => {
  try {
    const res = await aiApi.health()
    serviceStatus.value = res.data.status === 'healthy' ? 'healthy' : 'error'
  } catch {
    serviceStatus.value = 'error'
  }
}

const loadModelVersions = async () => {
  try {
    const res = await aiApi.getModelVersions()
    if (res.data.code === 200) {
      currentVersion.value = res.data.data.current_version
      versions.value = res.data.data.versions
    }
  } catch (error) {
    console.error('加载模型版本失败:', error)
  }
}

const loadTrainingStatus = async () => {
  try {
    const res = await aiApi.getTrainingStatus()
    if (res.data.code === 200) {
      trainingStatus.value = res.data.data

      // 训练完成后刷新版本列表
      if (res.data.data.status === 'completed' && !res.data.data.is_training) {
        loadModelVersions()
        loadTrainingHistory()
      }
    }
  } catch (error) {
    console.error('获取训练状态失败:', error)
  }
}

const loadTrainingHistory = async () => {
  try {
    const res = await aiApi.getTrainingHistory()
    if (res.data.code === 200) {
      trainingHistory.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取训练历史失败:', error)
  }
}

// 操作
const handleStartTraining = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要开始增量训练吗？训练将使用已批准的样本数据。',
      '确认训练',
      { type: 'info' }
    )

    startingTraining.value = true
    const res = await aiApi.startTraining({
      epochs: trainingForm.epochs,
      batch_size: trainingForm.batch_size,
      auto_swap: trainingForm.auto_swap
    })

    if (res.data.code === 200) {
      ElMessage.success('训练任务已启动')
      loadTrainingStatus()
    } else {
      ElMessage.error(res.data.message || '启动训练失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '启动训练失败')
    }
  } finally {
    startingTraining.value = false
  }
}

const handleCancelTraining = async () => {
  try {
    await ElMessageBox.confirm('确定要取消当前训练任务吗？', '确认取消', { type: 'warning' })
    const res = await aiApi.cancelTraining()
    if (res.data.code === 200) {
      ElMessage.success('取消请求已发送')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('取消训练失败')
    }
  }
}

const handleSwapModel = async (version: string) => {
  try {
    await ElMessageBox.confirm(
      `确定要切换到模型版本 ${version} 吗？切换后将立即生效。`,
      '确认切换',
      { type: 'warning' }
    )

    const res = await aiApi.swapModel(version)
    if (res.data.code === 200) {
      ElMessage.success(`已切换到版本 ${version}`)
      loadModelVersions()
    } else {
      ElMessage.error(res.data.message || '切换失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('模型切换失败')
    }
  }
}

const handleRollback = async () => {
  try {
    await ElMessageBox.confirm('确定要回滚到上一个模型版本吗？', '确认回滚', { type: 'warning' })

    const res = await aiApi.rollbackModel()
    if (res.data.code === 200) {
      ElMessage.success('模型已回滚')
      loadModelVersions()
    } else {
      ElMessage.error(res.data.message || '回滚失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('模型回滚失败')
    }
  }
}

// 生命周期
onMounted(() => {
  loadServiceHealth()
  loadModelVersions()
  loadTrainingStatus()
  loadTrainingHistory()

  // 定时刷新训练状态
  statusTimer = window.setInterval(() => {
    loadTrainingStatus()
  }, 3000)
})

onUnmounted(() => {
  if (statusTimer) {
    clearInterval(statusTimer)
  }
})
</script>

<style scoped>
.admin-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.admin-menu {
  margin: 20px 0;
  background: white;
  border-radius: 4px;
}

.status-row {
  margin-bottom: 20px;
}

.status-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.model-info p {
  margin: 8px 0;
  color: #606266;
}

.metrics-info {
  padding: 10px 0;
}

.no-metrics {
  text-align: center;
  color: #909399;
  padding: 30px 0;
}

.training-card {
  min-height: 280px;
}

.training-progress {
  padding: 10px 0;
}

.training-progress p {
  margin: 8px 0;
  color: #606266;
}

.start-training {
  padding: 10px 0;
}

.versions-card {
  margin-bottom: 20px;
}

.history-card {
  margin-bottom: 20px;
}

.current-label {
  color: #67c23a;
  font-size: 12px;
}
</style>
