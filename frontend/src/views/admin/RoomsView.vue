<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import {
  adminRoomService,
  type AdminRoomResponse,
  type AdminCreateRoomRequest,
  type AdminUpdateRoomRequest
} from '@/services/adminRoomService'
import { useToast } from '@/composables/useToast'

const { toast, showToast } = useToast()

const loading = ref(false)
const rooms = ref<AdminRoomResponse[]>([])
const roomTypes = ['LECTURE_HALL', 'COMPUTER_LAB', 'MEETING_ROOM', 'STUDY_ROOM']

const searchQuery = ref('')
const filterRoomType = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)

// Modal state
const showAddRoomModal = ref(false)
const showEditRoomModal = ref(false)
const showDeleteConfirmModal = ref(false)
const createLoading = ref(false)
const deleteLoading = ref(false)
const createError = ref<string | null>(null)

const newRoom = ref<AdminCreateRoomRequest>({
  roomName: '',
  capacity: 40,
  roomType: 'LECTURE_HALL',
})

const editingRoom = ref<AdminRoomResponse | null>(null)
const deletingRoom = ref<AdminRoomResponse | null>(null)

async function fetchRooms() {
  try {
    loading.value = true
    const response = await adminRoomService.getRooms(
      currentPage.value,
      pageSize.value,
      searchQuery.value || undefined,
      filterRoomType.value || undefined
    )
    rooms.value = response.content
    totalElements.value = response.totalElements
    totalPages.value = response.totalPages
  } catch (error) {
    console.error('Failed to fetch rooms:', error)
    showToast('Failed to load rooms. Please try again.', 'error')
  } finally {
    loading.value = false
  }
}

// Search with debounce
let searchTimeout: ReturnType<typeof setTimeout>
function handleSearch() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    currentPage.value = 1
    fetchRooms()
  }, 300)
}

watch(filterRoomType, () => {
  currentPage.value = 1
  fetchRooms()
})

function clearFilters() {
  searchQuery.value = ''
  filterRoomType.value = ''
  currentPage.value = 1
  fetchRooms()
}

function handlePageChange(page: number) {
  if (page >= 1 && (totalPages.value === 0 || page <= totalPages.value) && page !== currentPage.value) {
    currentPage.value = page
    fetchRooms()
  }
}

const paginationPages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const pages: number[] = []
  const start = Math.max(1, current - 1)
  const end = Math.min(total, current + 1)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

watch(pageSize, () => {
  currentPage.value = 1
  fetchRooms()
})

async function handleCreateRoom() {
  try {
    createLoading.value = true
    createError.value = null
    await adminRoomService.createRoom(newRoom.value)
    
    showToast('Room created successfully', 'success')
    showAddRoomModal.value = false
    
    // Reset form
    newRoom.value = {
      roomName: '',
      capacity: 40,
      roomType: 'LECTURE_HALL',
    }
    
    // Refresh list
    fetchRooms()
  } catch (err) {
    const error = err as Error
    createError.value = error.message || 'Failed to create room'
  } finally {
    createLoading.value = false
  }
}

function openAddRoomModal() {
  newRoom.value = {
    roomName: '',
    capacity: 40,
    roomType: 'LECTURE_HALL',
  }
  createError.value = null
  showAddRoomModal.value = true
}

function openEditModal(room: AdminRoomResponse) {
  editingRoom.value = { ...room }
  showEditRoomModal.value = true
  createError.value = null
}

async function handleUpdateRoom() {
  if (!editingRoom.value) return
  
  try {
    createLoading.value = true
    createError.value = null
    const updateData: AdminUpdateRoomRequest = {
      roomName: editingRoom.value.roomName,
      capacity: editingRoom.value.capacity || 40,
      roomType: editingRoom.value.roomType || 'LECTURE_HALL',
    }
    
    await adminRoomService.updateRoom(editingRoom.value.roomId, updateData)
    
    showToast('Room updated successfully', 'success')
    showEditRoomModal.value = false
    fetchRooms()
  } catch (err) {
    const error = err as Error
    createError.value = error.message || 'Failed to update room'
  } finally {
    createLoading.value = false
  }
}

function openDeleteModal(room: AdminRoomResponse) {
  deletingRoom.value = room
  showDeleteConfirmModal.value = true
}

async function handleDeleteRoom() {
  if (!deletingRoom.value) return
  
  try {
    deleteLoading.value = true
    await adminRoomService.deleteRoom(deletingRoom.value.roomId)
    showToast('Room deleted successfully', 'success')
    showDeleteConfirmModal.value = false
    fetchRooms()
  } catch (err) {
    console.error('Failed to delete room:', err)
    showToast('Failed to delete room', 'error')
  } finally {
    deleteLoading.value = false
  }
}

onMounted(() => {
  fetchRooms()
})
</script>

<template>
  <div class="max-w-[1400px] w-full mx-auto p-6 md:p-10 flex flex-col gap-8">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-slate-900 dark:text-white text-3xl font-bold leading-tight tracking-tight">
          Room Management
        </h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">
          Manage university classrooms and their capacities.
        </p>
      </div>
      <div class="flex items-center gap-3">
        <button
          @click="openAddRoomModal"
          class="flex items-center gap-2 px-6 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95"
        >
          <span class="material-symbols-outlined text-[20px]">add_circle</span>
          Add New Room
        </button>
      </div>
    </div>
    
    <Teleport to="body">
      <div v-if="toast" class="fixed top-4 right-4 z-[100] animate-fade-in">
        <div 
          :class="[ 
            'flex items-center gap-3 px-4 py-3 rounded-lg shadow-lg border min-w-[300px]',
            toast.type === 'success' ? 'bg-green-50 border-green-200 text-green-800 dark:bg-green-900/30 dark:border-green-800 dark:text-green-300' : 
            'bg-red-50 border-red-200 text-red-800 dark:bg-red-900/30 dark:border-red-800 dark:text-red-300'
          ]"
        >
          <span class="material-symbols-outlined shrink-0">
            {{ toast.type === 'success' ? 'check_circle' : 'error' }}
          </span>
          <p class="text-sm font-medium">{{ toast.message }}</p>
        </div>
      </div>
    </Teleport>

    <!-- Filters Bar -->
    <div
      class="bg-surface-light dark:bg-surface-dark p-4 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex flex-col md:flex-row gap-4"
    >
      <div class="relative flex-1">
        <span
          class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
          >search</span
        >
        <input
          v-model="searchQuery"
          @input="handleSearch"
          class="w-full pl-10 pr-4 h-11 bg-white dark:bg-stone-900 border border-stone-200 dark:border-stone-700 text-sm rounded-lg focus:ring-primary focus:border-primary transition-all text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
          placeholder="Search rooms by name..."
          type="text"
        />
      </div>
      <div class="flex flex-col sm:flex-row gap-4 w-full md:w-auto">
        <select
          v-model="filterRoomType"
          class="h-11 w-full sm:w-48 bg-white dark:bg-stone-900 border border-stone-200 dark:border-stone-700 text-sm rounded-lg focus:ring-primary focus:border-primary transition-all text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
        >
          <option value="">All Room Types</option>
          <option v-for="type in roomTypes" :key="type" :value="type">{{ type.replace('_', ' ') }}</option>
        </select>
        
        <button
          v-if="searchQuery || filterRoomType !== ''"
          @click="clearFilters"
          class="flex items-center justify-center w-full sm:w-auto gap-2 h-11 px-4 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-slate-700 dark:text-slate-300 rounded-lg font-semibold transition-all whitespace-nowrap"
        >
          <span class="material-symbols-outlined text-[20px]">filter_list_off</span>
          Clear
        </button>
      </div>
    </div>

    <!-- Table -->
    <div class="bg-surface-light dark:bg-surface-dark border border-stone-200 dark:border-stone-800 rounded-xl overflow-hidden shadow-sm">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm whitespace-nowrap">
          <thead class="bg-stone-50 dark:bg-stone-800/50 text-slate-500 dark:text-slate-400 font-medium border-b border-stone-200 dark:border-stone-800">
            <tr>
              <th class="px-6 py-4">Room Name</th>
              <th class="px-6 py-4">Type</th>
              <th class="px-6 py-4 text-center">Capacity</th>
              <th class="px-6 py-4">Assigned Teacher</th>
              <th class="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-200 dark:divide-stone-800">
            <tr v-if="loading">
              <td colspan="5" class="px-6 py-8 text-center text-slate-500 dark:text-slate-400">
                <div class="flex items-center justify-center gap-2">
                  <span class="material-symbols-outlined animate-spin">progress_activity</span>
                  Loading rooms...
                </div>
              </td>
            </tr>
            <tr v-else-if="rooms.length === 0">
              <td colspan="5" class="px-6 py-8 text-center text-slate-500 dark:text-slate-400">
                No rooms found. Try modifying your search.
              </td>
            </tr>
            <tr
              v-else
              v-for="room in rooms"
              :key="room.roomId"
              class="hover:bg-stone-50/50 dark:hover:bg-stone-800/30 transition-colors"
            >
              <td class="px-6 py-4 font-medium text-slate-900 dark:text-white">
                {{ room.roomName }}
              </td>
              <td class="px-6 py-4">
                <span :class="[
                  'inline-flex items-center px-2 py-1 rounded-md text-xs font-semibold',
                  room.roomType === 'LECTURE_HALL' ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400' :
                  room.roomType === 'COMPUTER_LAB' ? 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400' :
                  'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400'
                ]">
                  {{ room.roomType?.replace('_', ' ') }}
                </span>
              </td>
              <td class="px-6 py-4 text-center text-slate-600 dark:text-slate-400">
                {{ room.capacity }}
              </td>
              <td class="px-6 py-4">
                <div v-if="room.assignedTeacherName" class="flex items-center gap-2">
                  <span class="w-7 h-7 flex items-center justify-center rounded-full bg-primary/10 text-primary">
                    <span class="material-symbols-outlined text-[16px]">person</span>
                  </span>
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-300">{{ room.assignedTeacherName }}</span>
                </div>
                <span v-else class="inline-flex items-center px-2 py-1 rounded-md text-xs font-semibold bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">
                  Available
                </span>
              </td>
              <td class="px-6 py-4 text-right">
                <div class="flex items-center justify-end gap-2">
                  <button
                    @click="openEditModal(room)"
                    class="p-2 text-slate-400 hover:text-primary hover:bg-primary/10 rounded-lg transition-colors"
                    title="Edit Room"
                  >
                    <span class="material-symbols-outlined text-[20px]">edit</span>
                  </button>
                  <button
                    @click="openDeleteModal(room)"
                    :disabled="!!room.assignedTeacherName"
                    :class="[
                      'p-2 rounded-lg transition-colors',
                      room.assignedTeacherName
                        ? 'text-slate-300 dark:text-slate-600 cursor-not-allowed'
                        : 'text-slate-400 hover:text-red-500 hover:bg-red-500/10'
                    ]"
                    :title="room.assignedTeacherName ? `Cannot delete: assigned to ${room.assignedTeacherName}` : 'Delete Room'"
                  >
                    <span class="material-symbols-outlined text-[20px]">{{ room.assignedTeacherName ? 'lock' : 'delete' }}</span>
                  </button>
                </div>
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
            class="h-9 py-0 pr-8 pl-3 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-sm"
          >
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-slate-700 dark:text-slate-300 mr-2"
            >Page {{ currentPage }} of {{ totalPages || 1 }}</span
          >
          <div class="flex gap-1">
            <button
              class="w-9 h-9 flex items-center justify-center rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-400 hover:text-primary transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="currentPage <= 1"
              aria-label="Previous page"
              @click="handlePageChange(currentPage - 1)"
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
              @click="handlePageChange(p)"
            >
              {{ p }}
            </button>
            <button
              class="w-9 h-9 flex items-center justify-center rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-400 hover:text-primary transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="currentPage >= totalPages || totalPages === 0"
              aria-label="Next page"
              @click="handlePageChange(currentPage + 1)"
            >
              <span class="material-symbols-outlined text-[18px]">chevron_right</span>
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Add Room Modal -->
    <Teleport to="body">
      <div v-if="showAddRoomModal" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-slate-900/50 backdrop-blur-sm" @click="showAddRoomModal = false"></div>
        <div class="relative w-full max-w-md bg-surface-light dark:bg-surface-dark rounded-xl shadow-2xl border border-stone-200 dark:border-stone-800 overflow-hidden flex flex-col max-h-[90vh]">
          <div class="flex items-center justify-between px-6 py-4 border-b border-stone-200 dark:border-stone-800">
            <h3 class="text-lg font-bold text-slate-900 dark:text-white">Add New Room</h3>
            <button @click="showAddRoomModal = false" class="text-slate-400 hover:text-slate-600 dark:hover:text-slate-300">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div class="overflow-y-auto p-6 space-y-4">
            <div v-if="createError" class="p-3 bg-red-50 text-red-600 dark:bg-red-900/30 dark:text-red-400 text-sm rounded-lg border border-red-200 dark:border-red-800/30">
              {{ createError }}
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Room Name <span class="text-red-500">*</span></label>
              <input
                v-model="newRoom.roomName"
                type="text"
                placeholder="e.g. A101"
                class="w-full px-3 py-2 border border-stone-200 dark:border-stone-800 bg-transparent rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
              />
            </div>
            
            <div>
              <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Capacity <span class="text-red-500">*</span></label>
              <input
                v-model="newRoom.capacity"
                type="number"
                min="10"
                class="w-full px-3 py-2 border border-stone-200 dark:border-stone-800 bg-transparent rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Room Type <span class="text-red-500">*</span></label>
              <select
                v-model="newRoom.roomType"
                class="w-full px-3 py-2 border border-stone-200 dark:border-stone-800 bg-transparent rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
              >
                <option v-for="type in roomTypes" :key="type" :value="type">{{ type }}</option>
              </select>
            </div>
          </div>

          <div class="px-6 py-4 border-t border-stone-200 dark:border-stone-800 bg-stone-50 dark:bg-stone-800/30 flex justify-end gap-3 rounded-b-xl">
            <button
              @click="showAddRoomModal = false"
              class="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 hover:bg-stone-200 dark:hover:bg-stone-700 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button
              @click="handleCreateRoom"
              :disabled="createLoading || !newRoom.roomName || !newRoom.capacity"
              class="px-4 py-2 text-sm font-medium text-white bg-primary hover:bg-primary-dark rounded-lg transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span v-if="createLoading" class="material-symbols-outlined animate-spin text-[18px]">progress_activity</span>
              Save Room
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Edit Room Modal -->
    <Teleport to="body">
      <div v-if="showEditRoomModal && editingRoom" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-slate-900/50 backdrop-blur-sm" @click="showEditRoomModal = false"></div>
        <div class="relative w-full max-w-md bg-surface-light dark:bg-surface-dark rounded-xl shadow-2xl border border-stone-200 dark:border-stone-800 overflow-hidden flex flex-col max-h-[90vh]">
          <div class="flex items-center justify-between px-6 py-4 border-b border-stone-200 dark:border-stone-800">
            <h3 class="text-lg font-bold text-slate-900 dark:text-white">Edit Room</h3>
            <button @click="showEditRoomModal = false" class="text-slate-400 hover:text-slate-600 dark:hover:text-slate-300">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div class="overflow-y-auto p-6 space-y-4">
            <div v-if="createError" class="p-3 bg-red-50 text-red-600 dark:bg-red-900/30 dark:text-red-400 text-sm rounded-lg border border-red-200 dark:border-red-800/30">
              {{ createError }}
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Room Name <span class="text-red-500">*</span></label>
              <input
                v-model="editingRoom.roomName"
                type="text"
                class="w-full px-3 py-2 border border-stone-200 dark:border-stone-800 bg-transparent rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
              />
            </div>
            
            <div>
              <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Capacity <span class="text-red-500">*</span></label>
              <input
                v-model="editingRoom.capacity"
                type="number"
                min="10"
                class="w-full px-3 py-2 border border-stone-200 dark:border-stone-800 bg-transparent rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Room Type <span class="text-red-500">*</span></label>
              <select
                v-model="editingRoom.roomType"
                class="w-full px-3 py-2 border border-stone-200 dark:border-stone-800 bg-transparent rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
              >
                <option v-for="type in roomTypes" :key="type" :value="type">{{ type }}</option>
              </select>
            </div>
          </div>

          <div class="px-6 py-4 border-t border-stone-200 dark:border-stone-800 bg-stone-50 dark:bg-stone-800/30 flex justify-end gap-3 rounded-b-xl">
            <button
              @click="showEditRoomModal = false"
              class="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 hover:bg-stone-200 dark:hover:bg-stone-700 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button
              @click="handleUpdateRoom"
              :disabled="createLoading || !editingRoom.roomName || !editingRoom.capacity"
              class="px-4 py-2 text-sm font-medium text-white bg-primary hover:bg-primary-dark rounded-lg transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span v-if="createLoading" class="material-symbols-outlined animate-spin text-[18px]">progress_activity</span>
              Save Changes
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Delete Confirm Modal -->
    <Teleport to="body">
      <div v-if="showDeleteConfirmModal && deletingRoom" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-slate-900/50 backdrop-blur-sm" @click="showDeleteConfirmModal = false"></div>
        <div class="relative w-full max-w-md bg-surface-light dark:bg-surface-dark rounded-xl shadow-2xl border border-stone-200 dark:border-stone-800 overflow-hidden">
          <div class="p-6 text-center">
            <div class="w-16 h-16 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center mx-auto mb-4">
              <span class="material-symbols-outlined text-3xl text-red-600 dark:text-red-400">warning</span>
            </div>
            <h3 class="text-xl font-bold text-slate-900 dark:text-white mb-2">Delete Room</h3>
            <p class="text-sm text-slate-500 dark:text-slate-400">
              Are you sure you want to delete room <span class="font-bold text-slate-900 dark:text-white">{{ deletingRoom.roomName }}</span>? This action cannot be undone.
            </p>
          </div>
          
          <div class="px-6 py-4 border-t border-stone-200 dark:border-stone-800 bg-stone-50 dark:bg-stone-800/30 flex justify-end gap-3 mt-2">
            <button
              @click="showDeleteConfirmModal = false"
              class="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 hover:bg-stone-200 dark:hover:bg-stone-700 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button
              @click="handleDeleteRoom"
              :disabled="deleteLoading"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg transition-colors flex items-center gap-2 disabled:opacity-50"
            >
              <span v-if="deleteLoading" class="material-symbols-outlined animate-spin text-[18px]">progress_activity</span>
              Delete
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
