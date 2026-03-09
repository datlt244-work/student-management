import { apiFetch } from '@/utils/api'

export interface AttendanceAppealRequest {
  classId: number
  attendanceDate: string
  reason: string
  evidenceUrl: string
}

export interface AttendanceAppealResponse {
  appealId: string
  classId: number
  courseName: string
  attendanceDate: string
  reason: string
  evidenceUrl: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  teacherResponse?: string
  createdAt: string
}

export interface EvidenceUploadResponse {
  evidenceUrl: string
}

export async function submitAttendanceAppeal(request: AttendanceAppealRequest): Promise<AttendanceAppealResponse> {
  const response = await apiFetch('/student/attendance-appeals', {
    method: 'POST',
    body: JSON.stringify(request),
  })
  
  const data = await response.json()
  if (response.ok) {
    return data.result
  }
  throw new Error(data.message || 'Failed to submit appeal')
}

export async function getStudentAppeals(): Promise<AttendanceAppealResponse[]> {
  const response = await apiFetch('/student/attendance-appeals')
  const data = await response.json()
  if (response.ok) {
    return data.result
  }
  return []
}

export async function uploadAppealEvidence(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiFetch('/student/attendance-appeals/upload-evidence', {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to upload evidence (${response.status})`)
  }

  const data = await response.json()
  return data.result.evidenceUrl
}
