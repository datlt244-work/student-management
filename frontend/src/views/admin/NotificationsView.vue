<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  getNotificationHistory,
  sendAdminNotification,
  deleteNotification,
  type AdminNotificationListItem,
} from '@/services/adminNotificationService'
import { useToast } from '@/composables/useToast'

const { toast, showToast } = useToast()

// Data states
const notifications = ref<AdminNotificationListItem[]>([])
const isLoading = ref(false)
const isSending = ref(false)
const stats = ref({
  activeTokens: 12458,
  sentLast30Days: 845,
})

// Modal states
const showCreateModal = ref(false)
const showDeleteConfirmModal = ref(false)
const deleteTargetId = ref<string | number | null>(null)
const isDeleting = ref(false)

// Form states
const activeTab = ref('broadcast') // 'broadcast', 'targeted', 'personal'
const form = ref({
  title: 'Exam Schedule Update',
  body: 'Please be advised that the final exam schedule for the Fall 2023 semester has been updated. Check your portal for details.',
  actionUrl: '',
  role: 'All Roles',
  department: 'All Departments',
  class: 'All Classes',
  recipientId: '',
})

// Filter states
const searchQuery = ref('')
const typeFilter = ref('')
const currentPage = ref(1)
const totalPages = ref(12)
const pageSize = ref(20)

async function fetchNotifications() {
  isLoading.value = true
  try {
    notifications.value = await getNotificationHistory()
  } catch (error) {
    showToast('Failed to fetch notification history', 'error')
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchNotifications()
})

async function handleSend() {
  if (!form.value.title.trim() || !form.value.body.trim()) {
    showToast('Title and body are required', 'error')
    return
  }

  isSending.value = true
  try {
    await sendAdminNotification({
      ...form.value,
      type: activeTab.value,
    })
    showToast('Notification sent successfully')
    showCreateModal.value = false
    await fetchNotifications()
  } catch (error) {
    showToast('Failed to send notification', 'error')
  } finally {
    isSending.value = false
  }
}

function openDeleteConfirm(id: string | number) {
  deleteTargetId.value = id
  showDeleteConfirmModal.value = true
}

async function confirmDelete() {
  if (deleteTargetId.value === null) return
  isDeleting.value = true
  try {
    await deleteNotification(deleteTargetId.value)
    showToast('Notification deleted/recalled')
    showDeleteConfirmModal.value = false
    await fetchNotifications()
  } catch (error) {
    showToast('Failed to delete notification', 'error')
  } finally {
    isDeleting.value = false
  }
}

function getTypeClass(type: string) {
  switch (type.toLowerCase()) {
    case 'broadcast':
      return 'bg-purple-50 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400'
    case 'targeted':
      return 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'
    case 'personal':
      return 'bg-orange-50 dark:bg-orange-900/30 text-orange-600 dark:text-orange-400'
    default:
      return 'bg-stone-50 dark:bg-stone-800 text-stone-600 dark:text-stone-400'
  }
}

const paginationPages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const pages: number[] = []
  const start = Math.max(1, current - 1)
  const end = Math.min(total, current + 1)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    // fetch data for page
  }
}

function clearFilters() {
  searchQuery.value = ''
  typeFilter.value = ''
}
</script>

<template>
  <!-- Toast feedback -->
  <Transition name="toast">
    <div
      v-if="toast"
      :class="[
        'fixed top-6 right-6 z-[200] flex items-center gap-3 px-5 py-3.5 rounded-xl shadow-2xl text-sm font-medium border backdrop-blur-sm',
        toast.type === 'success'
          ? 'bg-green-50 dark:bg-green-900/40 border-green-200 dark:border-green-700 text-green-800 dark:text-green-300'
          : 'bg-red-50 dark:bg-red-900/40 border-red-200 dark:border-red-700 text-red-800 dark:text-red-300',
      ]"
    >
      <span class="material-symbols-outlined text-[20px]">
        {{ toast.type === 'success' ? 'check_circle' : 'error' }}
      </span>
      {{ toast.message }}
    </div>
  </Transition>

  <div class="max-w-[1400px] w-full mx-auto p-6 md:p-10 flex flex-col gap-8 relative">
    <!-- Header -->
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
        <div
          v-if="isLoading"
          class="flex items-center gap-2 px-3 py-1.5 rounded-full bg-stone-100 dark:bg-stone-800 text-xs font-medium text-slate-600 dark:text-slate-300 mr-2"
        >
          <span
            class="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin"
          ></span>
          <span>Loading...</span>
        </div>
        <button
          @click="showCreateModal = true"
          class="flex items-center gap-2 px-6 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95"
        >
          <span class="material-symbols-outlined text-[20px]">add_circle</span>
          Create Notification
        </button>
      </div>
    </div>

    <!-- Stats -->
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

    <!-- Filters -->
    <div
      class="bg-surface-light dark:bg-surface-dark p-4 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex flex-col lg:flex-row gap-4"
    >
      <div class="relative flex-1 min-w-0">
        <span
          class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]"
          >search</span
        >
        <input
          v-model="searchQuery"
          class="w-full pl-10 pr-4 h-11 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
          placeholder="Search by title..."
          type="text"
        />
      </div>
      <div class="flex flex-wrap gap-2 lg:gap-3 items-center justify-end shrink-0">
        <select
          v-model="typeFilter"
          class="h-11 px-3 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary min-w-[150px]"
        >
          <option value="">All Types</option>
          <option value="broadcast">Broadcast</option>
          <option value="targeted">Targeted</option>
          <option value="personal">Personal</option>
        </select>
        <div class="relative">
          <input
            class="h-11 pl-10 pr-4 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
            placeholder="Date Range"
            type="text"
          />
          <span
            class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]"
            >date_range</span
          >
        </div>
        <button
          @click="clearFilters"
          class="flex items-center justify-center gap-2 h-11 px-4 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-slate-700 dark:text-slate-300 rounded-lg font-semibold transition-all"
        >
          <span class="material-symbols-outlined text-[20px]">filter_list</span>
          Clear
        </button>
      </div>
    </div>

    <!-- Table -->
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
                    'inline-flex items-center px-2.5 py-0.5 rounded-md text-xs font-bold',
                    getTypeClass(notif.type),
                  ]"
                >
                  {{ notif.type.toUpperCase() }}
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
                  <span class="text-sm text-slate-600 dark:text-slate-400 font-medium">{{
                    notif.status
                  }}</span>
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
                    @click="openDeleteConfirm(notif.id)"
                    class="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 hover:bg-red-100 dark:hover:bg-red-900/30 rounded-md transition-colors"
                    :title="notif.type === 'Broadcast' ? 'Recall Message' : 'Delete Record'"
                  >
                    <span class="material-symbols-outlined text-[16px]">{{
                      notif.type === 'Broadcast' ? 'undo' : 'delete'
                    }}</span>
                    {{ notif.type === 'Broadcast' ? 'Recall' : 'Delete' }}
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="notifications.length === 0 && !isLoading">
              <td colspan="6" class="p-8 text-center text-slate-500 dark:text-slate-400 text-sm">
                No notification history found.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div
        class="p-4 border-t border-stone-200 dark:border-stone-800 bg-stone-50/50 dark:bg-stone-900/30 flex flex-col sm:flex-row items-center justify-between gap-4"
      >
        <div class="flex items-center gap-4">
          <span class="text-sm text-slate-500 dark:text-slate-400">Records per page:</span>
          <select
            v-model="pageSize"
            class="h-9 py-0 pr-8 pl-3 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-sm focus:ring-primary"
          >
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-slate-700 dark:text-slate-300 mr-2"
            >Page {{ currentPage }} of {{ totalPages }}</span
          >
          <div class="flex gap-1">
            <button
              class="w-9 h-9 flex items-center justify-center rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-400 hover:text-primary transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="currentPage <= 1"
              @click="goToPage(currentPage - 1)"
            >
              <span class="material-symbols-outlined text-[18px]">chevron_left</span>
            </button>
            <button
              v-for="p in paginationPages"
              :key="p"
              :class="[
                'w-9 h-9 flex items-center justify-center rounded-lg font-medium text-sm transition-colors',
                currentPage === p
                  ? 'bg-primary text-white shadow-sm shadow-primary/20'
                  : 'border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-600 dark:text-slate-400 hover:bg-stone-50 dark:hover:bg-stone-800',
              ]"
              @click="goToPage(p)"
            >
              {{ p }}
            </button>
            <button
              class="w-9 h-9 flex items-center justify-center rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-400 hover:text-primary transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="currentPage >= totalPages"
              @click="goToPage(currentPage + 1)"
            >
              <span class="material-symbols-outlined text-[18px]">chevron_right</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modals -->

    <!-- Create Notification Modal -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showCreateModal"
          class="fixed inset-0 z-[150] flex items-center justify-center p-4 outline-none"
        >
          <div
            class="absolute inset-0 bg-black/50 backdrop-blur-md"
            @click="showCreateModal = false"
          ></div>
          <div
            class="relative bg-white dark:bg-surface-dark w-full max-w-5xl rounded-2xl shadow-2xl flex flex-col max-h-[90vh] overflow-hidden border border-stone-200 dark:border-stone-800"
          >
            <!-- Modal Header -->
            <div
              class="px-6 py-5 border-b border-stone-100 dark:border-stone-800 flex items-center justify-between shrink-0"
            >
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-xl bg-orange-100 dark:bg-orange-900/20 text-primary">
                  <span class="material-symbols-outlined">add_circle</span>
                </div>
                <div>
                  <h2 class="text-xl font-bold text-slate-900 dark:text-white">
                    Create Notification
                  </h2>
                  <p class="text-sm text-slate-500 dark:text-slate-400 mt-0.5">
                    Send a new message to students or faculty members.
                  </p>
                </div>
              </div>
              <button
                @click="showCreateModal = false"
                class="p-2 rounded-lg hover:bg-stone-100 dark:hover:bg-stone-800 text-slate-400 hover:text-slate-600 transition-colors"
                aria-label="Close modal"
              >
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>

            <div class="flex flex-col md:flex-row h-full overflow-hidden">
              <!-- Form Content -->
              <div
                class="flex-1 flex flex-col border-r border-stone-200 dark:border-stone-800 overflow-y-auto"
              >
                <div class="px-6 pt-6">
                  <div class="flex space-x-1 rounded-xl bg-stone-100 dark:bg-stone-800 p-1.5">
                    <button
                      v-for="t in ['broadcast', 'targeted', 'personal']"
                      :key="t"
                      @click="activeTab = t"
                      :class="[
                        'w-full rounded-lg py-2 text-sm font-semibold transition-all',
                        activeTab === t
                          ? 'bg-white dark:bg-stone-700 text-slate-900 dark:text-white shadow-sm ring-1 ring-black/5'
                          : 'text-slate-500 dark:text-slate-400 hover:text-slate-900 hover:bg-white/50 dark:hover:bg-stone-700/50',
                      ]"
                    >
                      {{
                        t === 'broadcast' ? 'Broadcast' : t === 'targeted' ? 'Targeted' : 'Personal'
                      }}
                    </button>
                  </div>
                </div>

                <div class="p-6 space-y-5">
                  <!-- Audience selection -->
                  <div
                    v-if="activeTab !== 'broadcast'"
                    class="p-4 bg-orange-50/50 dark:bg-orange-900/10 rounded-xl border border-orange-100/50 dark:border-orange-900/20"
                  >
                    <div
                      class="flex items-center gap-2 mb-4 text-primary font-bold text-xs uppercase tracking-wider"
                    >
                      <span class="material-symbols-outlined text-lg">filter_alt</span>
                      {{ activeTab === 'targeted' ? 'Target Audience' : 'Recipient Details' }}
                    </div>

                    <div
                      v-if="activeTab === 'targeted'"
                      class="grid grid-cols-1 md:grid-cols-3 gap-4"
                    >
                      <div>
                        <label class="block text-xs font-bold text-slate-500 mb-1">Role</label>
                        <select v-model="form.role" class="w-full">
                          <option>All Roles</option>
                          <option>STUDENT</option>
                          <option>TEACHER</option>
                        </select>
                      </div>
                      <div>
                        <label class="block text-xs font-bold text-slate-500 mb-1"
                          >Department</label
                        >
                        <select v-model="form.department" class="w-full">
                          <option>All Departments</option>
                          <option>Computer Science</option>
                          <option>English Literature</option>
                        </select>
                      </div>
                      <div>
                        <label class="block text-xs font-bold text-slate-500 mb-1">Class</label>
                        <select v-model="form.class" class="w-full">
                          <option>All Classes</option>
                          <option>CS101-A</option>
                          <option>ENG202-B</option>
                        </select>
                      </div>
                    </div>
                    <div v-else>
                      <label class="block text-xs font-bold text-slate-500 mb-1"
                        >Search User (Email/Code)</label
                      >
                      <div class="relative">
                        <span
                          class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]"
                          >search</span
                        >
                        <input
                          v-model="form.recipientId"
                          class="w-full pl-10"
                          placeholder="Search for recipient..."
                          type="text"
                        />
                      </div>
                    </div>
                  </div>

                  <!-- Title & Body -->
                  <div class="space-y-4">
                    <div>
                      <label class="block text-sm font-bold text-slate-700 dark:text-slate-300 mb-1"
                        >Subject</label
                      >
                      <input
                        v-model="form.title"
                        class="w-full"
                        placeholder="e.g. Important Announcement"
                        type="text"
                      />
                    </div>
                    <div>
                      <label class="block text-sm font-bold text-slate-700 dark:text-slate-300 mb-1"
                        >Message Content</label
                      >
                      <textarea
                        v-model="form.body"
                        class="w-full h-32 resize-none leading-relaxed"
                        placeholder="Explain your notification here..."
                        maxlength="500"
                      ></textarea>
                      <div class="flex justify-between items-center mt-1">
                        <span class="text-[10px] text-slate-400 uppercase tracking-widest font-bold"
                          >Max 500 chars</span
                        >
                        <span
                          :class="[
                            'text-xs font-bold',
                            form.body.length > 450 ? 'text-red-500' : 'text-slate-400',
                          ]"
                          >{{ form.body.length }}/500</span
                        >
                      </div>
                    </div>
                    <div>
                      <label class="block text-sm font-bold text-slate-700 dark:text-slate-300 mb-1"
                        >Action Link (Optional)</label
                      >
                      <div class="relative">
                        <span
                          class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-lg"
                          >link</span
                        >
                        <input
                          v-model="form.actionUrl"
                          class="w-full pl-10"
                          placeholder="https://example.com/details"
                          type="text"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Preview section -->
              <div
                class="hidden md:flex w-[380px] bg-stone-50 dark:bg-stone-900/50 p-8 flex-col items-center flex-shrink-0 border-l border-stone-100 dark:border-stone-800 overflow-y-auto"
              >
                <h3
                  class="text-xs font-bold text-slate-400 dark:text-slate-500 uppercase tracking-[0.2em] mb-8"
                >
                  Live Mobile Preview
                </h3>

                <div
                  class="relative w-[280px] h-[540px] bg-slate-900 rounded-[3rem] shadow-2xl ring-8 ring-slate-900 border-4 border-slate-800 overflow-hidden shrink-0 flex flex-col"
                >
                  <!-- Phone Status Bar -->
                  <div class="h-6 bg-slate-900 w-full flex items-center justify-between px-6 pt-1">
                    <span class="text-[10px] text-white font-bold">9:41</span>
                    <div class="flex gap-1">
                      <span class="material-symbols-outlined text-[8px] text-white"
                        >signal_cellular_alt</span
                      >
                      <span class="material-symbols-outlined text-[8px] text-white">wifi</span>
                      <span class="material-symbols-outlined text-[8px] text-white"
                        >battery_full</span
                      >
                    </div>
                  </div>

                  <!-- Phone Display -->
                  <div
                    class="flex-1 bg-background-light dark:bg-stone-950 flex flex-col relative overflow-hidden"
                  >
                    <!-- Nav Dummy -->
                    <div class="bg-primary px-4 py-3 pb-8 text-white shadow-md">
                      <div class="flex justify-between items-center mb-2">
                        <span class="material-symbols-outlined text-lg">menu</span>
                        <span class="font-bold text-xs uppercase tracking-wider"
                          >Notifications</span
                        >
                        <span class="material-symbols-outlined text-lg">account_circle</span>
                      </div>
                    </div>

                    <!-- Notification UI -->
                    <div class="px-3 -mt-4 relative z-10 space-y-3">
                      <Transition name="fade" mode="out-in">
                        <div
                          :key="activeTab + form.title"
                          class="bg-white dark:bg-surface-dark rounded-2xl shadow-xl p-4 border-l-4 border-primary transition-all duration-300"
                        >
                          <div class="flex justify-between items-start mb-2">
                            <span
                              class="text-[9px] font-bold text-primary bg-primary/10 px-2 py-0.5 rounded-full uppercase"
                              >{{ activeTab }}</span
                            >
                            <span class="text-[9px] font-medium text-slate-400 italic"
                              >Just now</span
                            >
                          </div>
                          <h4
                            class="font-bold text-slate-900 dark:text-white text-xs mb-1 truncate"
                          >
                            {{ form.title || 'Notification Subject' }}
                          </h4>
                          <p
                            class="text-[10px] text-slate-600 dark:text-slate-400 leading-normal line-clamp-3"
                          >
                            {{ form.body || 'Your message will appear here exactly as sent...' }}
                          </p>
                        </div>
                      </Transition>

                      <!-- Dummy cards -->
                      <div
                        v-for="i in 2"
                        :key="i"
                        class="bg-white/60 dark:bg-stone-900 rounded-xl p-3 opacity-30 blur-[0.5px]"
                      >
                        <div class="h-2 w-12 bg-stone-200 dark:bg-stone-700 rounded mb-2"></div>
                        <div class="h-2 w-3/4 bg-stone-200 dark:bg-stone-700 rounded mb-1"></div>
                        <div class="h-2 w-1/2 bg-stone-200 dark:bg-stone-700 rounded"></div>
                      </div>
                    </div>

                    <!-- Bottom Nav Dummy -->
                    <div
                      class="absolute bottom-0 w-full bg-white dark:bg-surface-dark border-t border-stone-200 dark:border-stone-800 h-12 flex items-center justify-around px-2"
                    >
                      <div class="flex flex-col items-center gap-0.5 text-slate-300">
                        <span class="material-symbols-outlined text-lg">home</span>
                      </div>
                      <div class="flex flex-col items-center gap-0.5 text-primary">
                        <span class="material-symbols-outlined text-lg">notifications</span>
                      </div>
                      <div class="flex flex-col items-center gap-0.5 text-slate-300">
                        <span class="material-symbols-outlined text-lg">person</span>
                      </div>
                    </div>
                  </div>

                  <!-- Notch -->
                  <div
                    class="absolute top-0 left-1/2 -translate-x-1/2 w-24 h-5 bg-slate-900 rounded-b-2xl"
                  ></div>
                </div>
              </div>
            </div>

            <!-- Footer -->
            <div
              class="px-6 py-5 border-t border-stone-100 dark:border-stone-800 flex justify-end gap-3 bg-stone-50/30 dark:bg-stone-900/10 shrink-0"
            >
              <button
                @click="showCreateModal = false"
                class="px-6 py-2.5 rounded-lg border border-stone-200 dark:border-stone-700 text-slate-600 dark:text-slate-300 font-bold hover:bg-stone-100 dark:hover:bg-stone-800 transition-all text-sm"
              >
                Cancel
              </button>
              <button
                @click="handleSend"
                :disabled="isSending"
                class="flex items-center gap-2 px-8 py-2.5 bg-primary hover:bg-primary-dark disabled:bg-slate-400 text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95 text-sm"
              >
                <span
                  v-if="isSending"
                  class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"
                ></span>
                <span v-else class="material-symbols-outlined text-[20px]">send</span>
                {{ isSending ? 'Sending...' : 'Send Notification' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Delete/Recall Confirm Modal -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showDeleteConfirmModal"
          class="fixed inset-0 z-[150] flex items-center justify-center p-4 outline-none"
        >
          <div
            class="absolute inset-0 bg-black/50 backdrop-blur-md"
            @click="showDeleteConfirmModal = false"
          ></div>
          <div
            class="relative bg-white dark:bg-surface-dark w-full max-w-md rounded-2xl shadow-2xl p-6 border border-stone-200 dark:border-stone-800"
          >
            <div class="flex flex-col items-center text-center gap-4">
              <div
                class="w-16 h-16 rounded-full bg-red-100 dark:bg-red-900/20 text-red-600 flex items-center justify-center"
              >
                <span class="material-symbols-outlined text-4xl">warning</span>
              </div>
              <div>
                <h3 class="text-xl font-bold text-slate-900 dark:text-white">Are you sure?</h3>
                <p class="text-slate-500 dark:text-slate-400 mt-2 text-sm">
                  This action will hide this notification from user history. Broadcasts may no
                  longer be retrievable.
                </p>
              </div>
            </div>
            <div class="flex gap-3 mt-8">
              <button
                @click="showDeleteConfirmModal = false"
                class="flex-1 px-4 py-2.5 rounded-lg border border-stone-200 dark:border-stone-700 text-slate-600 dark:text-slate-300 font-bold hover:bg-stone-100 dark:hover:bg-stone-800 transition-all"
              >
                No, Keep it
              </button>
              <button
                @click="confirmDelete"
                :disabled="isDeleting"
                class="flex-1 px-4 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-lg font-bold flex items-center justify-center gap-2 transition-all active:scale-95"
              >
                <span
                  v-if="isDeleting"
                  class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"
                ></span>
                {{ isDeleting ? 'Processing...' : 'Yes, Delete/Recall' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
@reference "../../assets/base.css";

.toast-enter-active,
.toast-leave-active {
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-20px) scale(0.9);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

input,
select,
textarea {
  @apply rounded-xl border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-sm focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all duration-200;
}
</style>
