<template>
  <div>
    <el-page-header @back="goBack" title="返回">
      <template #content>
        <span class="page-title">管理后台</span>
      </template>
      <template v-if="$slots['header-extra']" #extra>
        <slot name="header-extra" />
      </template>
    </el-page-header>

    <el-menu
      :default-active="activeRoute"
      mode="horizontal"
      :router="true"
      class="admin-menu"
    >
      <el-menu-item index="/admin/videos">视频管理</el-menu-item>
      <el-menu-item index="/admin/behaviors">危险行为管理</el-menu-item>
      <el-menu-item index="/admin/thresholds">告警阈值配置</el-menu-item>
      <el-menu-item index="/admin/models">模型管理</el-menu-item>
      <el-menu-item v-if="authStore.isSuperAdmin() || authStore.isDeveloper()" index="/admin/users">用户管理</el-menu-item>
      <el-menu-item v-if="authStore.isDeveloper()" index="/developer/logs">系统日志</el-menu-item>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'

defineProps<{
  activeRoute: string
}>()

const router = useRouter()
const authStore = useAuthStore()

const goBack = () => {
  router.push('/dashboard')
}
</script>

<style scoped>
.admin-menu {
  margin: 20px 0;
  background: white;
  border-radius: 4px;
}
</style>
