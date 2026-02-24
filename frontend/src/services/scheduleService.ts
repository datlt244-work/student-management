import { apiFetch } from '@/utils/api'

export interface StudentSchedule {
  courseCode: string
  courseName: string
  teacherName: string
  roomNumber: string
  dayOfWeek: number
  startTime: string
  endTime: string
  classStatus: string
}

/**
 * Lấy thời khóa biểu của sinh viên theo học kỳ (UC-18).
 * Endpoint: GET /student/schedule
 */
export async function getMySchedule(semesterId: number): Promise<StudentSchedule[]> {
  const response = await apiFetch(`/student/schedule?semesterId=${semesterId}`)

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch schedule (${response.status})`)
  }

  const data = await response.json()
  return data.result as StudentSchedule[]
}
