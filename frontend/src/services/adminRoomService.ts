import { apiFetch } from '@/utils/api'

export interface AdminRoomResponse {
  roomId: number
  roomName: string
  capacity: number
  roomType: string
  notes: string | null
  status: boolean
  assignedTeacherId: string | null
  assignedTeacherName: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminCreateRoomRequest {
  roomName: string
  capacity: number
  roomType: string
  notes?: string
}

export interface AdminUpdateRoomRequest {
  roomName?: string
  capacity?: number
  roomType?: string
  notes?: string
  status?: boolean
}

interface PaginatedResponse {
  content: AdminRoomResponse[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const adminRoomService = {
  async getRooms(
    page: number = 1,
    size: number = 10,
    search?: string,
    roomType?: string,
  ): Promise<PaginatedResponse> {
    const params = new URLSearchParams()
    params.append('page', String(page - 1))
    params.append('size', String(size))
    if (search) params.append('search', search)
    if (roomType) params.append('roomType', roomType)

    const res = await apiFetch(`/admin/rooms?${params.toString()}`)
    const json = await res.json()
    return json.result
  },

  async createRoom(request: AdminCreateRoomRequest): Promise<AdminRoomResponse> {
    const res = await apiFetch('/admin/rooms', {
      method: 'POST',
      body: JSON.stringify(request),
    })
    const json = await res.json()
    return json.result
  },

  async updateRoom(id: number, request: AdminUpdateRoomRequest): Promise<AdminRoomResponse> {
    const res = await apiFetch(`/admin/rooms/${id}`, {
      method: 'PUT',
      body: JSON.stringify(request),
    })
    const json = await res.json()
    return json.result
  },

  async deleteRoom(id: number): Promise<void> {
    await apiFetch(`/admin/rooms/${id}`, {
      method: 'DELETE',
    })
  },

  async getAllRoomsSimple(): Promise<AdminRoomResponse[]> {
    const params = new URLSearchParams()
    params.append('page', '0')
    params.append('size', '1000')
    const res = await apiFetch(`/admin/rooms?${params.toString()}`)
    const json = await res.json()
    return json.result?.content || []
  },
}
