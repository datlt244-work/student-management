/**
 * API Configuration và Helper Functions
 *
 * Trong development: Sử dụng Vite proxy (/api/v1 -> localhost:6868)
 * Trong production: Gọi trực tiếp qua VITE_API_BASE_URL (https://api.admin-datlt244.io.vn)
 */

import { useAuthStore } from '@/stores/auth'

/**
 * Lấy base URL cho API calls
 * - Development: Trả về empty string (dùng relative path với Vite proxy)
 * - Production: Trả về VITE_API_BASE_URL (https://api.admin-datlt244.io.vn)
 */
export function getApiBaseUrl(): string {
  const baseUrl = import.meta.env.VITE_API_BASE_URL
  const apiPath = import.meta.env.VITE_API_BASE_PATH || '/api/v1'

  // Nếu có VITE_API_BASE_URL, dùng nó (production)
  if (baseUrl) {
    return `${baseUrl}${apiPath}`
  }

  // Nếu không có, dùng relative path (development với Vite proxy)
  return apiPath
}

/**
 * Tạo full API URL từ endpoint
 * @param endpoint - Endpoint path (ví dụ: '/auth/login')
 * @returns Full API URL
 *
 * @example
 * // Development: '/api/v1/auth/login' (sẽ được proxy bởi Vite)
 * // Production: 'https://api.admin-datlt244.io.vn/api/v1/auth/login'
 * getApiUrl('/auth/login')
 */
export function getApiUrl(endpoint: string): string {
  const baseUrl = getApiBaseUrl()
  const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`
  return `${baseUrl}${cleanEndpoint}`
}

// Flag chống gọi refresh token đồng thời
let isRefreshing = false
let refreshPromise: Promise<boolean> | null = null

/**
 * Gọi API refresh token
 * @returns true nếu refresh thành công, false nếu thất bại
 */
async function doRefreshToken(): Promise<boolean> {
  const authStore = useAuthStore()

  if (!authStore.refreshToken) {
    return false
  }

  try {
    const url = getApiUrl('/auth/refresh-token')
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: authStore.refreshToken }),
    })

    if (!response.ok) {
      return false
    }

    const data = await response.json()
    const result = data.result || data

    // Giữ nguyên rememberMe setting hiện tại
    const rememberMe = localStorage.getItem('rememberMe') === 'true'
    authStore.setAuth(
      {
        accessToken: result.accessToken,
        refreshToken: result.refreshToken,
        userId: result.userId,
        email: result.email,
        role: result.role,
        profilePictureUrl: result.profilePictureUrl,
      },
      rememberMe,
    )

    return true
  } catch {
    return false
  }
}

/**
 * Refresh token với dedup — nhiều request 401 đồng thời chỉ gọi refresh 1 lần
 */
async function refreshTokenWithDedup(): Promise<boolean> {
  if (isRefreshing && refreshPromise) {
    return refreshPromise
  }

  isRefreshing = true
  refreshPromise = doRefreshToken().finally(() => {
    isRefreshing = false
    refreshPromise = null
  })

  return refreshPromise
}

/**
 * Fetch wrapper với:
 * - Auto base URL
 * - Auto Authorization header (nếu có token)
 * - Auto refresh token khi nhận 401 và retry request
 */
export async function apiFetch(
  endpoint: string,
  options?: RequestInit,
): Promise<Response> {
  const authStore = useAuthStore()
  const url = getApiUrl(endpoint)

  // Tự động thêm Authorization header nếu có token
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options?.headers as Record<string, string>),
  }

  if (authStore.accessToken && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${authStore.accessToken}`
  }

  const response = await fetch(url, { ...options, headers })

  // Nếu 401 và có refresh token → thử refresh rồi retry
  if (response.status === 401 && authStore.refreshToken) {
    // Không retry cho chính endpoint refresh-token hoặc login
    if (endpoint.includes('/auth/refresh-token') || endpoint.includes('/auth/login')) {
      return response
    }

    const refreshed = await refreshTokenWithDedup()

    if (refreshed) {
      // Retry request với token mới
      headers['Authorization'] = `Bearer ${authStore.accessToken}`
      return fetch(url, { ...options, headers })
    }

    // Refresh thất bại → clear auth và redirect về login
    authStore.clearAuth()
    window.location.href = '/login'
  }

  return response
}
