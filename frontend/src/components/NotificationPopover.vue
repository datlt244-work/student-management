<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getMyNotifications,
  getUnreadCount,
  markAsRead,
  markAllAsRead,
  type UserNotification,
} from '@/services/portalNotificationService'

const props = defineProps<{
  portal: string
}>()

const router = useRouter()
const isOpen = ref(false)
const notifications = ref<UserNotification[]>([])
const unreadCount = ref(0)
const loading = ref(false)
const popoverRef = ref<HTMLElement | null>(null)

const showDetailModal = ref(false)
const selectedNotification = ref<UserNotification | null>(null)

async function fetchSummary() {
  try {
    unreadCount.value = await getUnreadCount()
  } catch (err) {
    console.error('Failed to fetch unread count', err)
  }
}

async function togglePopover() {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    loading.value = true
    try {
      const data = await getMyNotifications(0, 10)
      notifications.value = data.content
    } catch (err) {
      console.error('Failed to fetch notifications', err)
    } finally {
      loading.value = false
    }
  }
}

async function handleMarkAsRead(notif: UserNotification) {
  selectedNotification.value = notif
  showDetailModal.value = true
  isOpen.value = false

  if (notif.isRead) return

  try {
    await markAsRead(notif.notificationId)
    notif.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (err) {
    console.error('Failed to mark as read', err)
  }
}

async function markSingleAsRead(event: Event, notif: UserNotification) {
  event.stopPropagation()
  if (notif.isRead) return

  try {
    await markAsRead(notif.notificationId)
    notif.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (err) {
    console.error('Failed to mark as read', err)
  }
}

async function handleMarkAllRead() {
  try {
    await markAllAsRead()
    notifications.value.forEach((n) => (n.isRead = true))
    unreadCount.value = 0
  } catch (err) {
    console.error('Failed to mark all as read', err)
  }
}

function viewAll() {
  isOpen.value = false
  router.push({ name: `${props.portal}-notifications` })
}

function handleClickOutside(event: MouseEvent) {
  if (popoverRef.value && !popoverRef.value.contains(event.target as Node)) {
    isOpen.value = false
  }
}

function formatDate(dateStr: string) {
  const date = new Date(dateStr)
  const now = new Date()
  const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / 60000)

  if (diffInMinutes < 1) return 'Just now'
  if (diffInMinutes < 60) return `${diffInMinutes}m ago`

  const diffInHours = Math.floor(diffInMinutes / 60)
  if (diffInHours < 24) return `${diffInHours}h ago`

  return date.toLocaleDateString()
}

onMounted(() => {
  fetchSummary()
  // Refresh count every 30 seconds
  const interval = setInterval(fetchSummary, 30000)
  document.addEventListener('mousedown', handleClickOutside)

  onUnmounted(() => {
    clearInterval(interval)
    document.removeEventListener('mousedown', handleClickOutside)
  })
})
</script>

<template>
  <div class="relative" ref="popoverRef">
    <button
      class="flex items-center justify-center rounded-lg size-10 bg-background-light dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 transition-colors relative"
      @click="togglePopover"
    >
      <span class="material-symbols-outlined text-[24px]">notifications</span>
      <span
        v-if="unreadCount > 0"
        class="absolute top-2 right-2 size-4 bg-red-500 text-white text-[10px] font-bold rounded-full border border-surface-light dark:border-surface-dark flex items-center justify-center"
      >
        {{ unreadCount > 9 ? '9+' : unreadCount }}
      </span>
    </button>

    <!-- Dropdown -->
    <Transition name="fade">
      <div
        v-if="isOpen"
        class="absolute right-0 mt-2 w-80 sm:w-96 bg-surface-light dark:bg-surface-dark rounded-xl shadow-2xl border border-border-light dark:border-border-dark z-50 overflow-hidden"
      >
        <div
          class="px-4 py-3 border-b border-border-light dark:border-border-dark flex items-center justify-between"
        >
          <h3 class="font-bold text-sm">Notifications</h3>
          <button
            @click="handleMarkAllRead"
            :disabled="unreadCount === 0"
            class="text-xs text-primary hover:underline font-medium disabled:text-text-muted-light dark:disabled:text-text-muted-dark disabled:no-underline"
          >
            Mark all read
          </button>
        </div>

        <div class="max-h-[400px] overflow-y-auto">
          <div v-if="loading" class="p-8 flex justify-center">
            <div
              class="animate-spin size-6 border-2 border-primary border-t-transparent rounded-full"
            ></div>
          </div>

          <div v-else-if="notifications.length === 0" class="p-8 text-center">
            <span
              class="material-symbols-outlined text-4xl text-text-muted-light dark:text-text-muted-dark mb-2"
            >
              notifications_off
            </span>
            <p class="text-text-muted-light dark:text-text-muted-dark text-sm">
              No notifications yet
            </p>
          </div>

          <div v-else class="divide-y divide-border-light dark:divide-border-dark">
            <div
              v-for="notif in notifications"
              :key="notif.notificationId"
              @click="handleMarkAsRead(notif)"
              class="px-4 py-4 hover:bg-stone-50 dark:hover:bg-stone-800/50 cursor-pointer transition-all relative flex gap-3 border-l-4"
              :class="[
                !notif.isRead
                  ? 'bg-primary/5 border-l-primary'
                  : 'bg-transparent border-l-transparent opacity-80',
              ]"
            >
              <!-- Unread Dot Indicator -->
              <div class="mt-1.5 shrink-0">
                <div
                  v-if="!notif.isRead"
                  class="size-2.5 bg-primary rounded-full shadow-[0_0_8px_rgba(var(--color-primary-rgb),0.5)]"
                ></div>
                <div v-else class="size-2.5"></div>
              </div>

              <div class="flex-1 min-w-0">
                <p class="text-sm font-bold break-words">{{ notif.title }}</p>
                <p
                  class="text-xs text-text-muted-light dark:text-text-muted-dark line-clamp-3 mt-0.5 break-words"
                >
                  {{ notif.body }}
                </p>
                <p
                  class="text-[10px] text-text-muted-light dark:text-text-muted-dark mt-1 flex items-center gap-1"
                >
                  <span class="material-symbols-outlined text-[12px]">schedule</span>
                  {{ formatDate(notif.createdAt) }}
                </p>
              </div>

              <!-- Inline Mark as Read -->
              <div v-if="!notif.isRead" class="self-center">
                <button
                  @click="markSingleAsRead($event, notif)"
                  class="size-8 flex items-center justify-center rounded-full hover:bg-primary/20 text-primary transition-colors"
                  title="Mark as read"
                >
                  <span class="material-symbols-outlined text-[20px]">check_circle</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div
          v-if="notifications.length > 0"
          class="px-4 py-2 bg-stone-50 dark:bg-stone-900/50 border-t border-border-light dark:border-border-dark text-center"
        >
          <button
            @click="viewAll"
            class="text-xs font-bold text-primary hover:text-primary-dark transition-colors"
          >
            View all notifications
          </button>
        </div>
      </div>
    </Transition>

    <!-- Detail Modal -->
    <Teleport to="body">
      <Transition name="fade-modal">
        <div
          v-if="showDetailModal"
          class="fixed inset-0 z-[100] flex items-center justify-center p-4"
        >
          <div
            class="absolute inset-0 bg-black/50 backdrop-blur-sm"
            @click="showDetailModal = false"
          ></div>
          <div
            class="relative bg-surface-light dark:bg-surface-dark w-full max-w-lg rounded-2xl shadow-2xl border border-border-light dark:border-border-dark overflow-hidden flex flex-col"
          >
            <div
              class="px-6 py-4 border-b border-border-light dark:border-border-dark flex items-center justify-between bg-stone-50/50 dark:bg-stone-900/30"
            >
              <h3 class="font-bold text-sm">Notification Details</h3>
              <button
                @click="showDetailModal = false"
                class="size-8 flex items-center justify-center rounded-lg hover:bg-stone-200 dark:hover:bg-stone-800 transition-colors"
              >
                <span class="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>

            <div class="p-6 space-y-4 max-h-[70vh] overflow-y-auto">
              <div>
                <h4 class="text-lg font-bold text-primary break-words">
                  {{ selectedNotification?.title }}
                </h4>
                <div
                  class="flex items-center gap-2 mt-1 text-[10px] text-text-muted-light dark:text-text-muted-dark font-medium"
                >
                  <span class="material-symbols-outlined text-[14px]">schedule</span>
                  {{
                    selectedNotification
                      ? new Date(selectedNotification.createdAt).toLocaleString()
                      : ''
                  }}
                </div>
              </div>

              <div
                class="bg-background-light dark:bg-stone-900/50 p-4 rounded-xl border border-border-light dark:border-border-dark text-sm leading-relaxed break-words whitespace-pre-wrap text-text-main-light dark:text-text-main-dark"
              >
                {{ selectedNotification?.body }}
              </div>

              <div v-if="selectedNotification?.actionUrl" class="pt-2">
                <a
                  :href="selectedNotification.actionUrl"
                  target="_blank"
                  class="inline-flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg text-sm font-bold shadow-lg shadow-primary/20 hover:bg-primary-dark transition-all active:scale-95"
                >
                  <span class="material-symbols-outlined text-[18px]">open_in_new</span>
                  View Related Content
                </a>
              </div>
            </div>

            <div
              class="px-6 py-4 bg-stone-50/50 dark:bg-stone-900/30 border-t border-border-light dark:border-border-dark flex justify-end"
            >
              <button
                @click="showDetailModal = false"
                class="px-6 py-2 rounded-lg border border-border-light dark:border-border-dark font-bold text-sm hover:bg-stone-100 dark:hover:bg-stone-800 transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.fade-modal-enter-active,
.fade-modal-leave-active {
  transition: opacity 0.3s ease;
}
.fade-modal-enter-from,
.fade-modal-leave-to {
  opacity: 0;
}
</style>
