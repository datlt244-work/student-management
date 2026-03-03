import { apiFetch } from '@/utils/api'

export interface ClassEnrollmentStat {
  classId: number
  courseCode: string
  courseName: string
  teacherName: string
  maxSlot: number
  currentSlot: number
  fillRate: string
  status: string
}

export interface EnrollmentStatsResponse {
  totalClasses: number
  totalSlots: number
  filledSlots: number
  fillRate: string
  cacheActive: boolean
  classes: ClassEnrollmentStat[]
}

export async function getEnrollmentStats(
  semesterId: number,
): Promise<EnrollmentStatsResponse> {
  const res = await apiFetch(`/admin/semesters/${semesterId}/enrollment-stats`)
  const json = await res.json()
  return json.result
}
