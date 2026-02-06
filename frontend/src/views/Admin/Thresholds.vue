<template>
  <div class="admin-container">
    <AdminMenu activeRoute="/admin/thresholds" />

    <!-- 新增：说明卡片 -->
    <el-alert
      title="置信度阈值说明"
      type="info"
      :closable="false"
      style="margin: 20px 0;"
    >
      <p>置信度阈值决定了AI检测结果是否触发预警。</p>
      <p>• 阈值越高，预警越准确但可能遗漏；阈值越低，预警越灵敏但可能误报。</p>
      <p>• 建议根据实际场景调整，一般设置在60%-80%之间。</p>
    </el-alert>

    <el-card class="list-card">
      <el-table :data="configStore.thresholds" style="width: 100%">
        <el-table-column label="危险行为" width="150">
          <template #default="{ row }">
            {{ configStore.getBehaviorName(row.dangerBehaviorId) }}
          </template>
        </el-table-column>
        <el-table-column label="置信度阈值" width="300">
          <template #default="{ row }">
            <div class="threshold-slider-container">
              <el-slider
                v-model="row.confidenceThreshold"
                :min="0"
                :max="1"
                :step="0.01"
                :format-tooltip="(val: number) => (val * 100).toFixed(0) + '%'"
                @change="handleThresholdChange(row)"
              />
              <span class="threshold-value">
                {{ (row.confidenceThreshold * 100).toFixed(0) }}%
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="timeWindowSeconds" label="时间窗口(秒)" width="150" />
        <el-table-column prop="maxAlertsPerWindow" label="最大预警数" width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.isActive"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="showDialog" title="编辑阈值" width="500px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="置信度阈值">
          <el-slider
            v-model="form.confidenceThreshold"
            :min="0"
            :max="1"
            :step="0.01"
            :format-tooltip="(val: number) => (val * 100).toFixed(0) + '%'"
          />
        </el-form-item>
        <el-form-item label="时间窗口(秒)">
          <el-input-number v-model="form.timeWindowSeconds" :min="1" />
        </el-form-item>
        <el-form-item label="最大预警数">
          <el-input-number v-model="form.maxAlertsPerWindow" :min="1" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.isActive" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useConfigStore } from '@/store/modules/config'
import AdminMenu from '@/components/AdminMenu.vue'
import type { AlertThreshold } from '@/types/alert'

const configStore = useConfigStore()

const showDialog = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  confidenceThreshold: 0.7,
  timeWindowSeconds: 60,
  maxAlertsPerWindow: 10,
  isActive: true
})

onMounted(async () => {
  await configStore.fetchDangerBehaviors()
  await configStore.fetchThresholds()
})

const handleEdit = (row: AlertThreshold) => {
  editingId.value = row.id
  Object.assign(form, row)
  showDialog.value = true
}

const handleSave = async () => {
  if (!editingId.value) return

  try {
    await configStore.updateThreshold(editingId.value, form)
    ElMessage.success('更新成功')
    showDialog.value = false
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const handleThresholdChange = async (row: AlertThreshold) => {
  try {
    await configStore.updateThreshold(row.id, {
      confidenceThreshold: row.confidenceThreshold
    })
    ElMessage.success('阈值已更新')
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const handleStatusChange = async (row: AlertThreshold) => {
  try {
    await configStore.updateThreshold(row.id, { isActive: row.isActive })
    ElMessage.success('状态已更新')
  } catch (error) {
    ElMessage.error('更新失败')
  }
}
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

.list-card {
  margin-top: 20px;
}

.threshold-slider-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.threshold-value {
  min-width: 45px;
  font-size: 14px;
  font-weight: 600;
  color: #409EFF;
}
</style>
