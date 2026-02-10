import { apiFetch } from '@/utils/api'

// ========== Types ==========

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED' | 'PENDING_VERIFICATION'

export interface AdminUserRoleSummary {
  roleId: number
  roleName: string
}

export interface AdminUserListItem {
  userId: string
  email: string
  role: AdminUserRoleSummary
  status: UserStatus
  emailVerified: boolean
  profilePictureUrl: string | null
  lastLoginAt: string | null
  loginCount: number
  createdAt: string
}

export interface AdminUserListResult {
  content: AdminUserListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// ========== API Functions ==========

/**
 * UC-11.1: Danh sách Users (Admin)
 * Endpoint: GET /admin/users
 * Query params: page, size, sort, search, status, roleId
 */
export async function getAdminUsers(params: {
  page?: number
  size?: number
  sort?: string
  search?: string
  status?: UserStatus | ''
  roleId?: number | ''
}): Promise<AdminUserListResult> {
  const searchParams = new URLSearchParams()

  if (params.page !== undefined) {
    // BE dùng page 0-based, UI dùng 1-based → chuyển đổi tại đây
    searchParams.set('page', String(Math.max(0, params.page)))
  }
  if (params.size !== undefined) {
    searchParams.set('size', String(params.size))
  }
  if (params.sort) {
    searchParams.set('sort', params.sort)
  }
  if (params.search) {
    searchParams.set('search', params.search.trim())
  }
  if (params.status) {
    searchParams.set('status', params.status)
  }
  if (params.roleId !== undefined && params.roleId !== '') {
    searchParams.set('roleId', String(params.roleId))
  }

  const queryString = searchParams.toString()
  const endpoint = queryString ? `/admin/users?${queryString}` : '/admin/users'

  const response = await apiFetch(endpoint)

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch users (${response.status})`)
  }

  const data = await response.json()
  return (data.result || data) as AdminUserListResult
}


