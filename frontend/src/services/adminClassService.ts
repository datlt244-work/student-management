import { apiFetch } from '@/utils/api';

/**
 * UC-14.1: Danh sách Lớp học (Admin)
 */
export interface AdminClassListItem {
  classId: number;
  courseName: string;
  courseCode: string;
  teacherName: string;
  teacherId: string;
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

export interface AdminUpdateClassRequest extends AdminCreateClassRequest {
  status: 'OPEN' | 'CLOSED' | 'CANCELLED';
}

export async function updateAdminClass(classId: number, request: AdminUpdateClassRequest): Promise<AdminClassListItem> {
  const response = await apiFetch(`/admin/classes/${classId}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    throw new Error(errorData?.message || `Failed to update class (${response.status})`);
  }
  const data = await response.json();
  return (data.result || data) as AdminClassListItem;
}

export async function deleteAdminClass(classId: number): Promise<void> {
  const response = await apiFetch(`/admin/classes/${classId}`, {
    method: 'DELETE'
  });
  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    throw new Error(errorData?.message || `Failed to delete class (${response.status})`);
  }
}

/**
 * UC-14.4: Chi tiết Lớp học & Danh sách Sinh viên
 */
export interface AdminClassStudent {
  enrollmentId: number;
  studentId: string;
  studentCode: string;
  fullName: string;
  email: string;
  enrollmentDate: string;
}

export interface AdminClassDetail extends AdminClassListItem {
  students: AdminClassStudent[];
}

export async function getAdminClassDetail(classId: number): Promise<AdminClassDetail> {
  const response = await apiFetch(`/admin/classes/${classId}`);
  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    throw new Error(errorData?.message || `Failed to fetch class detail (${response.status})`);
  }
  const data = await response.json();
  return (data.result || data) as AdminClassDetail;
}
