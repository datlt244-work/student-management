import { apiFetch } from '@/utils/api'

export interface ApiResponse<T> {
  result: T
  message?: string
  timestamp?: string
}

export interface StudentGrade {
  courseCode: string
  courseName: string
  credits: number
  grade: number | null
  grade4: number | null
  feedback: string | null
}

export interface StudentTranscript {
  grades: StudentGrade[]
  semesterGpa: number | null
  cumulativeGpa: number | null
  totalCredits: number
}

export const gradeService = {
  async getGradesBySemester(semesterId: number) {
    const response = await apiFetch(`/student/grades?semesterId=${semesterId}`)
    return response.json() as Promise<ApiResponse<StudentGrade[]>>
  },

  async getTranscript() {
    const response = await apiFetch('/student/grades/transcript')
    return response.json() as Promise<ApiResponse<StudentTranscript>>
  }
}
