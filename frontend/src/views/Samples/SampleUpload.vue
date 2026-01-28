<template>
  <div class="sample-upload-container">
    <el-page-header @back="goBack" title="返回">
      <template #content>
        <span class="page-title">上传训练样本</span>
      </template>
    </el-page-header>

    <el-card class="upload-card">
      <el-upload
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :show-file-list="false"
        accept="image/*"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          拖拽图片到此处或 <em>点击选择</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持JPG、PNG格式，文件大小不超过10MB，尺寸不超过4096x4096
          </div>
        </template>
      </el-upload>

      <!-- 图片预览 -->
      <div v-if="previewUrl" class="preview-section">
        <h3>图片预览</h3>
        <img :src="previewUrl" alt="预览" class="preview-image" />
        <div class="image-info">
          <p>文件名: {{ selectedFile?.name }}</p>
          <p>文件大小: {{ formatFileSize(selectedFile?.size || 0) }}</p>
          <p v-if="imageWidth && imageHeight">
            图片尺寸: {{ imageWidth }} x {{ imageHeight }}
          </p>
        </div>
        <div class="action-buttons">
          <el-button @click="clearSelection">重新选择</el-button>
          <el-button type="primary" @click="handleUpload" :loading="uploading">
            上传并标注
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { sampleApi } from '@/api/sample'

const router = useRouter()

const selectedFile = ref<File | null>(null)
const previewUrl = ref<string>('')
const imageWidth = ref<number>(0)
const imageHeight = ref<number>(0)
const uploading = ref(false)

const goBack = () => {
  router.push('/dashboard')
}

const handleFileChange = (file: any) => {
  const rawFile = file.raw as File

  // 验证文件类型
  if (!rawFile.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return
  }

  // 验证文件大小（10MB）
  if (rawFile.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过10MB')
    return
  }

  selectedFile.value = rawFile

  // 创建预览URL
  const reader = new FileReader()
  reader.onload = (e) => {
    previewUrl.value = e.target?.result as string

    // 获取图片尺寸
    const img = new Image()
    img.onload = () => {
      imageWidth.value = img.width
      imageHeight.value = img.height

      // 验证图片尺寸
      if (img.width > 4096 || img.height > 4096) {
        ElMessage.error('图片尺寸不能超过4096x4096')
        clearSelection()
      }
    }
    img.src = previewUrl.value
  }
  reader.readAsDataURL(rawFile)
}

const clearSelection = () => {
  selectedFile.value = null
  previewUrl.value = ''
  imageWidth.value = 0
  imageHeight.value = 0
}

const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择图片')
    return
  }

  uploading.value = true
  try {
    const response = await sampleApi.upload(selectedFile.value)
    ElMessage.success('上传成功，开始标注')
    // 跳转到标注页面
    router.push(`/samples/annotate/${response.data.sampleId}`)
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}
</script>

<style scoped>
.sample-upload-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.upload-card {
  margin-top: 20px;
  max-width: 800px;
}

.preview-section {
  margin-top: 30px;
  padding-top: 30px;
  border-top: 1px solid #eee;
}

.preview-section h3 {
  margin-bottom: 15px;
  font-size: 16px;
  font-weight: 600;
}

.preview-image {
  max-width: 100%;
  max-height: 400px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.image-info {
  margin: 15px 0;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 4px;
}

.image-info p {
  margin: 5px 0;
  color: #666;
}

.action-buttons {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}
</style>
