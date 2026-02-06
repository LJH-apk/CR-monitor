<template>
  <div class="admin-container">
    <AdminMenu activeRoute="/admin/behaviors">
      <template #header-extra>
        <el-button type="primary" @click="showDialog = true">
          <el-icon><Plus /></el-icon>
          添加行为
        </el-button>
      </template>
    </AdminMenu>

    <el-card class="list-card">
      <el-table :data="configStore.dangerBehaviors" style="width: 100%">
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="严重等级" width="120">
          <template #default="{ row }">
            <el-rate v-model="row.severityLevel" disabled />
          </template>
        </el-table-column>
        <el-table-column label="颜色" width="100">
          <template #default="{ row }">
            <el-color-picker v-model="row.colorCode" disabled />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.isActive"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="showDialog"
      :title="editingId ? '编辑行为' : '添加行为'"
      width="500px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item label="严重等级">
          <el-rate v-model="form.severityLevel" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="form.colorCode" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useConfigStore } from '@/store/modules/config'
import AdminMenu from '@/components/AdminMenu.vue'
import type { DangerBehavior } from '@/types/alert'

const configStore = useConfigStore()

const showDialog = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  name: '',
  description: '',
  severityLevel: 1,
  colorCode: '#FF0000',
  isActive: true
})

onMounted(() => {
  configStore.fetchDangerBehaviors()
})

const handleEdit = (row: DangerBehavior) => {
  editingId.value = row.id
  Object.assign(form, row)
  showDialog.value = true
}

const handleSave = async () => {
  try {
    if (editingId.value) {
      await configStore.updateDangerBehavior(editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await configStore.createDangerBehavior(form)
      ElMessage.success('添加成功')
    }
    showDialog.value = false
    resetForm()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleStatusChange = async (row: DangerBehavior) => {
  try {
    await configStore.updateDangerBehavior(row.id, { isActive: row.isActive })
    ElMessage.success('状态已更新')
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个行为吗？', '提示', {
      type: 'warning'
    })
    await configStore.deleteDangerBehavior(id)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const resetForm = () => {
  editingId.value = null
  form.name = ''
  form.description = ''
  form.severityLevel = 1
  form.colorCode = '#FF0000'
  form.isActive = true
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
</style>
