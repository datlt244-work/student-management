<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch } from '@/utils/api'

const router = useRouter()

const email = ref('')
const isLoading = ref(false)
const successMessage = ref('')
const errorMessage = ref('')
const cooldownMinutes = ref(0)

async function handleSubmit(e: Event) {
  e.preventDefault()
  errorMessage.value = ''
  successMessage.value = ''

  if (!email.value) {
    errorMessage.value = 'Please enter your email address.'
    return
  }

  isLoading.value = true

  try {
    const response = await apiFetch('/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email: email.value }),
    })

    const data = await response.json()

    if (!response.ok) {
      errorMessage.value = data.message || 'Something went wrong. Please try again.'
      return
    }

    const result = data.result || data
    cooldownMinutes.value = result.cooldownMinutes || 15
    successMessage.value =
      result.message || 'If an account exists with this email, a password reset link has been sent.'
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

        <!-- Success State -->
        <template v-if="successMessage">
          <div class="px-8 pt-10 pb-2 text-center">
            <div class="inline-flex items-center justify-center size-16 rounded-full bg-green-100 dark:bg-green-900/20 mb-4 text-green-600 dark:text-green-400">
              <span class="material-symbols-outlined !text-[32px]">mark_email_read</span>
            </div>
            <h1 class="text-2xl font-bold leading-tight">Check Your Email</h1>
          </div>
          <div class="px-8 pb-6 text-center">
            <p class="text-text-muted-light dark:text-text-muted-dark text-base font-normal leading-normal">
              {{ successMessage }}
            </p>
            <p class="text-text-muted-light dark:text-text-muted-dark text-sm mt-3">
              The link will expire in <span class="font-semibold text-primary">{{ cooldownMinutes }} minutes</span>.
            </p>
          </div>
          <div class="px-8 pb-10 text-center">
            <button
              class="inline-flex items-center gap-2 text-sm font-medium text-text-muted-light dark:text-text-muted-dark hover:text-primary transition-colors group cursor-pointer"
              @click="goToLogin"
            >
              <span class="material-symbols-outlined !text-[16px] group-hover:-translate-x-1 transition-transform">arrow_back</span>
              Back to Login
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
                <h1 class="text-2xl font-bold tracking-tight">Forgot Password</h1>
                <p class="text-text-muted-light dark:text-text-muted-dark text-sm mt-1">
                  Enter your registered email address and we'll send you a reset link.
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
              <label class="flex flex-col gap-1.5">
                <span class="text-sm font-medium">Email Address</span>
                <div class="relative group">
                  <span class="absolute left-4 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark group-focus-within:text-primary transition-colors">
                    <span class="material-symbols-outlined text-[20px]">mail</span>
                  </span>
                  <input
                    v-model="email"
                    class="form-input w-full rounded-lg border-border-light dark:border-border-dark bg-background-light dark:bg-background-dark/50 pl-11 pr-4 py-3 text-sm focus:border-primary focus:ring-1 focus:ring-primary placeholder:text-text-muted-light/70 dark:placeholder:text-text-muted-dark/50 transition-all"
                    placeholder="student@fpt.edu.vn"
                    required
                    type="email"
                  />
                </div>
              </label>

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
                <span>{{ isLoading ? 'Sending...' : 'Send Reset Link' }}</span>
                <span v-if="!isLoading" class="material-symbols-outlined text-sm">arrow_forward</span>
              </button>
            </form>
          </div>

          <!-- Footer -->
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
