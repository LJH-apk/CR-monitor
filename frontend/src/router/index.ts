import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      redirect: '/dashboard'
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('@/views/Dashboard.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin/behaviors',
      name: 'DangerBehaviors',
      component: () => import('@/views/Admin/DangerBehaviors.vue'),
      meta: { requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/admin/thresholds',
      name: 'Thresholds',
      component: () => import('@/views/Admin/Thresholds.vue'),
      meta: { requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/admin/videos',
      name: 'VideoManagement',
      component: () => import('@/views/Admin/VideoManagement.vue'),
      meta: { requiresAuth: true, requiresAdmin: true }
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 需要认证的路由
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
    return
  }

  // 需要管理员权限的路由
  if (to.meta.requiresAdmin && !authStore.isAdmin()) {
    next('/dashboard')
    return
  }

  // 已登录用户访问登录页，重定向到首页
  if (to.path === '/login' && authStore.isAuthenticated) {
    next('/dashboard')
    return
  }

  next()
})

export default router
