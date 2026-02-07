<script setup lang="ts">
import { useRouter } from 'vue-router'

import AuthLayout from '@shared/components/auth/AuthLayout.vue'
import LoginForm from '@shared/components/auth/LoginForm.vue'

const router = useRouter()

const onLoginSubmit = (formData: any) => {
  console.log('Admin App received login data:', formData)

  // Logic Login dành riêng cho Admin
  // 1. Gọi API login
  // 2. Nếu thành công -> router.push('/dashboard')

  // Demo chuyển trang:
  if (formData.role === 'Admin') {
    router.push('/dashboard')
  } else {
    alert('Bạn không phải admin!')
  }
}

const onForgotPassword = () => {
  router.push('/forgot-password')
}
</script>

<template>
  <AuthLayout title="Admin Portal" subtitle="Sign in to manage the system">
    <LoginForm :show-roles="true" @submit="onLoginSubmit" @forgot-password="onForgotPassword" />

    <template #footer>
      <p class="text-xs text-text-muted-light dark:text-text-muted-dark">
        Technical Issue?
        <a href="#" class="text-primary font-medium hover:underline">Contact IT Support</a>
      </p>
    </template>
  </AuthLayout>
</template>
