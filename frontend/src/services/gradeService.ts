import { apiFetch } from '@/utils/api'

export interface ApiResponse<T> {
  result: T
  message?: string
  timestamp?: string
}

export interface StudentAssessmentScore {
  category: string
  itemName: string
  weight: number
  value: number | null
  comment: string | null
  isTotal: boolean
}

export interface StudentGrade {
  enrollmentId: number
  semesterName: string
  semesterYear: number
  courseCode: string
  courseName: string
  classCode: string
  fromDate: string
  toDate: string
  credits: number
  grade: number | null
  grade4: number | null
  status: 'PASSED' | 'FAILED' | 'IN_PROGRESS'
  feedback: string | null
  assessmentScores: StudentAssessmentScore[]
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
  },
}
