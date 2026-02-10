import { apiFetch } from '@/utils/api'

// ========== Types ==========

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
  confirmPassword: string
  logoutOtherDevices?: boolean
}

export interface ChangePasswordResponse {
  message: string
  loggedOutDevices: number
}

// ========== API Functions ==========

/**
 * Đổi mật khẩu (UC-10).
 * Endpoint: POST /users/me/change-password
 * Sau khi đổi mật khẩu, backend xóa toàn bộ refresh token của user.
 * Client nên logout và yêu cầu user đăng nhập lại.
 */
export async function changePassword(
  request: ChangePasswordRequest,
): Promise<ChangePasswordResponse> {
  const response = await apiFetch('/users/me/change-password', {
    method: 'POST',
    body: JSON.stringify({
      currentPassword: request.currentPassword,
      newPassword: request.newPassword,
      confirmPassword: request.confirmPassword,
      logoutOtherDevices: request.logoutOtherDevices ?? true,
    }),
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    const message = errorData?.message ?? `Change password failed (${response.status})`
    throw new Error(message)
  }

  const data = await response.json()
  return data.result as ChangePasswordResponse
}
