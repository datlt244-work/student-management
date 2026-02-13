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
  fullName?: string
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

// ========== Admin health ==========

export interface HealthComponent {
  status: string
  details: string
  latencyMs?: number | null
}

export interface AdminHealthResponse {
  overallStatus: string
  timestamp: string
  backend: HealthComponent
  database: HealthComponent
  redis: HealthComponent
  minio: HealthComponent
  nginx: HealthComponent
  frontend: HealthComponent
}

export async function getAdminHealth(): Promise<AdminHealthResponse> {
  const response = await apiFetch('/admin/health')
  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch health (${response.status})`)
  }
  const data = await response.json()
  return (data.result || data) as AdminHealthResponse
}

// ========== UC-11.5: Update User Status ==========

export interface AdminUpdateUserStatusRequest {
  status: UserStatus
  banReason?: string
}

export async function updateAdminUserStatus(
  userId: string,
  body: AdminUpdateUserStatusRequest,
): Promise<AdminUserDetailResult> {
  const endpoint = `/admin/users/${encodeURIComponent(userId)}/status`
  const response = await apiFetch(endpoint, {
    method: 'PATCH',
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    const message = errorData?.message || `Failed to update user status (${response.status})`
    throw new Error(message)
  }

  const data = await response.json()
  return (data.result || data) as AdminUserDetailResult
}

// ========== UC-11.7: Soft Delete User ==========

export async function deleteAdminUser(userId: string): Promise<void> {
  const endpoint = `/admin/users/${encodeURIComponent(userId)}`
  const response = await apiFetch(endpoint, {
    method: 'DELETE',
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    const message = errorData?.message || `Failed to delete user (${response.status})`
    throw new Error(message)
  }
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

// ========== Departments (for dropdown) ==========

export interface AdminDepartmentItem {
  departmentId: number
  name: string
  officeLocation?: string
}

/**
 * GET /admin/departments — Danh sách khoa (cho select khi tạo user).
 * Endpoint đơn giản không có pagination.
 */
export async function getAdminDepartments(): Promise<AdminDepartmentItem[]> {
  const response = await apiFetch('/admin/departments/simple')
  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch departments (${response.status})`)
  }
  const data = await response.json()
  return (data.result ?? data) as AdminDepartmentItem[]
}

// ========== UC-13.1: Department List (Admin) ==========

export interface AdminDepartmentListItem {
  departmentId: number
  name: string
  officeLocation: string | null
  courseCount: number
  createdAt: string
  status: 'ACTIVE' | 'INACTIVE'
}

export interface AdminDepartmentListResult {
  content: AdminDepartmentListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  totalCourses: number
}

/**
 * UC-13.1: Danh sách Departments (Admin) với pagination
 * Endpoint: GET /admin/departments
 * Query params: page, size, sort, search
 */
export async function getAdminDepartmentList(params: {
  page?: number
  size?: number
  sort?: string
  search?: string
  status?: string
}): Promise<AdminDepartmentListResult> {
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

  const queryString = searchParams.toString()
  const endpoint = queryString ? `/admin/departments?${queryString}` : '/admin/departments'

  const response = await apiFetch(endpoint)

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch departments (${response.status})`)
  }

  const data = await response.json()
  return (data.result || data) as AdminDepartmentListResult
}

// ========== UC-13.2: Create Department ==========

export interface AdminCreateDepartmentRequest {
  name: string
  officeLocation?: string
}

export interface AdminDepartmentDetailResponse {
  departmentId: number
  name: string
  officeLocation: string | null
  createdAt: string
  status: 'ACTIVE' | 'INACTIVE'
}

/**
 * UC-13.2: Tạo Department (Admin)
 * Endpoint: POST /admin/departments
 */
export async function createAdminDepartment(
  body: AdminCreateDepartmentRequest,
): Promise<AdminDepartmentDetailResponse> {
  const response = await apiFetch('/admin/departments', {
    method: 'POST',
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    const message = errorData?.message || `Failed to create department (${response.status})`
    throw new Error(message)
  }

  const data = await response.json()
  return (data.result || data) as AdminDepartmentDetailResponse
}

// ========== UC-13.3: Update Department ==========

export interface AdminUpdateDepartmentRequest {
  name?: string
  officeLocation?: string
}

/**
 * UC-13.3: Cập Nhật Department (Admin)
 * Endpoint: PUT /admin/departments/{departmentId}
 */
export async function updateAdminDepartment(
  departmentId: number,
  body: AdminUpdateDepartmentRequest,
): Promise<AdminDepartmentDetailResponse> {
  const response = await apiFetch(`/admin/departments/${departmentId}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    const message = errorData?.message || `Failed to update department (${response.status})`
    throw new Error(message)
  }

  const data = await response.json()
  return (data.result || data) as AdminDepartmentDetailResponse
}

// ========== UC-13.4: Delete Department ==========

/**
 * UC-13.4: Xóa Department (Soft Delete)
 * Endpoint: DELETE /admin/departments/{departmentId}
 */
export async function deleteAdminDepartment(departmentId: number): Promise<void> {
  const response = await apiFetch(`/admin/departments/${departmentId}`, {
    method: 'DELETE',
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    const message = errorData?.message || `Failed to delete department (${response.status})`
    throw new Error(message)
  }
}

/**
 * UC-13.5: Update Department Status (Active/Inactive)
 * PATCH /admin/departments/{departmentId}/status
 */
export async function updateAdminDepartmentStatus(
  departmentId: number,
  status: 'ACTIVE' | 'INACTIVE',
): Promise<AdminDepartmentDetailResponse> {
  const response = await apiFetch(`/admin/departments/${departmentId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    const message = errorData?.message || `Failed to update department status (${response.status})`
    throw new Error(message)
  }

  const data = await response.json()
  return (data.result || data) as AdminDepartmentDetailResponse
}

// ========== UC-11.3a Create User Request ==========

export interface AdminCreateUserRequest {
  role: 'TEACHER' | 'STUDENT'
  email: string
  departmentId: number
  firstName: string
  lastName: string
  phone?: string
  // Teacher-only
  teacherCode?: string
  specialization?: string
  academicRank?: string
  officeRoom?: string
  degreesQualification?: string
  // Student-only
  studentCode?: string
  dob?: string // YYYY-MM-DD
  gender?: 'MALE' | 'FEMALE' | 'OTHER'
  major?: string
  address?: string
  year?: number
  manageClass?: string
}

// ========== UC - Admin Update User Profile ==========

export interface AdminUpdateUserProfileRequest {
  // Common
  firstName?: string
  lastName?: string
  phone?: string
  departmentId?: number
  // Teacher-only
  teacherCode?: string
  specialization?: string
  academicRank?: string
  officeRoom?: string
  degreesQualification?: string
  // Student-only
  studentCode?: string
  dob?: string // YYYY-MM-DD
  gender?: 'MALE' | 'FEMALE' | 'OTHER'
  major?: string
  address?: string
  year?: number
  manageClass?: string
}

/**
 * Admin update profile fields for Teacher/Student
 * Endpoint: PUT /admin/users/{userId}/profile
 */
export async function updateAdminUserProfile(
  userId: string,
  body: AdminUpdateUserProfileRequest,
): Promise<AdminUserDetailResult> {
  const endpoint = `/admin/users/${encodeURIComponent(userId)}/profile`
  const response = await apiFetch(endpoint, {
    method: 'PUT',
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    const message = errorData?.message || `Failed to update user (${response.status})`
    throw new Error(message)
  }

  const data = await response.json()
  return (data.result || data) as AdminUserDetailResult
}

/**
 * UC-11.3a: Tạo User (Teacher hoặc Student)
 * POST /admin/users — 201 Created, trả về user + profile.
 */
export async function createAdminUser(body: AdminCreateUserRequest): Promise<AdminUserDetailResult> {
  const response = await apiFetch('/admin/users', {
    method: 'POST',
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    const message = errorData?.message || `Failed to create user (${response.status})`
    throw new Error(message)
  }

  const data = await response.json()
  return (data.result || data) as AdminUserDetailResult
}

// ========== Courses (Admin) ==========

export interface AdminCourseListItem {
  courseId: number
  code: string
  name: string
  credits: number
  departmentId?: number
  departmentName?: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
}

export interface AdminCourseListResult {
  content: AdminCourseListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function getAdminCourses(params: {
  page?: number
  size?: number
  search?: string
  status?: string | ''
  departmentId?: number | ''
  sort?: string
}): Promise<AdminCourseListResult> {
  const searchParams = new URLSearchParams()

  if (params.page !== undefined) {
    // BE uses 0-based index
    searchParams.set('page', String(Math.max(0, params.page - 1)))
  }
  if (params.size !== undefined) {
    searchParams.set('size', String(params.size))
  }
  if (params.search) {
    searchParams.set('search', params.search.trim())
  }
  if (params.status) {
    searchParams.set('status', params.status)
  }
  if (params.departmentId) {
    searchParams.set('departmentId', String(params.departmentId))
  }
  if (params.sort) {
    searchParams.set('sort', params.sort)
  }

  const queryString = searchParams.toString()
  const endpoint = queryString ? `/admin/courses?${queryString}` : '/admin/courses'

  const response = await apiFetch(endpoint)
  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch courses (${response.status})`)
  }
  const data = await response.json()
  return (data.result || data) as AdminCourseListResult
}


