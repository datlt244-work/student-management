import { apiFetch } from '@/utils/api'

export interface ClassSessionResponse {
  sessionId: number
  roomId: number
  roomName: string
  dayOfWeek: number
  startTime: string
  endTime: string
}

export interface StudentAvailableClass {
  classId: number
  courseCode: string
  courseName: string
  credits: number
  teacherName: string
  sessions: ClassSessionResponse[]
  maxStudents: number
  currentStudents: number
  status: 'OPEN' | 'CLOSED' | 'CANCELLED'
}

export interface StudentEnrolledClass {
  enrollmentId: number
  classId: number
  courseCode: string
  courseName: string
  credits: number
  teacherName: string
  sessions: ClassSessionResponse[]
  enrollmentDate: string
}

export async function getAvailableClasses(): Promise<StudentAvailableClass[]> {
  const response = await apiFetch('/student/classes/available')
  if (response.ok) {
    const data = await response.json()
    return data.result
  }
  throw new Error('Failed to fetch available classes')
}

export async function getEnrolledClasses(): Promise<StudentEnrolledClass[]> {
  const response = await apiFetch('/student/classes/enrolled')
  if (response.ok) {
    const data = await response.json()
    return data.result
  }
  throw new Error('Failed to fetch enrolled classes')
}

export async function enrollInClass(classId: number): Promise<void> {
  const response = await apiFetch(`/student/classes/${classId}/enroll`, {
    method: 'POST',
  })
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}))
    throw new Error(errorData.message || 'Failed to enroll in class')
  }
}

export async function dropClass(classId: number): Promise<void> {
  const response = await apiFetch(`/student/classes/${classId}/enroll`, {
    method: 'DELETE',
  })
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}))
    throw new Error(errorData.message || 'Failed to drop class')
  }
}
