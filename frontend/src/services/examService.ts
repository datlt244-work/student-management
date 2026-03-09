import { apiFetch } from '@/utils/api'

export interface ExamResponse {
  examId: string
  classId: number
  courseCode: string
  courseName: string
  roomNumber: string
  examDate: string
  startTime: string
  endTime: string
  examType: 'FINAL' | 'RESIT'
  note?: string
  isEligible: boolean
  totalAbsent: number
  absentPercentage: number
  reason?: string
  semesterStartDate: string
  semesterEndDate: string
}

export async function getStudentExams(semesterId: number): Promise<ExamResponse[]> {
  const response = await apiFetch(`/student/exams?semesterId=${semesterId}`)
  const data = await response.json()
  if (response.ok) {
    return data.result
  }
  return []
}
