import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export interface AuthUser {
  userId: string
  email: string
  role: string | null
  profilePictureUrl: string | null
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
  const user = ref<AuthUser | null>(loadUser())

  const isAuthenticated = computed(() => !!accessToken.value)
  const userRole = computed(() => user.value?.role?.toLowerCase() ?? null)
  const isAdmin = computed(() => userRole.value === 'admin')

  function loadUser(): AuthUser | null {
    const raw = localStorage.getItem('user')
    if (!raw) return null
    try {
      return JSON.parse(raw)
    } catch {
      return null
    }
  }

  function setAuth(data: {
    accessToken: string
    refreshToken: string
    userId: string
    email: string
    role: string | null
    profilePictureUrl: string | null
  }) {
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    user.value = {
      userId: data.userId,
      email: data.email,
      role: data.role,
      profilePictureUrl: data.profilePictureUrl,
    }

    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function clearAuth() {
    accessToken.value = null
    refreshToken.value = null
    user.value = null

    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
  }

  return {
    accessToken,
    refreshToken,
    user,
    isAuthenticated,
    userRole,
    isAdmin,
    setAuth,
    clearAuth,
  }
})

