<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getApiUrl } from '@/utils/api'

const route = useRoute()
const router = useRouter()

const status = ref<'loading' | 'success' | 'error'>('loading')
const message = ref('')
const countdown = ref(5)
let countdownTimer: ReturnType<typeof setInterval> | null = null

function goToLogin() {
  if (countdownTimer) clearInterval(countdownTimer)
  router.push({ name: 'login' })
}

function startCountdown() {
  countdown.value = 5
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      if (countdownTimer) clearInterval(countdownTimer)
      countdownTimer = null
      goToLogin()
    }
  }, 1000)
}

watch(
  () => status.value,
  (s) => {
    if (s === 'success' || s === 'error') {
      startCountdown()
    }
  },
)

onMounted(async () => {
  const token = (route.query.token as string)?.trim()
  if (!token) {
    status.value = 'error'
    message.value = 'Thiếu link kích hoạt. Vui lòng dùng đúng link trong email.'
    return
  }

  try {
    const url = getApiUrl('/auth/activate?token=' + encodeURIComponent(token))
    const response = await fetch(url, { method: 'GET' })

    const data = await response.json().catch(() => ({}))
    const errMsg = data?.message
    const okMsg = data?.result?.message ?? data?.message

    if (!response.ok) {
      status.value = 'error'
      message.value = errMsg || 'Link kích hoạt không hợp lệ hoặc đã hết hạn (72h). Vui lòng liên hệ Admin để gửi lại email.'
      return
    }

    status.value = 'success'
    message.value = okMsg || 'Tài khoản đã được kích hoạt. Bạn có thể đăng nhập.'
  } catch {
    status.value = 'error'
    message.value = 'Không thể kết nối. Vui lòng thử lại sau.'
  }
})

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<template>
  <div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm">
    <!-- Modal -->
    <div
      class="w-full max-w-md rounded-2xl border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-800 shadow-2xl p-8 text-center"
      role="dialog"
      aria-modal="true"
      aria-labelledby="activate-title"
    >
      <!-- Loading -->
      <template v-if="status === 'loading'">
        <div class="flex justify-center mb-6">
          <span class="material-symbols-outlined animate-spin text-5xl text-primary">progress_activity</span>
        </div>
        <h1 id="activate-title" class="text-xl font-bold text-slate-800 dark:text-white">Đang kích hoạt tài khoản…</h1>
        <p class="text-slate-500 dark:text-slate-400 mt-2">Vui lòng đợi trong giây lát.</p>
      </template>

      <!-- Success -->
      <template v-else-if="status === 'success'">
        <div class="w-16 h-16 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center mx-auto mb-6">
          <span class="material-symbols-outlined text-4xl text-green-600 dark:text-green-400">check_circle</span>
        </div>
        <h1 id="activate-title" class="text-xl font-bold text-slate-800 dark:text-white">Kích hoạt thành công</h1>
        <p class="text-slate-600 dark:text-slate-300 mt-2">{{ message }}</p>
        <p class="mt-4 text-sm text-slate-500 dark:text-slate-400">
          Chuyển về trang đăng nhập trong {{ countdown }} giây…
        </p>
        <button
          type="button"
          class="mt-6 w-full py-3 px-4 rounded-xl bg-primary hover:bg-primary-dark text-white font-bold transition-all"
          @click="goToLogin"
        >
          Đăng nhập ngay
        </button>
      </template>

      <!-- Error -->
      <template v-else>
        <div class="w-16 h-16 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center mx-auto mb-6">
          <span class="material-symbols-outlined text-4xl text-red-600 dark:text-red-400">error</span>
        </div>
        <h1 id="activate-title" class="text-xl font-bold text-slate-800 dark:text-white">Không thể kích hoạt</h1>
        <p class="text-slate-600 dark:text-slate-300 mt-2">{{ message }}</p>
        <p class="mt-4 text-sm text-slate-500 dark:text-slate-400">
          Chuyển về trang đăng nhập trong {{ countdown }} giây…
        </p>
        <button
          type="button"
          class="mt-6 w-full py-3 px-4 rounded-xl bg-stone-200 dark:bg-stone-700 hover:bg-stone-300 dark:hover:bg-stone-600 text-slate-800 dark:text-white font-bold transition-all"
          @click="goToLogin"
        >
          Về trang đăng nhập ngay
        </button>
      </template>
    </div>
  </div>
</template>
