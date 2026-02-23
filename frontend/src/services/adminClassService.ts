import { apiFetch } from '@/utils/api';

/**
 * UC-14.1: Danh sách Lớp học (Admin)
 */
export interface AdminClassListItem {
  classId: number;
  courseName: string;
  courseCode: string;
  teacherName: string;
  semesterName: string;
  roomNumber: string;
  schedule: string;
  dayOfWeek?: number;
  startTime?: string;
  endTime?: string;
  status: 'OPEN' | 'CLOSED' | 'CANCELLED';
  maxStudents: number;
  studentCount: number;
}

export interface AdminClassListResponse {
  content: AdminClassListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface GetAdminClassesParams {
  search?: string;
  status?: string;
  semesterId?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export interface AdminCreateClassRequest {
  courseId: number;
  teacherId: string;
  semesterId: number;
  roomNumber: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  maxStudents: number;
}

/**
 * Lấy danh sách lớp học (Admin)
 */
export async function getAdminClasses(params: GetAdminClassesParams): Promise<AdminClassListResponse> {
  const query = new URLSearchParams();
  if (params.search) query.set('search', params.search);
  if (params.status) query.set('status', params.status);
  if (params.semesterId) query.set('semesterId', String(params.semesterId));
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  if (params.sort) query.set('sort', params.sort);

  const response = await apiFetch(`/admin/classes?${query.toString()}`);
  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    throw new Error(errorData?.message || `Failed to fetch classes (${response.status})`);
  }
  const data = await response.json();
  return (data.result || data) as AdminClassListResponse;
}

export async function createAdminClass(request: AdminCreateClassRequest): Promise<AdminClassListItem> {
  const response = await apiFetch('/admin/classes', {
    method: 'POST',
    body: JSON.stringify(request)
  });
  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    throw new Error(errorData?.message || `Failed to create class (${response.status})`);
  }
  const data = await response.json();
  return (data.result || data) as AdminClassListItem;
}
