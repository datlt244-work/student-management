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
  <div class="bg-background-light dark:bg-background-dark min-h-screen flex flex-col font-display text-text-main-light dark:text-text-main-dark">
    <!-- Top Navigation -->
    <header class="flex items-center justify-between whitespace-nowrap border-b border-solid border-b-border-light dark:border-b-border-dark px-10 py-4 bg-background-light dark:bg-background-dark">
      <div class="flex items-center gap-3">
        <div class="size-8 text-primary">
          <span class="material-symbols-outlined !text-[32px]">school</span>
        </div>
        <h2 class="text-text-main-light dark:text-text-main-dark text-xl font-bold leading-tight tracking-tight">
          Student Management System
        </h2>
      </div>
    </header>

    <!-- Main Content Area -->
    <main class="flex-1 flex items-center justify-center p-4">
      <div class="w-full max-w-[480px] bg-surface-light dark:bg-surface-dark rounded-xl shadow-sm border border-border-light dark:border-border-dark overflow-hidden">
        <!-- Success State -->
        <template v-if="successMessage">
          <div class="px-8 pt-10 pb-2 text-center">
            <div class="inline-flex items-center justify-center size-16 rounded-full bg-green-100 dark:bg-green-900/20 mb-4 text-green-600 dark:text-green-400">
              <span class="material-symbols-outlined !text-[32px]">mark_email_read</span>
            </div>
            <h1 class="text-text-main-light dark:text-text-main-dark text-2xl font-bold leading-tight">
              Check Your Email
            </h1>
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
              class="inline-flex items-center gap-2 text-sm font-medium text-text-muted-light dark:text-text-muted-dark hover:text-primary dark:hover:text-primary transition-colors group cursor-pointer"
              @click="goToLogin"
            >
              <span class="material-symbols-outlined !text-[16px] group-hover:-translate-x-1 transition-transform">arrow_back</span>
              Back to Login
            </button>
          </div>
        </template>

        <!-- Form State -->
        <template v-else>
          <!-- Headline -->
          <div class="px-8 pt-10 pb-2 text-center">
            <div class="inline-flex items-center justify-center size-16 rounded-full bg-primary/10 dark:bg-primary/20 mb-4 text-primary">
              <span class="material-symbols-outlined !text-[32px]">lock_reset</span>
            </div>
            <h1 class="text-text-main-light dark:text-text-main-dark text-[32px] font-bold leading-tight">
              Forgot Password
            </h1>
          </div>

          <!-- Body Text -->
          <div class="px-8 pb-8 text-center">
            <p class="text-text-muted-light dark:text-text-muted-dark text-base font-normal leading-normal">
              Enter your registered email address below and we'll send you a link to reset your password.
            </p>
          </div>

          <!-- Error Message -->
          <div
            v-if="errorMessage"
            class="mx-8 mb-4 flex items-center gap-2 p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 text-sm"
          >
            <span class="material-symbols-outlined text-[18px]">error</span>
            <span>{{ errorMessage }}</span>
          </div>

          <!-- Form -->
          <form class="px-8 pb-6 flex flex-col gap-5" @submit="handleSubmit">
            <!-- Email Field -->
            <label class="flex flex-col gap-2">
              <span class="text-text-main-light dark:text-text-main-dark text-sm font-medium leading-normal">
                Email Address
              </span>
              <div class="relative">
                <input
                  v-model="email"
                  class="form-input flex w-full rounded-lg text-text-main-light dark:text-text-main-dark focus:outline-0 focus:ring-2 focus:ring-primary/50 border border-border-light dark:border-border-dark bg-background-light dark:bg-background-dark/50 h-12 placeholder:text-text-muted-light/70 dark:placeholder:text-text-muted-dark/50 px-4 pr-11 text-base font-normal transition-colors"
                  placeholder="student@fpt.edu.vn"
                  required
                  type="email"
                />
                <div class="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark pointer-events-none">
                  <span class="material-symbols-outlined !text-[20px]">mail</span>
                </div>
              </div>
            </label>

            <!-- Action Button -->
            <button
              class="flex w-full cursor-pointer items-center justify-center rounded-lg h-12 bg-primary hover:bg-primary-dark text-white text-base font-bold leading-normal tracking-[0.015em] transition-colors shadow-sm mt-2 disabled:opacity-60 disabled:cursor-not-allowed gap-2"
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
            </button>
          </form>

          <!-- Back Link -->
          <div class="px-8 pb-10 text-center">
            <button
              class="inline-flex items-center gap-2 text-sm font-medium text-text-muted-light dark:text-text-muted-dark hover:text-primary dark:hover:text-primary transition-colors group cursor-pointer"
              @click="goToLogin"
            >
              <span class="material-symbols-outlined !text-[16px] group-hover:-translate-x-1 transition-transform">arrow_back</span>
              Back to Login
            </button>
          </div>
        </template>
      </div>
    </main>

    <!-- Footer -->
    <footer class="py-6 text-center text-text-muted-light dark:text-text-muted-dark text-sm">
      © {{ new Date().getFullYear() }} Student Management System. All rights reserved.
    </footer>
  </div>
</template>

