import { apiFetch } from '@/utils/api';

export interface AdminRoomResponse {
  roomId: number;
  roomName: string;
  capacity?: number;
  roomType?: string;
  assignedTeacherId?: string;
  assignedTeacherName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminRoomListResponse {
  content: AdminRoomResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AdminCreateRoomRequest {
  roomName: string;
  capacity: number;
  roomType: string;
}

export interface AdminUpdateRoomRequest {
  roomName: string;
  capacity: number;
  roomType: string;
}

export const adminRoomService = {
  getRooms: async (
    page: number = 1,
    size: number = 10,
    search?: string,
    roomType?: string
  ): Promise<AdminRoomListResponse> => {
    const params = new URLSearchParams({
      page: (page - 1).toString(), // Backend expects 0-indexed page
      size: size.toString(),
    });
    if (search) params.append('search', search);
    if (roomType) params.append('roomType', roomType);

    const response = await apiFetch(`/api/v1/admin/rooms?${params.toString()}`);
    if (!response.ok) throw new Error('Failed to fetch rooms');
    const result = await response.json();
    return result.result;
  },

  getRoomDetails: async (id: number): Promise<AdminRoomResponse> => {
    const response = await apiFetch(`/api/v1/admin/rooms/${id}`);
    if (!response.ok) throw new Error('Failed to fetch room details');
    const result = await response.json();
    return result.result;
  },

  createRoom: async (data: AdminCreateRoomRequest): Promise<AdminRoomResponse> => {
    const response = await apiFetch('/api/v1/admin/rooms', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || 'Failed to create room');
    }
    const result = await response.json();
    return result.result;
  },

  updateRoom: async (id: number, data: AdminUpdateRoomRequest): Promise<AdminRoomResponse> => {
    const response = await apiFetch(`/api/v1/admin/rooms/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || 'Failed to update room');
    }
    const result = await response.json();
    return result.result;
  },

  deleteRoom: async (id: number): Promise<void> => {
    const response = await apiFetch(`/api/v1/admin/rooms/${id}`, {
      method: 'DELETE',
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || 'Failed to delete room');
    }
  },

  /**
   * Fetch all rooms (no pagination) for dropdown/select usage.
   * Uses a large page size to get all rooms in one call.
   */
  getAllRoomsSimple: async (): Promise<AdminRoomResponse[]> => {
    const response = await apiFetch('/api/v1/admin/rooms?page=0&size=1000');
    if (!response.ok) throw new Error('Failed to fetch rooms');
    const result = await response.json();
    return result.result?.content ?? [];
  },
};

