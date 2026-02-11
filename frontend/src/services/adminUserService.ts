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

// UC-11.2: User detail + profile
export interface AdminUserDetailDepartment {
  departmentId: number
  name: string
}

export interface AdminUserDetailStudentProfile {
  studentId: string
  studentCode: string
  firstName: string
  lastName: string
  dob: string | null
  gender: string | null
  major: string | null
  email: string
  phone: string | null
  address: string | null
  gpa: number | null
  year: number | null
  manageClass: string | null
  department: AdminUserDetailDepartment | null
}

export interface AdminUserDetailTeacherProfile {
  teacherId: string
  teacherCode: string
  firstName: string
  lastName: string
  email: string
  phone: string | null
  specialization: string | null
  academicRank: string | null
  officeRoom: string | null
  degreesQualification: string | null
  department: AdminUserDetailDepartment | null
}

export interface AdminUserDetailResult {
  userId: string
  email: string
  role: AdminUserRoleSummary
  status: UserStatus
  emailVerified: boolean
  banReason: string | null
  lastLoginAt: string | null
  loginCount: number
  createdAt: string
  studentProfile: AdminUserDetailStudentProfile | null
  teacherProfile: AdminUserDetailTeacherProfile | null
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

/**
 * UC-11.2: Xem chi tiết User (Admin)
 * Endpoint: GET /admin/users/{userId}
 * Trả về user + teacherProfile hoặc studentProfile theo role.
 */
export async function getAdminUserById(userId: string): Promise<AdminUserDetailResult> {
  const endpoint = `/admin/users/${encodeURIComponent(userId)}`
  const response = await apiFetch(endpoint)

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    const message = errorData?.message || `Failed to fetch user (${response.status})`
    if (response.status === 404) {
      throw new Error('USER_NOT_FOUND')
    }
    throw new Error(message)
  }

  const data = await response.json()
  return (data.result || data) as AdminUserDetailResult
}


