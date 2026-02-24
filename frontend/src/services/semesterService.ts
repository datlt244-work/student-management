import { apiFetch } from '@/utils/api'
import type { ApiResponse } from './gradeService'

export interface SemesterResponse {
  semesterId: number
  name: string
  year: number
  isCurrent: boolean
  displayName: string
}

export const semesterService = {
  async getAllSemesters() {
    const response = await apiFetch('/semesters')
    return response.json() as Promise<ApiResponse<SemesterResponse[]>>
  },

  async getCurrentSemester() {
    const response = await apiFetch('/semesters/current')
    return response.json() as Promise<ApiResponse<SemesterResponse>>
  }
}
