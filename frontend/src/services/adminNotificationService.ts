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

export async function getNotificationHistory() {
  // Mocking for now as the backend history endpoint isn't fully ready
  // return apiFetch('/admin/notifications/history')
  return [
    {
      id: 1,
      title: 'System Maintenance Alert',
      body: 'Server maintenance scheduled for tonight at 12:00 AM.',
      type: 'Broadcast',
      recipients: '12,450',
      status: 'Sent',
      date: 'Oct 24, 2023',
      time: '09:15 AM',
      createdAt: '2023-10-24T09:15:00Z',
    },
    {
      id: 2,
      title: 'CS101 Assignment Due',
      body: 'Reminder: Assignment 3 is due tomorrow at 23:59.',
      type: 'Targeted',
      recipients: '45',
      status: 'Delivered',
      date: 'Oct 23, 2023',
      time: '14:30 PM',
      createdAt: '2023-10-23T14:30:00Z',
    },
    {
      id: 3,
      title: 'Exam Schedule Update',
      body: 'Final exam schedule has been revised for computer science students.',
      type: 'Targeted',
      recipients: '120',
      status: 'Delivered',
      date: 'Oct 22, 2023',
      time: '10:00 AM',
      createdAt: '2023-10-22T10:00:00Z',
    },
  ] as AdminNotificationListItem[]
}

export async function sendAdminNotification(request: SendNotificationRequest) {
  if (request.type === 'broadcast') {
    return apiFetch('/notifications/broadcast', {
      method: 'POST',
      body: JSON.stringify({
        title: request.title,
        body: request.body,
        actionUrl: request.actionUrl,
      }),
    })
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
