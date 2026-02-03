<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="40" color="#409EFF"><VideoCamera /></el-icon>
          <h2>智能监控系统</h2>
        </div>
      </template>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="rules"
        label-width="80px"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="copyright">
      <p>© 2026 智能监控系统 版权所有 刘佳航 V1.5.4</p>
      <p class="icp-info">
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">京ICP备XXXXXXXX号-1</a>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, VideoCamera } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/modules/auth'

const router = useRouter()
const authStore = useAuthStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const success = await authStore.login(loginForm)
      if (success) {
        ElMessage.success('登录成功')
        router.push('/dashboard')
      } else {
        ElMessage.error('用户名或密码错误')
      }
    } catch (error) {
      ElMessage.error('登录失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-image: url('@/assets/background.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
}

.login-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 0;
}

.login-card {
  width: 450px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
  -webkit-backdrop-filter: blur(20px);
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.75) !important;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.login-card :deep(.el-card__header) {
  background: transparent;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.login-card :deep(.el-card__body) {
  background: transparent;
}

.card-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.card-header h2 {
  margin: 0;
  color: #303133;
  font-size: 24px;
}

.copyright {
  margin-top: 30px;
  text-align: center;
  position: relative;
  z-index: 1;
}

.copyright p {
  margin: 0;
  color: rgba(255, 255, 255, 0.95);
  font-size: 14px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
  font-weight: 500;
}

.icp-info {
  margin-top: 8px !important;
  font-size: 12px !important;
}

.icp-info a {
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  transition: color 0.3s;
}

.icp-info a:hover {
  color: #fff;
  text-decoration: underline;
}
</style>
