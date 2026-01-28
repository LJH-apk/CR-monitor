<template>
  <div class="admin-container">
    <el-page-header @back="goBack" title="返回">
      <template #content>
        <span class="page-title">管理后台</span>
      </template>
    </el-page-header>

    <!-- 导航菜单 -->
    <el-menu
      :default-active="'/admin/videos'"
      mode="horizontal"
      :router="true"
      class="admin-menu"
    >
      <el-menu-item index="/admin/videos">视频管理</el-menu-item>
      <el-menu-item index="/admin/behaviors">危险行为管理</el-menu-item>
      <el-menu-item index="/admin/thresholds">告警阈值配置</el-menu-item>
      <el-menu-item v-if="authStore.isSuperAdmin()" index="/admin/users">用户管理</el-menu-item>
    </el-menu>

    <el-card class="upload-card">
      <el-upload
        drag
        :action="uploadUrl"
        :headers="uploadHeaders"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload"
        accept="video/*"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          拖拽视频文件到此处或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持MP4、AVI等格式，文件大小不超过500MB
          </div>
        </template>
      </el-upload>
    </el-card>

    <el-card class="video-list-card">
      <template #header>
        <span>视频列表</span>
      </template>

      <el-table :data="videoStore.videos" style="width: 100%">
        <el-table-column prop="originalFilename" label="文件名" min-width="200" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时长" width="100">
          <template #default="{ row }">
            {{ formatDuration(row.duration) }}
          </template>
        </el-table-column>
        <el-table-column label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { useVideoStore } from '@/store/modules/video'
import { useAuthStore } from '@/store/modules/auth'

const router = useRouter()
const videoStore = useVideoStore()
const authStore = useAuthStore()

const uploadUrl = computed(() => 'http://localhost:8080/api/videos/upload')
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${authStore.token}`
}))

onMounted(() => {
  videoStore.fetchVideos()
})

const goBack = () => {
  router.push('/dashboard')
}

const beforeUpload = (file: File) => {
  const isVideo = file.type.startsWith('video/')
  const isLt500M = file.size / 1024 / 1024 < 500

  if (!isVideo) {
    ElMessage.error('只能上传视频文件!')
    return false
  }
  if (!isLt500M) {
    ElMessage.error('视频大小不能超过 500MB!')
    return false
  }
  return true
}

const handleUploadSuccess = () => {
  ElMessage.success('视频上传成功，正在转码...')
  videoStore.fetchVideos()
}

const handleUploadError = () => {
  ElMessage.error('视频上传失败')
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个视频吗？', '提示', {
      type: 'warning'
    })
    await videoStore.deleteVideo(id)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getStatusType = (status: string) => {
  const map: Record<string, any> = {
    UPLOADING: 'info',
    TRANSCODING: 'warning',
    READY: 'success',
    FAILED: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    UPLOADING: '上传中',
    TRANSCODING: '转码中',
    READY: '就绪',
    FAILED: '失败'
  }
  return map[status] || status
}

const formatDuration = (seconds?: number) => {
  if (!seconds) return '-'
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
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

.admin-menu {
  margin: 20px 0;
  background: white;
  border-radius: 4px;
}

.upload-card {
  margin: 20px 0;
}

.video-list-card {
  margin-top: 20px;
}
</style>
