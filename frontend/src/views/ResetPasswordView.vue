<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiFetch } from '@/utils/api'

const route = useRoute()
const router = useRouter()

const token = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')
const isSuccess = ref(false)
const isTokenMissing = ref(false)

onMounted(() => {
  token.value = (route.query.token as string) || ''
  if (!token.value) {
    isTokenMissing.value = true
  }
})

// Password strength logic
const passwordChecks = computed(() => [
  { label: 'At least 8 characters long', passed: newPassword.value.length >= 8 },
  { label: 'At least one uppercase letter', passed: /[A-Z]/.test(newPassword.value) },
  { label: 'At least one lowercase letter', passed: /[a-z]/.test(newPassword.value) },
  { label: 'At least one numeric digit', passed: /\d/.test(newPassword.value) },
  { label: 'At least one special character', passed: /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(newPassword.value) },
])

const passedCount = computed(() => passwordChecks.value.filter((c) => c.passed).length)
const strengthPercent = computed(() => Math.round((passedCount.value / passwordChecks.value.length) * 100))
const strengthLabel = computed(() => {
  if (strengthPercent.value === 0) return ''
  if (strengthPercent.value <= 40) return 'Weak'
  if (strengthPercent.value <= 60) return 'Fair'
  if (strengthPercent.value <= 80) return 'Good'
  return 'Strong'
})

const passwordsMatch = computed(() => {
  if (!confirmPassword.value) return null
  return newPassword.value === confirmPassword.value
})

async function handleSubmit(e: Event) {
  e.preventDefault()
  errorMessage.value = ''

  if (!newPassword.value || !confirmPassword.value) {
    errorMessage.value = 'Please fill in both password fields.'
    return
  }

  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = 'Passwords do not match.'
    return
  }

  if (passedCount.value < passwordChecks.value.length) {
    errorMessage.value = 'Please meet all password requirements.'
    return
  }

  isLoading.value = true

  try {
    const response = await apiFetch('/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({
        token: token.value,
        newPassword: newPassword.value,
        confirmPassword: confirmPassword.value,
      }),
    })

    const data = await response.json()

    if (!response.ok) {
      errorMessage.value = data.message || 'Failed to reset password. Please try again.'
      return
    }

    isSuccess.value = true
  } catch {
    errorMessage.value = 'Unable to connect to server. Please try again later.'
  } finally {
    isLoading.value = false
  }
}

function goToLogin() {
  router.push({ name: 'login' })
}
</script>

<template>
  <div
    class="bg-background-light dark:bg-background-dark text-text-main-light dark:text-text-main-dark font-display antialiased min-h-screen flex flex-col overflow-x-hidden geo-pattern"
  >
    <div class="layout-container flex h-full grow flex-col items-center justify-center p-4 sm:p-6 lg:p-8">
      <div
        class="w-full max-w-[480px] bg-surface-light dark:bg-surface-dark rounded-xl shadow-[0_4px_20px_rgba(0,0,0,0.05)] dark:shadow-[0_4px_20px_rgba(0,0,0,0.4)] border border-border-light dark:border-border-dark overflow-hidden relative"
      >
        <!-- Top accent bar -->
        <div class="h-2 w-full bg-primary"></div>

        <!-- Token Missing State -->
        <template v-if="isTokenMissing">
          <div class="p-8 flex flex-col gap-6">
            <div class="flex flex-col items-center gap-4 text-center">
              <div class="h-16 w-16 bg-red-100 dark:bg-red-900/20 rounded-full flex items-center justify-center text-red-600 dark:text-red-400">
                <span class="material-symbols-outlined text-4xl">error</span>
              </div>
              <div>
                <h1 class="text-2xl font-bold tracking-tight">Invalid Link</h1>
                <p class="text-text-muted-light dark:text-text-muted-dark text-sm mt-1">
                  This password reset link is invalid or has expired. Please request a new one.
                </p>
              </div>
            </div>
            <button
              class="w-full bg-primary hover:bg-primary-dark text-white font-medium py-3 rounded-lg shadow-md shadow-primary/20 transition-all active:scale-[0.98] flex items-center justify-center gap-2"
              @click="router.push({ name: 'forgot-password' })"
            >
              <span>Request New Link</span>
              <span class="material-symbols-outlined text-sm">arrow_forward</span>
            </button>
          </div>
          <div class="bg-background-light dark:bg-background-dark/30 py-3 px-8 border-t border-border-light dark:border-border-dark text-center">
            <button
              class="inline-flex items-center gap-2 text-sm font-medium text-text-muted-light dark:text-text-muted-dark hover:text-primary transition-colors group cursor-pointer"
              @click="goToLogin"
            >
              <span class="material-symbols-outlined !text-[16px] group-hover:-translate-x-1 transition-transform">arrow_back</span>
              Back to Login
            </button>
          </div>
        </template>

        <!-- Success State -->
        <template v-else-if="isSuccess">
          <div class="p-8 flex flex-col gap-6">
            <div class="flex flex-col items-center gap-4 text-center">
              <div class="h-16 w-16 bg-green-100 dark:bg-green-900/20 rounded-full flex items-center justify-center text-green-600 dark:text-green-400">
                <span class="material-symbols-outlined text-4xl">check_circle</span>
              </div>
              <div>
                <h1 class="text-2xl font-bold tracking-tight">Password Updated</h1>
                <p class="text-text-muted-light dark:text-text-muted-dark text-sm mt-1">
                  Your password has been reset successfully. All existing sessions have been logged out.
                </p>
              </div>
            </div>
            <button
              class="w-full bg-primary hover:bg-primary-dark text-white font-medium py-3 rounded-lg shadow-md shadow-primary/20 transition-all active:scale-[0.98] flex items-center justify-center gap-2"
              @click="goToLogin"
            >
              <span>Go to Login</span>
              <span class="material-symbols-outlined text-sm">arrow_forward</span>
            </button>
          </div>
        </template>

        <!-- Form State -->
        <template v-else>
          <div class="p-8 flex flex-col gap-6">
            <!-- Header -->
            <div class="flex flex-col items-center gap-4 text-center">
              <div class="h-16 w-16 bg-primary/10 rounded-full flex items-center justify-center text-primary">
                <span class="material-symbols-outlined text-4xl">lock_reset</span>
              </div>
              <div>
                <h1 class="text-2xl font-bold tracking-tight">New Password Setup</h1>
                <p class="text-text-muted-light dark:text-text-muted-dark text-sm mt-1">
                  Secure your account by creating a new password below.
                </p>
              </div>
            </div>

            <!-- Error Message -->
            <div
              v-if="errorMessage"
              class="flex items-center gap-2 p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 text-sm"
            >
              <span class="material-symbols-outlined text-[18px]">error</span>
              <span>{{ errorMessage }}</span>
            </div>

            <!-- Form -->
            <form class="flex flex-col gap-5" @submit="handleSubmit">
              <!-- New Password -->
              <label class="flex flex-col gap-1.5">
                <span class="text-sm font-medium">New Password</span>
                <div class="relative group">
                  <span class="absolute left-4 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark group-focus-within:text-primary transition-colors">
                    <span class="material-symbols-outlined text-[20px]">lock</span>
                  </span>
                  <input
                    v-model="newPassword"
                    class="form-input w-full rounded-lg border-border-light dark:border-border-dark bg-background-light dark:bg-background-dark/50 pl-11 pr-12 py-3 text-sm focus:border-primary focus:ring-1 focus:ring-primary placeholder:text-text-muted-light/70 dark:placeholder:text-text-muted-dark/50 transition-all"
                    placeholder="Enter new password"
                    :type="showNewPassword ? 'text' : 'password'"
                  />
                  <button
                    class="absolute right-4 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark hover:text-primary transition-colors"
                    type="button"
                    @click="showNewPassword = !showNewPassword"
                  >
                    <span class="material-symbols-outlined text-[20px]">{{ showNewPassword ? 'visibility' : 'visibility_off' }}</span>
                  </button>
                </div>
              </label>

              <!-- Confirm Password -->
              <label class="flex flex-col gap-1.5">
                <span class="text-sm font-medium">Confirm New Password</span>
                <div class="relative group">
                  <span class="absolute left-4 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark group-focus-within:text-primary transition-colors">
                    <span class="material-symbols-outlined text-[20px]">lock</span>
                  </span>
                  <input
                    v-model="confirmPassword"
                    class="form-input w-full rounded-lg pl-11 pr-12 py-3 text-sm transition-all bg-background-light dark:bg-background-dark/50 placeholder:text-text-muted-light/70 dark:placeholder:text-text-muted-dark/50"
                    :class="[
                      passwordsMatch === false
                        ? 'border-red-400 dark:border-red-600 focus:ring-1 focus:ring-red-300'
                        : passwordsMatch === true
                          ? 'border-green-400 dark:border-green-600 focus:ring-1 focus:ring-green-300'
                          : 'border-border-light dark:border-border-dark focus:border-primary focus:ring-1 focus:ring-primary',
                    ]"
                    placeholder="Confirm new password"
                    :type="showConfirmPassword ? 'text' : 'password'"
                  />
                  <button
                    class="absolute right-4 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark hover:text-primary transition-colors"
                    type="button"
                    @click="showConfirmPassword = !showConfirmPassword"
                  >
                    <span class="material-symbols-outlined text-[20px]">{{ showConfirmPassword ? 'visibility' : 'visibility_off' }}</span>
                  </button>
                </div>
                <p v-if="passwordsMatch === false" class="text-red-500 dark:text-red-400 text-xs">
                  Passwords do not match.
                </p>
              </label>

              <!-- Password Strength -->
              <div v-if="newPassword" class="space-y-2">
                <div class="flex justify-between items-center">
                  <span class="text-sm font-medium">Password Strength</span>
                  <span class="text-sm font-bold text-primary">{{ strengthLabel }} ({{ strengthPercent }}%)</span>
                </div>
                <div class="w-full h-2 bg-background-light dark:bg-background-dark rounded-full overflow-hidden border border-border-light dark:border-border-dark">
                  <div
                    class="h-full rounded-full transition-all duration-500"
                    :class="{
                      'bg-red-500': strengthPercent <= 40,
                      'bg-yellow-500': strengthPercent > 40 && strengthPercent <= 60,
                      'bg-primary': strengthPercent > 60 && strengthPercent <= 80,
                      'bg-green-500': strengthPercent > 80,
                    }"
                    :style="{ width: strengthPercent + '%' }"
                  ></div>
                </div>
              </div>

              <!-- Password Requirements -->
              <div class="bg-primary/5 dark:bg-primary/10 rounded-lg p-4 space-y-2 border border-primary/10 dark:border-primary/20">
                <p class="text-xs font-bold uppercase tracking-wider text-primary mb-2">Password Requirements</p>
                <div
                  v-for="check in passwordChecks"
                  :key="check.label"
                  class="flex items-center gap-2 text-sm"
                  :class="check.passed ? 'text-green-600 dark:text-green-400' : 'text-text-muted-light dark:text-text-muted-dark'"
                >
                  <span class="material-symbols-outlined text-lg">
                    {{ check.passed ? 'check_circle' : 'radio_button_unchecked' }}
                  </span>
                  <span>{{ check.label }}</span>
                </div>
              </div>

              <!-- Submit Button -->
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
                <span>{{ isLoading ? 'Updating...' : 'Update Password' }}</span>
                <span v-if="!isLoading" class="material-symbols-outlined text-sm">arrow_forward</span>
              </button>
            </form>
          </div>

          <!-- Card Footer -->
          <div class="bg-background-light dark:bg-background-dark/30 py-3 px-8 border-t border-border-light dark:border-border-dark text-center">
            <button
              class="inline-flex items-center gap-2 text-sm font-medium text-text-muted-light dark:text-text-muted-dark hover:text-primary transition-colors group cursor-pointer"
              @click="goToLogin"
            >
              <span class="material-symbols-outlined !text-[16px] group-hover:-translate-x-1 transition-transform">arrow_back</span>
              Back to Login
            </button>
          </div>
        </template>
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
