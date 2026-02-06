<template>
  <div class="admin-container">
    <AdminMenu activeRoute="/admin/users">
      <template #header-extra>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          创建用户
        </el-button>
      </template>
    </AdminMenu>

    <el-card class="list-card">
      <el-table :data="users" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column label="角色" width="150">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)">
              {{ getRoleText(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="200">
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

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchUsers"
        @current-change="fetchUsers"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>

    <!-- 创建用户对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建用户" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="createForm.password" type="password" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
            <el-option label="超级管理员" value="SUPER_ADMIN" />
            <el-option label="开发者" value="DEVELOPER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑用户" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="editForm.password"
            type="password"
            placeholder="留空则不修改"
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
            <el-option label="超级管理员" value="SUPER_ADMIN" />
            <el-option label="开发者" value="DEVELOPER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { userApi, type UserDTO } from '@/api/user'
import AdminMenu from '@/components/AdminMenu.vue'

const users = ref<UserDTO[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const editingId = ref<number | null>(null)

const createForm = reactive({
  username: '',
  password: '',
  email: '',
  role: 'USER'
})

const editForm = reactive({
  username: '',
  password: '',
  email: '',
  role: ''
})

onMounted(() => {
  fetchUsers()
})

const fetchUsers = async () => {
  loading.value = true
  try {
    const response = await userApi.getAll(
      currentPage.value - 1,
      pageSize.value,
      'id',
      'DESC'
    )
    users.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = async () => {
  if (!createForm.username || !createForm.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }

  try {
    await userApi.create(createForm)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    resetCreateForm()
    fetchUsers()
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const handleEdit = (row: UserDTO) => {
  editingId.value = row.id
  editForm.username = row.username
  editForm.password = ''
  editForm.email = row.email || ''
  editForm.role = row.role
  showEditDialog.value = true
}

const handleUpdate = async () => {
  if (!editingId.value) return

  try {
    const data: any = {
      username: editForm.username,
      email: editForm.email,
      role: editForm.role
    }
    if (editForm.password) {
      data.password = editForm.password
    }

    await userApi.update(editingId.value, data)
    ElMessage.success('更新成功')
    showEditDialog.value = false
    fetchUsers()
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个用户吗？', '提示', {
      type: 'warning'
    })
    await userApi.delete(id)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const resetCreateForm = () => {
  createForm.username = ''
  createForm.password = ''
  createForm.email = ''
  createForm.role = 'USER'
}

const getRoleType = (role: string) => {
  const map: Record<string, any> = {
    USER: 'info',
    ADMIN: 'warning',
    SUPER_ADMIN: 'danger',
    DEVELOPER: 'success'
  }
  return map[role] || 'info'
}

const getRoleText = (role: string) => {
  const map: Record<string, string> = {
    USER: '普通用户',
    ADMIN: '管理员',
    SUPER_ADMIN: '超级管理员',
    DEVELOPER: '开发者'
  }
  return map[role] || role
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
