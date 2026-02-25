import { apiFetch } from '@/utils/api'

export interface UserNotification {
  notificationId: string
  title: string
  body: string
  isRead: boolean
  actionUrl?: string
  notificationType: string
  createdAt: string
}

export interface NotificationListResponse {
  content: UserNotification[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export async function getMyNotifications(page = 0, size = 20): Promise<NotificationListResponse> {
  const response = await apiFetch(`/notifications/my?page=${page}&size=${size}`)
  if (!response.ok) {
    throw new Error('Failed to fetch notifications')
  }
  const data = await response.json()
  return data.result || data
}

export async function getUnreadCount(): Promise<number> {
  const response = await apiFetch('/notifications/unread-count')
  if (!response.ok) return 0
  const data = await response.json()
  return data.result ?? data
}

export async function markAsRead(notificationId: string): Promise<void> {
  const response = await apiFetch(`/notifications/${notificationId}/read`, {
    method: 'PATCH'
  })
  if (!response.ok) {
    throw new Error('Failed to mark notification as read')
  }
}

export async function markAllAsRead(): Promise<void> {
  const response = await apiFetch('/notifications/read-all', {
    method: 'PATCH'
  })
  if (!response.ok) {
    throw new Error('Failed to mark all notifications as read')
  }
}
