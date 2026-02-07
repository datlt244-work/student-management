/**
 * API Configuration và Helper Functions
 * 
 * Trong development: Sử dụng Vite proxy (/api/v1 -> localhost:6868)
 * Trong production: Gọi trực tiếp qua VITE_API_BASE_URL (https://api.admin-datlt244.io.vn)
 */

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

/**
 * Default fetch wrapper với base URL tự động
 */
export async function apiFetch(
  endpoint: string,
  options?: RequestInit
): Promise<Response> {
  const url = getApiUrl(endpoint)
  return fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  })
}

