<template>
  <div class="system-logs">
    <el-page-header @back="$router.push('/dashboard')" title="返回">
      <template #content>
        <span class="page-title">系统日志</span>
      </template>
    </el-page-header>

    <el-menu :default-active="'/developer/logs'" mode="horizontal" :router="true" style="margin: 20px 0">
      <el-menu-item index="/admin/videos">视频管理</el-menu-item>
      <el-menu-item index="/admin/behaviors">危险行为管理</el-menu-item>
      <el-menu-item index="/admin/thresholds">告警阈值配置</el-menu-item>
      <el-menu-item index="/admin/models">模型管理</el-menu-item>
      <el-menu-item v-if="authStore.isSuperAdmin() || authStore.isDeveloper()" index="/admin/users">用户管理</el-menu-item>
      <el-menu-item v-if="authStore.isDeveloper()" index="/developer/logs">系统日志</el-menu-item>
    </el-menu>

    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="4">
        <el-card shadow="hover">
          <el-statistic title="总日志数" :value="stats.totalCount" />
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <el-statistic title="错误日志" :value="stats.levelCounts?.ERROR || 0">
            <template #suffix>
              <el-tag type="danger" size="small">ERROR</el-tag>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <el-statistic title="警告日志" :value="stats.levelCounts?.WARN || 0">
            <template #suffix>
              <el-tag type="warning" size="small">WARN</el-tag>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <el-statistic title="登录成功" :value="stats.loginSuccessCount || 0">
            <template #suffix>
              <el-tag type="success" size="small">次</el-tag>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <el-statistic title="登录失败" :value="stats.loginFailCount || 0">
            <template #suffix>
              <el-tag type="warning" size="small">次</el-tag>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <el-statistic title="用户登出" :value="stats.logoutCount || 0">
            <template #suffix>
              <el-tag type="info" size="small">次</el-tag>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card style="margin-bottom: 20px">
      <el-form :inline="true" :model="filters">
        <el-form-item label="级别">
          <el-select v-model="filters.level" placeholder="全部" clearable style="width: 120px">
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filters.type" placeholder="全部" clearable style="width: 120px">
            <el-option label="登录" value="LOGIN" />
            <el-option label="登出" value="LOGOUT" />
            <el-option label="上传" value="UPLOAD" />
            <el-option label="删除" value="DELETE" />
            <el-option label="用户管理" value="USER_MGMT" />
            <el-option label="配置管理" value="CONFIG" />
            <el-option label="错误" value="ERROR" />
            <el-option label="操作" value="OPERATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="搜索消息或用户名" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchLogs">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志表格 -->
    <el-card>
      <el-table
        :data="logs"
        v-loading="loading"
        style="width: 100%"
        row-key="id"
        :expand-row-keys="expandedRows"
        @expand-change="handleExpandChange"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="log-expand-content">
              <div class="log-details" v-if="row.details">
                <el-text type="info" tag="strong">详细信息:</el-text>
                <pre>{{ row.details }}</pre>
              </div>
              <div class="log-details" v-if="row.requestUri">
                <el-text type="info" tag="strong">请求路径:</el-text>
                <span>{{ row.requestMethod }} {{ row.requestUri }}</span>
              </div>
              <div class="log-details" v-if="row.userAgent">
                <el-text type="info" tag="strong">浏览器信息:</el-text>
                <span>{{ row.userAgent }}</span>
              </div>
              <div class="log-details" v-if="row.userId">
                <el-text type="info" tag="strong">用户ID:</el-text>
                <span>{{ row.userId }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="级别" width="90">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)" size="small">
              {{ row.level }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)" size="small" effect="plain">
              {{ getTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="message" label="消息" min-width="200" show-overflow-tooltip />

        <el-table-column label="用户" width="120">
          <template #default="{ row }">
            <span v-if="row.username">{{ row.username }}</span>
            <el-text v-else type="info">-</el-text>
          </template>
        </el-table-column>

        <el-table-column label="IP / 位置" width="160">
          <template #default="{ row }">
            <div v-if="row.ipAddress">
              <div>{{ row.ipAddress }}</div>
              <el-text type="info" size="small">{{ row.location || '未知' }}</el-text>
            </div>
            <el-text v-else type="info">-</el-text>
          </template>
        </el-table-column>

        <el-table-column label="登录状态" width="100">
          <template #default="{ row }">
            <template v-if="row.type === 'LOGIN'">
              <el-tag v-if="row.loginSuccess" type="success" size="small">成功</el-tag>
              <el-tag v-else type="danger" size="small">失败</el-tag>
            </template>
            <template v-else-if="row.loginSuccess === true">
              <el-tag type="success" size="small">已登录</el-tag>
            </template>
            <el-text v-else type="info">-</el-text>
          </template>
        </el-table-column>

        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchLogs"
        @current-change="fetchLogs"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { logApi, type SystemLog, type LogStats } from '@/api/log'
import { useAuthStore } from '@/store/modules/auth'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()

const logs = ref<SystemLog[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const expandedRows = ref<number[]>([])
const dateRange = ref<[string, string] | null>(null)

const stats = ref<LogStats>({
  levelCounts: {},
  loginSuccessCount: 0,
  loginFailCount: 0,
  totalCount: 0
})

const filters = reactive({
  level: '',
  type: '',
  keyword: ''
})

const fetchLogs = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: currentPage.value - 1,
      size: pageSize.value,
      sortBy: 'createdAt',
      sortDir: 'DESC'
    }

    if (filters.level) params.level = filters.level
    if (filters.type) params.type = filters.type
    if (filters.keyword) params.keyword = filters.keyword
    if (dateRange.value) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }

    const response = await logApi.getLogs(params)
    logs.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    ElMessage.error('获取日志列表失败')
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const response = await logApi.getStats(24)
    stats.value = response.data
  } catch (error) {
    console.error('获取统计信息失败', error)
  }
}

const resetFilters = () => {
  filters.level = ''
  filters.type = ''
  filters.keyword = ''
  dateRange.value = null
  currentPage.value = 1
  fetchLogs()
}

const handleExpandChange = (row: SystemLog, expandedRowsList: SystemLog[]) => {
  expandedRows.value = expandedRowsList.map(r => r.id)
}

const getLevelType = (level: string) => {
  const map: Record<string, string> = {
    DEBUG: 'info',
    INFO: 'success',
    WARN: 'warning',
    ERROR: 'danger'
  }
  return map[level] || 'info'
}

const getTypeTagType = (type: string) => {
  const map: Record<string, string> = {
    LOGIN: '',
    LOGOUT: 'info',
    UPLOAD: 'success',
    DELETE: 'danger',
    USER_MGMT: 'warning',
    CONFIG: '',
    ERROR: 'danger',
    OPERATION: 'warning',
    SYSTEM: 'info'
  }
  return map[type] || ''
}

const getTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    LOGIN: '登录',
    LOGOUT: '登出',
    UPLOAD: '上传',
    DELETE: '删除',
    USER_MGMT: '用户管理',
    CONFIG: '配置管理',
    ERROR: '错误',
    OPERATION: '操作',
    SYSTEM: '系统'
  }
  return map[type] || type
}

const formatTime = (time: string) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  fetchLogs()
  fetchStats()
})
</script>

<style scoped>
.system-logs {
  padding: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.log-expand-content {
  padding: 10px 20px;
}

.log-details {
  padding: 10px 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.log-details span {
  color: #606266;
  word-break: break-all;
}

.log-details pre {
  margin: 0;
  padding: 10px;
  background-color: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 12px;
  max-height: 300px;
  overflow-y: auto;
}

:deep(.el-statistic__content) {
  font-size: 24px;
}
</style>
