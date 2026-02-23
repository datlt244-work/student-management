import { ref } from 'vue'

export interface ToastState {
  message: string
  type: 'success' | 'error'
}

export function useToast() {
  const toast = ref<ToastState | null>(null)
  let toastTimer: ReturnType<typeof setTimeout> | null = null

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    if (toastTimer) clearTimeout(toastTimer)
    toast.value = { message, type }
    toastTimer = setTimeout(() => {
      toast.value = null
    }, 3500)
  }

  return {
    toast,
    showToast,
  }
}
