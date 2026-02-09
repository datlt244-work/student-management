<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch } from '@/utils/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const selectedRole = ref<'Student' | 'Teacher' | 'Admin'>('Student')
const email = ref('')
const password = ref('')
const rememberMe = ref(false)

// Auto-fill email nếu đã remember trước đó
onMounted(() => {
  if (authStore.rememberedEmail) {
    email.value = authStore.rememberedEmail
    rememberMe.value = true
  }
})
const showPassword = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')

function togglePasswordVisibility() {
  showPassword.value = !showPassword.value
}

async function handleLogin(e: Event) {
  e.preventDefault()
  errorMessage.value = ''

  if (!email.value || !password.value) {
    errorMessage.value = 'Please enter both email and password.'
    return
  }

  isLoading.value = true

  try {
    const response = await apiFetch('/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        email: email.value,
        password: password.value,
      }),
    })

    const data = await response.json()

    if (!response.ok) {
      errorMessage.value = data.message || 'Login failed. Please check your credentials.'
      return
    }

    const result = data.result || data

    // Kiểm tra role được chọn trên UI có khớp với role từ server không
    const serverRole = result.role?.toLowerCase()
    const uiRole = selectedRole.value.toLowerCase()

    if (serverRole !== uiRole) {
      const roleLabel = selectedRole.value
      errorMessage.value = `Your account does not have the "${roleLabel}" role. Please select the correct role and try again.`
      return
    }

    // Lưu auth state vào store + localStorage
    authStore.setAuth(
      {
        accessToken: result.accessToken,
        refreshToken: result.refreshToken,
        userId: result.userId,
        email: result.email,
        role: result.role,
        profilePictureUrl: result.profilePictureUrl,
      },
      rememberMe.value,
    )

    // Redirect dựa theo role
    if (serverRole === 'admin') {
      router.push({ name: 'admin-dashboard' })
    } else if (serverRole === 'teacher') {
      router.push({ name: 'teacher-dashboard' })
    } else {
      // TODO: Thêm redirect cho student role
      router.push({ name: 'login' })
    }
  } catch (err) {
    errorMessage.value = 'Unable to connect to server. Please try again later.'
    console.error('Login error:', err)
  } finally {
    isLoading.value = false
  }
}

function handleGoogleLogin() {
  // TODO: Implement Google OAuth login
  console.log('Google login clicked')
}
</script>

<template>
  <div
    class="bg-background-light dark:bg-background-dark text-text-main-light dark:text-text-main-dark font-display antialiased min-h-screen flex flex-col overflow-x-hidden geo-pattern"
  >
    <div class="layout-container flex h-full grow flex-col items-center justify-center p-4 sm:p-6 lg:p-8">
      <!-- Login Card -->
      <div
        class="w-full max-w-md bg-surface-light dark:bg-surface-dark rounded-xl shadow-[0_4px_20px_rgba(0,0,0,0.05)] dark:shadow-[0_4px_20px_rgba(0,0,0,0.4)] border border-border-light dark:border-border-dark overflow-hidden relative"
      >
        <!-- Top accent bar -->
        <div class="h-2 w-full bg-primary"></div>

        <div class="p-8 flex flex-col gap-6">
          <!-- Header -->
          <div class="flex flex-col items-center gap-4 text-center">
            <div class="h-16 w-16 bg-primary/10 rounded-full flex items-center justify-center text-primary">
              <span class="material-symbols-outlined text-4xl">school</span>
            </div>
            <div>
              <h1 class="text-2xl font-bold tracking-tight">Welcome Back</h1>
              <p class="text-text-muted-light dark:text-text-muted-dark text-sm mt-1">
                Please sign in to your account
              </p>
            </div>
          </div>

          <!-- Role Selector -->
          <div
            class="flex p-1 bg-background-light dark:bg-background-dark/50 rounded-lg border border-border-light dark:border-border-dark"
          >
            <label
              v-for="role in ['Student', 'Teacher', 'Admin']"
              :key="role"
              class="cursor-pointer flex-1 relative"
            >
              <input
                v-model="selectedRole"
                class="peer sr-only"
                name="role"
                type="radio"
                :value="role"
              />
              <div
                class="flex items-center justify-center py-2 text-sm font-medium rounded-md text-text-muted-light dark:text-text-muted-dark transition-all peer-checked:bg-white dark:peer-checked:bg-surface-dark peer-checked:text-primary peer-checked:shadow-sm"
              >
                {{ role }}
              </div>
            </label>
          </div>

          <!-- Error Message -->
          <div
            v-if="errorMessage"
            class="flex items-center gap-2 p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 text-sm"
          >
            <span class="material-symbols-outlined text-[18px]">error</span>
            <span>{{ errorMessage }}</span>
          </div>

          <!-- Login Form -->
          <form class="flex flex-col gap-5" @submit="handleLogin">
            <!-- Email -->
            <label class="flex flex-col gap-1.5">
              <span class="text-sm font-medium">Email</span>
              <div class="relative group">
                <span
                  class="absolute left-4 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark group-focus-within:text-primary transition-colors"
                >
                  <span class="material-symbols-outlined text-[20px]">mail</span>
                </span>
                <input
                  v-model="email"
                  class="form-input w-full rounded-lg border-border-light dark:border-border-dark bg-background-light dark:bg-background-dark/50 pl-11 pr-4 py-3 text-sm focus:border-primary focus:ring-1 focus:ring-primary placeholder:text-text-muted-light/70 dark:placeholder:text-text-muted-dark/50 transition-all"
                  placeholder="Enter your email"
                  type="email"
                />
              </div>
            </label>

            <!-- Password -->
            <label class="flex flex-col gap-1.5">
              <span class="text-sm font-medium">Password</span>
              <div class="relative group">
                <span
                  class="absolute left-4 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark group-focus-within:text-primary transition-colors"
                >
                  <span class="material-symbols-outlined text-[20px]">lock</span>
                </span>
                <input
                  v-model="password"
                  class="form-input w-full rounded-lg border-border-light dark:border-border-dark bg-background-light dark:bg-background-dark/50 pl-11 pr-12 py-3 text-sm focus:border-primary focus:ring-1 focus:ring-primary placeholder:text-text-muted-light/70 dark:placeholder:text-text-muted-dark/50 transition-all"
                  placeholder="Enter your password"
                  :type="showPassword ? 'text' : 'password'"
                />
                <button
                  class="absolute right-4 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark hover:text-primary transition-colors"
                  type="button"
                  @click="togglePasswordVisibility"
                >
                  <span class="material-symbols-outlined text-[20px]">
                    {{ showPassword ? 'visibility' : 'visibility_off' }}
                  </span>
                </button>
              </div>
            </label>

            <!-- Remember me & Forgot password -->
            <div class="flex items-center justify-between mt-1">
              <label class="flex items-center gap-2 cursor-pointer group">
                <input
                  v-model="rememberMe"
                  class="h-4 w-4 rounded border-border-light dark:border-border-dark text-primary focus:ring-offset-0 focus:ring-primary/20 bg-white dark:bg-surface-dark checked:bg-primary checked:border-primary group-hover:border-primary transition-colors"
                  type="checkbox"
                />
                <span
                  class="text-sm text-text-muted-light dark:text-text-muted-dark group-hover:text-primary transition-colors"
                >
                  Remember me
                </span>
              </label>
              <router-link class="text-sm font-medium text-primary hover:text-primary-dark transition-colors" :to="{ name: 'forgot-password' }">
                Forgot Password?
              </router-link>
            </div>

            <!-- Login Button -->
            <button
              class="w-full bg-primary hover:bg-primary-dark text-white font-medium py-3 rounded-lg shadow-md shadow-primary/20 transition-all active:scale-[0.98] mt-2 flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed"
              type="submit"
              :disabled="isLoading"
            >
              <svg
                v-if="isLoading"
                class="animate-spin h-5 w-5 text-white"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
              >
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" :stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              <span>{{ isLoading ? 'Logging in...' : 'Login' }}</span>
              <span v-if="!isLoading" class="material-symbols-outlined text-sm">arrow_forward</span>
            </button>
          </form>

          <!-- Divider -->
          <div class="relative flex items-center">
            <div class="flex-grow border-t border-border-light dark:border-border-dark"></div>
            <span
              class="flex-shrink mx-4 text-xs font-semibold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider"
            >
              OR
            </span>
            <div class="flex-grow border-t border-border-light dark:border-border-dark"></div>
          </div>

          <!-- Google Login -->
          <button
            class="w-full flex items-center justify-center gap-3 bg-white hover:bg-gray-50 text-[#1f1f1f] font-medium py-3 px-4 rounded-lg border border-[#e1e1e1] shadow-sm transition-all active:scale-[0.98] text-sm"
            type="button"
            @click="handleGoogleLogin"
          >
            <svg height="18" viewBox="0 0 24 24" width="18" xmlns="http://www.w3.org/2000/svg">
              <path
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                fill="#4285F4"
              />
              <path
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                fill="#34A853"
              />
              <path
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                fill="#FBBC05"
              />
              <path
                d="M12 5.38c1.62 0 3.06.56 4.21 1.66l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                fill="#EA4335"
              />
              <path d="M1 1h22v22H1z" fill="none" />
            </svg>
            <span>Continue with Google</span>
          </button>
        </div>

        <!-- Footer -->
        <div
          class="bg-background-light dark:bg-background-dark/30 py-3 px-8 border-t border-border-light dark:border-border-dark text-center"
        >
          <p class="text-xs text-text-muted-light dark:text-text-muted-dark">
            Don't have an account?
            <a class="text-primary font-medium hover:underline" href="#">Contact Admin</a>
          </p>
        </div>
      </div>

      <!-- Version -->
      <div class="mt-8 text-center">
        <p class="text-xs text-text-muted-light dark:text-text-muted-dark font-medium opacity-80">
          Student Management System v1.0
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.geo-pattern {
  background-image:
    radial-gradient(circle at 10% 20%, rgba(244, 157, 37, 0.05) 0%, transparent 20%),
    radial-gradient(circle at 90% 80%, rgba(244, 157, 37, 0.05) 0%, transparent 20%),
    linear-gradient(
      135deg,
      transparent 0%,
      transparent 45%,
      rgba(244, 157, 37, 0.03) 45%,
      rgba(244, 157, 37, 0.03) 55%,
      transparent 55%,
      transparent 100%
    ),
    linear-gradient(
      45deg,
      transparent 0%,
      transparent 45%,
      rgba(244, 157, 37, 0.03) 45%,
      rgba(244, 157, 37, 0.03) 55%,
      transparent 55%,
      transparent 100%
    );
  background-size:
    100% 100%,
    100% 100%,
    60px 60px,
    60px 60px;
}
</style>

