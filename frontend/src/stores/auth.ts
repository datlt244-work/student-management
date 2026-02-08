import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export interface AuthUser {
  userId: string
  email: string
  role: string | null
  profilePictureUrl: string | null
}

function getStorage(): Storage {
  // Nếu đã chọn rememberMe trước đó → dùng localStorage, ngược lại sessionStorage
  return localStorage.getItem('rememberMe') === 'true' ? localStorage : sessionStorage
}

export const useAuthStore = defineStore('auth', () => {
  const storage = getStorage()
  const accessToken = ref<string | null>(storage.getItem('accessToken'))
  const refreshToken = ref<string | null>(storage.getItem('refreshToken'))
  const user = ref<AuthUser | null>(loadUser())

  const isAuthenticated = computed(() => !!accessToken.value)
  const userRole = computed(() => user.value?.role?.toLowerCase() ?? null)
  const isAdmin = computed(() => userRole.value === 'admin')
  const rememberedEmail = computed(() => localStorage.getItem('rememberedEmail') || '')

  function loadUser(): AuthUser | null {
    const raw = getStorage().getItem('user')
    if (!raw) return null
    try {
      return JSON.parse(raw)
    } catch {
      return null
    }
  }

  function setAuth(
    data: {
      accessToken: string
      refreshToken: string
      userId: string
      email: string
      role: string | null
      profilePictureUrl: string | null
    },
    rememberMe: boolean = false,
  ) {
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    user.value = {
      userId: data.userId,
      email: data.email,
      role: data.role,
      profilePictureUrl: data.profilePictureUrl,
    }

    // Chọn storage dựa theo rememberMe
    const target = rememberMe ? localStorage : sessionStorage
    localStorage.setItem('rememberMe', String(rememberMe))

    target.setItem('accessToken', data.accessToken)
    target.setItem('refreshToken', data.refreshToken)
    target.setItem('user', JSON.stringify(user.value))

    // Lưu email để auto-fill lần sau (nếu rememberMe)
    if (rememberMe) {
      localStorage.setItem('rememberedEmail', data.email)
    } else {
      localStorage.removeItem('rememberedEmail')
    }
  }

  function clearAuth() {
    accessToken.value = null
    refreshToken.value = null
    user.value = null

    // Xóa ở cả hai storage
    for (const s of [localStorage, sessionStorage]) {
      s.removeItem('accessToken')
      s.removeItem('refreshToken')
      s.removeItem('user')
    }
    localStorage.removeItem('rememberMe')
  }

  return {
    accessToken,
    refreshToken,
    user,
    isAuthenticated,
    userRole,
    isAdmin,
    rememberedEmail,
    setAuth,
    clearAuth,
  }
})
