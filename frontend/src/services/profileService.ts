import { apiFetch } from '@/utils/api'

// ========== Types ==========

export interface DepartmentInfo {
  departmentId: number
  name: string
  officeLocation: string | null
}

export interface SemesterInfo {
  semesterId: number
  name: string
  year: number
  displayName: string
  startDate: string | null
  endDate: string | null
}

export interface StudentProfile {
  studentId: string
  studentCode: string | null
  firstName: string
  lastName: string
  dob: string | null
  gender: string | null
  major: string | null
  phone: string | null
  address: string | null
  gpa: number | null
  department: DepartmentInfo | null
  currentSemester: SemesterInfo | null
}

export interface TeacherProfile {
  teacherId: string
  teacherCode: string | null
  firstName: string
  lastName: string
  phone: string | null
  specialization: string | null
  academicRank: string | null
  officeRoom: string | null
  department: DepartmentInfo | null
}

export interface CombinedProfile {
  // From users table
  userId: string
  email: string
  profilePictureUrl: string | null
  role: string
  status: string
  emailVerified: boolean
  lastLoginAt: string | null
  loginCount: number
  createdAt: string

  // From students table (null if not STUDENT)
  studentProfile: StudentProfile | null

  // From teachers table (null if not TEACHER)
  teacherProfile: TeacherProfile | null
}

// ========== API Functions ==========

/**
 * Lấy thông tin profile đầy đủ: User + Student/Teacher profile
 * Endpoint: GET /profile/me
 */
export async function getMyProfile(): Promise<CombinedProfile> {
  const response = await apiFetch('/profile/me')

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch profile (${response.status})`)
  }

  const data = await response.json()
  return data.result as CombinedProfile
}
