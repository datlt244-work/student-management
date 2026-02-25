import { apiFetch } from '@/utils/api'

export interface AdminNotificationListItem {
  id: string | number
  title: string
  body: string
  type: 'Broadcast' | 'Targeted' | 'Personal'
  recipients: string | number
  status: 'Sent' | 'Delivered' | 'Failed'
  date: string
  time: string
  createdAt: string
}

export interface SendNotificationRequest {
  title: string
  body: string
  type: string
  actionUrl?: string
  role?: string
  departmentId?: number
  classId?: string
  recipientId?: string
}

export interface AdminNotificationListResult {
  content: AdminNotificationListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function getNotificationHistory(params: {
  page?: number
  size?: number
  search?: string
  type?: string
  startDate?: string
  endDate?: string
}): Promise<AdminNotificationListResult> {
  const searchParams = new URLSearchParams()
  if (params.page !== undefined) searchParams.set('page', String(Math.max(0, params.page - 1)))
  if (params.size !== undefined) searchParams.set('size', String(params.size))
  if (params.search) searchParams.set('search', params.search.trim())
  if (params.type) searchParams.set('type', params.type.toUpperCase())
  if (params.startDate) searchParams.set('startDate', params.startDate)
  if (params.endDate) searchParams.set('endDate', params.endDate)

  const queryString = searchParams.toString()
  const endpoint = queryString ? `/notifications/history?${queryString}` : '/notifications/history'

  const response = await apiFetch(endpoint)
  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch history (${response.status})`)
  }
  const data = await response.json()
  const raw = data.result || data
  const list = (raw.content || []) as any[]

  return {
    content: list.map((item: any) => ({
      id: item.sentId,
      title: item.title,
      body: item.body,
      type: item.notificationType === 'BROADCAST' ? 'Broadcast' : 'Targeted',
      recipients: item.recipientCount,
      status: 'Sent',
      date: new Date(item.createdAt).toLocaleDateString(),
      time: new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      createdAt: item.createdAt,
    })),
    page: raw.number ?? 0,
    size: raw.size ?? 20,
    totalElements: raw.totalElements ?? 0,
    totalPages: raw.totalPages ?? 0,
  }
}

export async function sendAdminNotification(request: SendNotificationRequest) {
  if (request.type === 'broadcast') {
    const response = await apiFetch('/notifications/broadcast', {
      method: 'POST',
      body: JSON.stringify({
        title: request.title,
        body: request.body,
        actionUrl: request.actionUrl,
      }),
    })

    if (!response.ok) {
      const errorData = await response.json().catch(() => null)
      throw new Error(errorData?.message || `Failed to send broadcast (${response.status})`)
    }
    return
  }

  // Targeted and Personal would go to other endpoints or shared endpoint with filters
  console.log('Sending targeted/personal notification', request)
  return Promise.resolve()
}

export async function deleteNotification(id: string | number) {
  // return apiFetch(`/admin/notifications/${id}`, { method: 'DELETE' })
  console.log('Deleting notification', id)
  return Promise.resolve()
}
