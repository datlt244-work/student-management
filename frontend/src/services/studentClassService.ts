import { apiFetch } from '@/utils/api'

export interface StudentAvailableClass {
  classId: number
  courseCode: string
  courseName: string
  credits: number
  teacherName: string
  schedule: string
  roomNumber: string
  maxStudents: number
  currentStudents: number
  status: 'OPEN' | 'CLOSED' | 'CANCELLED'
}

export async function getAvailableClasses(): Promise<StudentAvailableClass[]> {
  const response = await apiFetch('/student/classes/available')
  if (response.ok) {
    const data = await response.json()
    return data.result
  }
  throw new Error('Failed to fetch available classes')
}

// TODO: Add enroll and drop methods later for UC-16.2 and UC-16.3
