<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { apiFetch } from '@/utils/api'

const notifications = ref<any[]>([])
const loading = ref(false)
const stats = ref({
  activeTokens: 12458,
  sentLast30Days: 845,
})

async function fetchNotifications() {
  loading.value = true
  try {
    // Trong thực tế sẽ gọi API lấy lịch sử thông báo
    // const response = await apiFetch('/notifications/history')
    // notifications.value = response

    // Mock data based on the requested UI
    notifications.value = [
      {
        id: 1,
        dateTime: 'Oct 24, 2023 09:15 AM',
        date: 'Oct 24, 2023',
        time: '09:15 AM',
        title: 'System Maintenance Alert',
        body: 'Server maintenance scheduled for...',
        type: 'Broadcast',
        recipients: '12,450',
        status: 'Sent',
      },
      {
        id: 2,
        dateTime: 'Oct 23, 2023 14:30 PM',
        date: 'Oct 23, 2023',
        time: '14:30 PM',
        title: 'CS101 Assignment Due',
        body: 'Reminder: Assignment 3 is due tomorrow...',
        type: 'Targeted',
        recipients: '45',
        status: 'Delivered',
      },
      {
        id: 3,
        dateTime: 'Oct 22, 2023 10:00 AM',
        date: 'Oct 22, 2023',
        time: '10:00 AM',
        title: 'Exam Schedule Update',
        body: 'Final exam schedule has been revised for...',
        type: 'Targeted',
        recipients: '120',
        status: 'Delivered',
      },
    ]
  } catch (error) {
    console.error('Failed to fetch notifications', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchNotifications()
})

function getStatusColor(status: string) {
  switch (status.toLowerCase()) {
    case 'sent':
      return 'bg-green-500'
    case 'delivered':
      return 'bg-green-500'
    case 'failed':
      return 'bg-red-500'
    default:
      return 'bg-stone-400'
  }
}

function getTypeClass(type: string) {
  switch (type.toLowerCase()) {
    case 'broadcast':
      return 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300'
    case 'targeted':
      return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300'
    case 'personal':
      return 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-300'
    default:
      return 'bg-stone-100 text-stone-800'
  }
}
</script>

<template>
  <div class="max-w-[1400px] w-full mx-auto p-6 md:p-10 flex flex-col gap-8">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-slate-900 dark:text-white text-3xl font-bold leading-tight tracking-tight">
          Notification Management
        </h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">
          Manage system-wide communication and view sent history.
        </p>
      </div>
      <div class="flex items-center gap-3">
        <button
          class="flex items-center gap-2 px-6 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95"
        >
          <span class="material-symbols-outlined text-[20px]">add_circle</span>
          Create Notification
        </button>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div
        class="bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex items-center gap-5"
      >
        <div class="p-4 bg-blue-50 dark:bg-blue-900/20 rounded-xl text-blue-600 dark:text-blue-400">
          <span class="material-symbols-outlined text-3xl">devices</span>
        </div>
        <div>
          <p class="text-sm font-medium text-slate-500 dark:text-slate-400 mb-1">
            Active FCM Tokens
          </p>
          <h3 class="text-3xl font-bold text-slate-900 dark:text-white">
            {{ stats.activeTokens.toLocaleString() }}
          </h3>
          <p class="text-xs text-green-600 font-medium mt-1 flex items-center gap-1">
            <span class="material-symbols-outlined text-sm">trending_up</span> +5.2% this week
          </p>
        </div>
      </div>
      <div
        class="bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex items-center gap-5"
      >
        <div
          class="p-4 bg-orange-50 dark:bg-orange-900/20 rounded-xl text-orange-600 dark:text-orange-400"
        >
          <span class="material-symbols-outlined text-3xl">send</span>
        </div>
        <div>
          <p class="text-sm font-medium text-slate-500 dark:text-slate-400 mb-1">
            Notifications Sent (Last 30 Days)
          </p>
          <h3 class="text-3xl font-bold text-slate-900 dark:text-white">
            {{ stats.sentLast30Days }}
          </h3>
          <p class="text-xs text-slate-400 mt-1">Avg. 28 daily</p>
        </div>
      </div>
    </div>

    <div
      class="bg-surface-light dark:bg-surface-dark p-4 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex flex-col lg:flex-row gap-4"
    >
      <div class="relative flex-1">
        <span
          class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
          >search</span
        >
        <input class="w-full pl-10 pr-4 h-11" placeholder="Search by title..." type="text" />
      </div>
      <div class="grid grid-cols-2 md:grid-cols-3 gap-4 lg:w-auto w-full">
        <select class="h-11">
          <option value="">Notification Type</option>
          <option value="broadcast">Broadcast</option>
          <option value="targeted">Targeted</option>
          <option value="personal">Personal</option>
        </select>
        <div class="relative col-span-1 md:col-span-1">
          <input class="w-full h-11 pl-10 pr-4" placeholder="Date Range" type="text" />
          <span
            class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]"
            >date_range</span
          >
        </div>
        <button
          class="flex items-center justify-center gap-2 h-11 px-4 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-slate-700 dark:text-slate-300 rounded-lg font-semibold transition-all"
        >
          <span class="material-symbols-outlined text-[20px]">filter_list</span>
          Clear
        </button>
      </div>
    </div>

    <div
      class="w-full overflow-hidden rounded-xl border border-stone-200 dark:border-stone-800 bg-surface-light dark:bg-surface-dark shadow-sm"
    >
      <div
        class="p-4 border-b border-stone-200 dark:border-stone-800 bg-stone-50/50 dark:bg-stone-900/30"
      >
        <h3 class="font-bold text-slate-800 dark:text-slate-200">Sent History</h3>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr
              class="bg-stone-50 dark:bg-stone-900/50 border-b border-stone-200 dark:border-stone-800"
            >
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Date/Time
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Title
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Type
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Recipients
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Status
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap text-right"
              >
                Actions
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-200 dark:divide-stone-800">
            <tr
              v-for="notif in notifications"
              :key="notif.id"
              class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors"
            >
              <td class="p-4 text-sm text-slate-500 whitespace-nowrap">
                {{ notif.date }}<br /><span class="text-xs">{{ notif.time }}</span>
              </td>
              <td class="p-4 whitespace-nowrap">
                <span class="text-sm font-bold text-slate-900 dark:text-white leading-none">{{
                  notif.title
                }}</span>
                <p class="text-xs text-slate-500 mt-1 truncate max-w-[200px]">{{ notif.body }}</p>
              </td>
              <td class="p-4 whitespace-nowrap">
                <span
                  :class="[
                    'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                    getTypeClass(notif.type),
                  ]"
                >
                  {{ notif.type }}
                </span>
              </td>
              <td class="p-4 whitespace-nowrap">
                <span class="text-sm text-slate-700 dark:text-slate-300 font-medium">{{
                  notif.recipients
                }}</span>
              </td>
              <td class="p-4 whitespace-nowrap">
                <div class="flex items-center gap-1.5">
                  <span
                    v-if="notif.status === 'Sent'"
                    class="w-2 h-2 rounded-full bg-green-500"
                  ></span>
                  <span v-else class="material-symbols-outlined text-green-500 text-[18px]"
                    >done_all</span
                  >
                  <span class="text-sm text-slate-600 dark:text-slate-400">{{ notif.status }}</span>
                </div>
              </td>
              <td class="p-4 text-right whitespace-nowrap">
                <div class="flex items-center justify-end gap-1">
                  <button
                    class="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-slate-600 dark:text-slate-300 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 rounded-md transition-colors"
                    title="View Details"
                  >
                    <span class="material-symbols-outlined text-[16px]">visibility</span>
                    Details
                  </button>
                  <button
                    v-if="notif.type === 'Broadcast'"
                    class="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 hover:bg-red-100 dark:hover:bg-red-900/30 rounded-md transition-colors"
                    title="Recall Message"
                  >
                    <span class="material-symbols-outlined text-[16px]">undo</span>
                    Recall
                  </button>
                  <button
                    v-else
                    class="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 hover:bg-red-100 dark:hover:bg-red-900/30 rounded-md transition-colors"
                    title="Delete Record"
                  >
                    <span class="material-symbols-outlined text-[16px]">delete</span>
                    Delete
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div
        class="p-4 border-t border-stone-200 dark:border-stone-800 bg-stone-50/50 dark:bg-stone-900/30 flex items-center justify-between"
      >
        <div class="flex items-center gap-4">
          <span class="text-sm text-slate-500 dark:text-slate-400">Records per page:</span>
          <select
            class="h-9 py-0 border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-sm rounded"
          >
            <option>20</option>
            <option>50</option>
            <option>100</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-slate-700 dark:text-slate-300 mr-2"
            >Page 1 of 12</span
          >
          <div class="flex gap-1">
            <button
              class="w-9 h-9 flex items-center justify-center rounded border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-400 hover:text-primary transition-colors disabled:opacity-50"
              disabled
            >
              <span class="material-symbols-outlined text-[18px]">chevron_left</span>
            </button>
            <button
              class="w-9 h-9 flex items-center justify-center rounded bg-primary text-white font-bold text-sm shadow-sm shadow-primary/20"
            >
              1
            </button>
            <button
              class="w-9 h-9 flex items-center justify-center rounded border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-600 dark:text-slate-400 hover:bg-stone-50 dark:hover:bg-stone-800 font-medium text-sm transition-colors"
            >
              2
            </button>
            <button
              class="w-9 h-9 flex items-center justify-center rounded border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-600 dark:text-slate-400 hover:bg-stone-50 dark:hover:bg-stone-800 font-medium text-sm transition-colors"
            >
              3
            </button>
            <button
              class="w-9 h-9 flex items-center justify-center rounded border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-400 hover:text-primary transition-colors"
            >
              <span class="material-symbols-outlined text-[18px]">chevron_right</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
