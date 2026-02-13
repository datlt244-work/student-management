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
    {
      path: '/activate',
      name: 'activate',
      component: () => import('@/views/ActivateView.vue'),
      meta: { guest: true },
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
          component: () => import('@/views/admin/UsersView.vue'),
        },
        {
          path: 'users/:userId',
          name: 'admin-user-detail',
          component: () => import('@/views/admin/UserDetailView.vue'),
        },
        {
          path: 'departments',
          name: 'admin-departments',
          component: () => import('@/views/admin/DepartmentsView.vue'),
        },
        {
          path: 'courses',
          name: 'admin-courses',
          component: () => import('@/views/admin/CoursesView.vue'),
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
    // Teacher routes
    {
      path: '/teacher',
      component: () => import('@/layouts/TeacherLayout.vue'),
      meta: { requiresAuth: true, role: 'teacher' },
      children: [
        {
          path: '',
          redirect: { name: 'teacher-dashboard' },
        },
        {
          path: 'dashboard',
          name: 'teacher-dashboard',
          component: () => import('@/views/teacher/DashboardView.vue'),
        },
        {
          path: 'students',
          name: 'teacher-students',
          component: () => import('@/views/teacher/DashboardView.vue'), // TODO: create Students page
        },
        {
          path: 'classes',
          name: 'teacher-classes',
          component: () => import('@/views/teacher/DashboardView.vue'), // TODO: create Classes page
        },
        {
          path: 'grades',
          name: 'teacher-grades',
          component: () => import('@/views/teacher/DashboardView.vue'), // TODO: create Grades page
        },
        {
          path: 'schedule',
          name: 'teacher-schedule',
          component: () => import('@/views/teacher/DashboardView.vue'), // TODO: create Schedule page
        },
        {
          path: 'profile',
          name: 'teacher-profile',
          component: () => import('@/views/teacher/ProfileView.vue'),
        },
      ],
    },
    // Student routes
    {
      path: '/student',
      component: () => import('@/layouts/StudentLayout.vue'),
      meta: { requiresAuth: true, role: 'student' },
      children: [
        {
          path: '',
          redirect: { name: 'student-dashboard' },
        },
        {
          path: 'dashboard',
          name: 'student-dashboard',
          component: () => import('@/views/student/DashboardView.vue'),
        },
        {
          path: 'courses',
          name: 'student-courses',
          component: () => import('@/views/student/DashboardView.vue'), // TODO: create Courses page
        },
        {
          path: 'grades',
          name: 'student-grades',
          component: () => import('@/views/student/DashboardView.vue'), // TODO: create Grades page
        },
        {
          path: 'schedule',
          name: 'student-schedule',
          component: () => import('@/views/student/DashboardView.vue'), // TODO: create Schedule page
        },
        {
          path: 'profile',
          name: 'student-profile',
          component: () => import('@/views/student/ProfileView.vue'),
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
    if (role === 'teacher') {
      return next({ name: 'teacher-dashboard' })
    }
    if (role === 'student') {
      return next({ name: 'student-dashboard' })
    }
  }

  next()
})

export default router
