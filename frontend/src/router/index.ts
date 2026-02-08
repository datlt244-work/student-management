import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guest: true },
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('@/views/ForgotPasswordView.vue'),
      meta: { guest: true },
    },
    {
      path: '/reset-password',
      name: 'reset-password',
      component: () => import('@/views/ResetPasswordView.vue'),
    },
    // Admin routes
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, role: 'admin' },
      children: [
        {
          path: '',
          redirect: { name: 'admin-dashboard' },
        },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/DashboardView.vue'),
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/DashboardView.vue'), // TODO: create UserManagement page
        },
        {
          path: 'courses',
          name: 'admin-courses',
          component: () => import('@/views/admin/DashboardView.vue'), // TODO: create CourseManagement page
        },
        {
          path: 'classes',
          name: 'admin-classes',
          component: () => import('@/views/admin/DashboardView.vue'), // TODO: create ClassManagement page
        },
        {
          path: 'logs',
          name: 'admin-logs',
          component: () => import('@/views/admin/DashboardView.vue'), // TODO: create SystemLogs page
        },
      ],
    },
  ],
})

// Navigation guard
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  // Route requires auth
  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      return next({ name: 'login' })
    }

    // Route requires specific role
    if (to.meta.role && authStore.userRole !== to.meta.role) {
      return next({ name: 'login' })
    }
  }

  // Guest-only route (login page) — redirect if already logged in
  if (to.meta.guest && authStore.isAuthenticated) {
    const role = authStore.userRole
    if (role === 'admin') {
      return next({ name: 'admin-dashboard' })
    }
    // TODO: Add redirects for student/teacher roles
  }

  next()
})

export default router
