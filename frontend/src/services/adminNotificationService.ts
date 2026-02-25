import { apiFetch } from '@/utils/api'

export interface AdminNotificationListItem {
  id: string | number
  title: string
  body: string
  type: 'Broadcast' | 'Targeted' | 'Personal'
  recipients: string | number
  status: 'Pending' | 'Sent' | 'Failed' | 'Cancelled'
  date: string
  time: string
  createdAt: string
  scheduledAt?: string
  sentAt?: string
  actionUrl?: string
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
  scheduledAt?: string
}

export interface AdminNotificationListResult {
  content: AdminNotificationListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface NotificationStatsResult {
  totalSent: number
  broadcast: number
  targeted: number
  personal: number
  failed: number
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
    content: list.map((item: any) => {
      const displayDate = item.status === 'PENDING' ? item.scheduledAt : (item.sentAt || item.createdAt);
      return {
        id: item.sentId,
        title: item.title,
        body: item.body,
        type:
          item.notificationType === 'BROADCAST'
            ? 'Broadcast'
            : item.notificationType === 'TARGETED'
              ? 'Targeted'
              : 'Personal',
        recipients: item.recipientCount,
        status: item.status.charAt(0) + item.status.slice(1).toLowerCase(),
        date: new Date(displayDate).toLocaleDateString(),
        time: new Date(displayDate).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        createdAt: item.createdAt,
        scheduledAt: item.scheduledAt,
        sentAt: item.sentAt,
        actionUrl: item.actionUrl
      }
    }),
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
        scheduledAt: request.scheduledAt
      }),
    })

    if (!response.ok) {
      const errorData = await response.json().catch(() => null)
      throw new Error(errorData?.message || `Failed to send broadcast (${response.status})`)
    }
    return
  }

  if (request.type === 'targeted') {
    const response = await apiFetch('/notifications/send-targeted', {
      method: 'POST',
      body: JSON.stringify({
        title: request.title,
        body: request.body,
        actionUrl: request.actionUrl,
        role: request.role,
        departmentId: request.departmentId,
        classCode: request.classId,
        scheduledAt: request.scheduledAt
      }),
    })

    if (!response.ok) {
      const errorData = await response.json().catch(() => null)
      throw new Error(errorData?.message || `Failed to send targeted notification (${response.status})`)
    }
    return
  }

  if (request.type === 'personal') {
    const response = await apiFetch('/notifications/send-personal', {
      method: 'POST',
      body: JSON.stringify({
        title: request.title,
        body: request.body,
        actionUrl: request.actionUrl,
        recipientId: request.recipientId,
        scheduledAt: request.scheduledAt
      }),
    })

    if (!response.ok) {
      const errorData = await response.json().catch(() => null)
      throw new Error(errorData?.message || `Failed to send personal notification (${response.status})`)
    }
    return
  }

  // Targeted and Personal would go to other endpoints or shared endpoint with filters
  console.log('Sending targeted/personal notification', request)
  return Promise.resolve()
}

export interface RecipientSearchResponse {
  name: string
  identifier: string
  role: string
}

export async function searchRecipients(query: string): Promise<RecipientSearchResponse[]> {
  const response = await apiFetch(`/notifications/search-recipients?query=${encodeURIComponent(query)}`)
  if (!response.ok) return []
  const data = await response.json()
  return data.result || data
}

export async function getNotificationStats(): Promise<{ activeTokens: number; sentLast30Days: number }> {
  const response = await apiFetch('/notifications/stats')
  if (!response.ok) return { activeTokens: 0, sentLast30Days: 0 }
  const data = await response.json()
  return data.result || data
}

export async function deleteNotification(id: string | number) {
  const response = await apiFetch(`/notifications/history/${id}`, { method: 'DELETE' })
  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to delete/recall notification (${response.status})`)
  }
}
