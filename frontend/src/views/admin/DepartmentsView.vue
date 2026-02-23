<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { watchDebounced } from '@vueuse/core'
import {
  getAdminDepartmentList,
  createAdminDepartment,
  updateAdminDepartment,
  deleteAdminDepartment,
  type AdminDepartmentListItem,
  type AdminCreateDepartmentRequest,
  type AdminUpdateDepartmentRequest,
  updateAdminDepartmentStatus,
} from '@/services/adminUserService'
import { useToast } from '@/composables/useToast'

const { toast, showToast } = useToast()

// Filters & pagination
const searchQuery = ref('')
const statusFilter = ref('')
const sortBy = ref<'createdAt,desc' | 'createdAt,asc' | ''>('createdAt,desc')
const pageSize = ref(10)
const currentPage = ref(1) // UI 1-based
const totalPages = ref(0)
const totalElements = ref(0)

// Loading & error state
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)

// Server data
const departments = ref<AdminDepartmentListItem[]>([])
const totalCoursesCount = ref(0)

// Status toggle handler

// Status toggle handler
async function toggleDepartmentStatus(dept: AdminDepartmentListItem) {
  // Prevent spamming if needed, but simple await is fine
  try {
    const newStatus = dept.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    await updateAdminDepartmentStatus(dept.departmentId, newStatus)
    dept.status = newStatus // Update UI
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Failed to update status'
    showToast(msg, 'error')
  }
}

// Stats (computed from data)
const totalDepartments = computed(() => totalElements.value)
const totalCourses = computed(() => totalCoursesCount.value)

// Add Department modal state
const showAddDepartmentModal = ref(false)
const newDepartment = ref({
  name: '',
  officeLocation: '',
})

// Create department submit state
const createDepartmentLoading = ref(false)
const createDepartmentError = ref<string | null>(null)

// Edit Department modal state
const showEditDepartmentModal = ref(false)
const editingDepartment = ref<AdminDepartmentListItem | null>(null)
const editDepartment = ref({
  name: '',
  officeLocation: '',
})

// Update department submit state
const updateDepartmentLoading = ref(false)
const updateDepartmentError = ref<string | null>(null)

// Delete Department modal state
const showDeleteConfirmModal = ref(false)
const deletingDepartment = ref<AdminDepartmentListItem | null>(null)
const deleteDepartmentLoading = ref(false)

function clearFilters() {
  searchQuery.value = ''
  statusFilter.value = ''
  sortBy.value = 'createdAt,desc'
}

async function fetchDepartments() {
  try {
    isLoading.value = true
    errorMessage.value = null

    const result = await getAdminDepartmentList({
      page: currentPage.value - 1, // Convert to 0-based
      size: pageSize.value,
      sort: sortBy.value || 'createdAt,desc',
      search: searchQuery.value || undefined,
      status: statusFilter.value || undefined,
    })

    departments.value = result.content
    currentPage.value = result.page + 1
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
    totalCoursesCount.value = (result as unknown as { totalCourses?: number }).totalCourses ?? 0
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'message' in err) {
      errorMessage.value =
        String((err as { message?: unknown }).message) || 'Failed to load departments'
    } else {
      errorMessage.value = 'Failed to load departments'
    }
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchDepartments()
})

// Debounce tất cả filter/search để tránh gọi API trùng lặp (đặc biệt khi clearFilters)
watchDebounced(
  [searchQuery, sortBy, pageSize, statusFilter],
  () => {
    currentPage.value = 1
    fetchDepartments()
  },
  { debounce: 500, maxWait: 1000 },
)

// Pagination handlers
function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value && page !== currentPage.value) {
    currentPage.value = page
    fetchDepartments()
  }
}

function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
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

// Modal handlers
function handleAddDepartment() {
  showAddDepartmentModal.value = true
  createDepartmentError.value = null
  newDepartment.value = {
    name: '',
    officeLocation: '',
  }
}

function closeAddDepartmentModal() {
  showAddDepartmentModal.value = false
  createDepartmentError.value = null
  newDepartment.value = {
    name: '',
    officeLocation: '',
  }
}

async function submitNewDepartment() {
  createDepartmentError.value = null
  createDepartmentLoading.value = true

  try {
    // Validation
    if (!newDepartment.value.name?.trim()) {
      createDepartmentError.value = 'Department name is required'
      createDepartmentLoading.value = false
      return
    }

    const payload: AdminCreateDepartmentRequest = {
      name: newDepartment.value.name.trim(),
      officeLocation: newDepartment.value.officeLocation?.trim() || undefined,
    }

    await createAdminDepartment(payload)
    closeAddDepartmentModal()
    await fetchDepartments()
    showToast('Department has been created successfully.')
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Failed to create department'
    createDepartmentError.value = msg
  } finally {
    createDepartmentLoading.value = false
  }
}

// Edit modal handlers
function handleEditDepartment(dept: AdminDepartmentListItem) {
  editingDepartment.value = dept
  editDepartment.value = {
    name: dept.name,
    officeLocation: dept.officeLocation || '',
  }
  updateDepartmentError.value = null
  showEditDepartmentModal.value = true
}

function closeEditDepartmentModal() {
  showEditDepartmentModal.value = false
  editingDepartment.value = null
  updateDepartmentError.value = null
  editDepartment.value = {
    name: '',
    officeLocation: '',
  }
}

async function submitUpdateDepartment() {
  if (!editingDepartment.value) return

  updateDepartmentError.value = null
  updateDepartmentLoading.value = true

  try {
    // Validation
    if (!editDepartment.value.name?.trim()) {
      updateDepartmentError.value = 'Department name is required'
      updateDepartmentLoading.value = false
      return
    }

    const payload: AdminUpdateDepartmentRequest = {
      name: editDepartment.value.name.trim(),
      officeLocation: editDepartment.value.officeLocation?.trim() || undefined,
    }

    await updateAdminDepartment(editingDepartment.value.departmentId, payload)
    closeEditDepartmentModal()
    await fetchDepartments()
    showToast('Department has been updated successfully.')
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Failed to update department'
    updateDepartmentError.value = msg
  } finally {
    updateDepartmentLoading.value = false
  }
}

// Delete modal handlers
function handleDeleteDepartment(dept: AdminDepartmentListItem) {
  deletingDepartment.value = dept
  showDeleteConfirmModal.value = true
}

function closeDeleteConfirmModal() {
  showDeleteConfirmModal.value = false
  deletingDepartment.value = null
}

async function confirmDeleteDepartment() {
  if (!deletingDepartment.value) return

  deleteDepartmentLoading.value = true

  try {
    await deleteAdminDepartment(deletingDepartment.value.departmentId)
    closeDeleteConfirmModal()
    await fetchDepartments()
    showToast('Department has been deleted successfully.')
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Failed to delete department'
    showToast(msg, 'error')
    closeDeleteConfirmModal()
  } finally {
    deleteDepartmentLoading.value = false
  }
}
</script>

<template>
  <!-- Toast -->
  <Transition name="toast">
    <div
      v-if="toast"
      :class="[
        'fixed top-6 right-6 z-[100] flex items-center gap-3 px-5 py-3.5 rounded-xl shadow-2xl text-sm font-medium border backdrop-blur-sm',
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

  <div class="max-w-[1400px] w-full mx-auto p-6 md:p-10 flex flex-col gap-8">
    <!-- Header -->
    <div
      class="flex flex-col md:flex-row md:items-center justify-between gap-6 pb-4 border-b border-stone-200 dark:border-stone-800"
    >
      <div>
        <h1 class="text-slate-900 dark:text-white text-3xl font-bold leading-tight tracking-tight">
          Department Management
        </h1>
        <p class="text-slate-500 dark:text-slate-400 mt-2 text-sm">
          Organize and manage institutional departments and faculty allocation.
        </p>
      </div>
      <button
        class="inline-flex items-center gap-2 rounded-lg bg-primary hover:bg-primary-dark text-white px-5 py-2.5 text-sm font-bold shadow-md shadow-orange-500/20 transition-all whitespace-nowrap"
        @click="handleAddDepartment"
      >
        <span class="material-symbols-outlined text-[20px]">add_circle</span>
        Add New Department
      </button>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div
        class="flex items-center gap-5 rounded-xl p-6 bg-surface-light dark:bg-surface-dark border border-stone-200 dark:border-stone-800 shadow-sm"
      >
        <div class="p-3 rounded-lg bg-orange-100 dark:bg-orange-900/20 text-primary">
          <span class="material-symbols-outlined text-3xl">corporate_fare</span>
        </div>
        <div>
          <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">Total Departments</p>
          <p class="text-slate-900 dark:text-white text-3xl font-bold mt-0.5">
            {{ isLoading ? '...' : totalDepartments }}
          </p>
        </div>
      </div>
      <div
        class="flex items-center gap-5 rounded-xl p-6 bg-surface-light dark:bg-surface-dark border border-stone-200 dark:border-stone-800 shadow-sm"
      >
        <div
          class="p-3 rounded-lg bg-blue-100 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400"
        >
          <span class="material-symbols-outlined text-3xl">menu_book</span>
        </div>
        <div>
          <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">Total Courses</p>
          <p class="text-slate-900 dark:text-white text-3xl font-bold mt-0.5">
            {{ isLoading ? '...' : totalCourses }}
          </p>
        </div>
      </div>
    </div>

    <!-- Filters & Search -->
    <div
      class="flex flex-col md:flex-row items-center gap-4 bg-white dark:bg-surface-dark p-4 rounded-xl border border-stone-200 dark:border-stone-800"
    >
      <div class="relative flex-1 w-full">
        <span
          class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]"
          >search</span
        >
        <input
          v-model="searchQuery"
          class="w-full pl-10 pr-4 py-2 bg-stone-50 dark:bg-stone-800/50 border-stone-200 dark:border-stone-700 rounded-lg text-sm focus:ring-primary focus:border-primary transition-all"
          placeholder="Search department name..."
          type="text"
        />
      </div>
      <div class="flex items-center gap-4 w-full md:w-auto">
        <select
          v-model="statusFilter"
          class="w-full md:w-48 py-2 bg-stone-50 dark:bg-stone-800/50 border-stone-200 dark:border-stone-700 rounded-lg text-sm focus:ring-primary focus:border-primary"
        >
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
        </select>
        <select
          v-model="sortBy"
          class="w-full md:w-48 py-2 bg-stone-50 dark:bg-stone-800/50 border-stone-200 dark:border-stone-700 rounded-lg text-sm focus:ring-primary focus:border-primary"
        >
          <option value="createdAt,desc">Newest First</option>
          <option value="createdAt,asc">Oldest First</option>
          <option value="name,asc">Name A-Z</option>
          <option value="name,desc">Name Z-A</option>
        </select>
        <button
          class="flex items-center justify-center gap-1 h-[38px] px-4 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-slate-700 dark:text-slate-300 rounded-lg font-semibold transition-all text-sm"
          @click="clearFilters"
          title="Clear Filters"
        >
          <span class="material-symbols-outlined text-[18px]">filter_list</span>
          <span class="hidden md:inline">Clear</span>
        </button>
      </div>
    </div>

    <!-- Error Message -->
    <div
      v-if="errorMessage"
      class="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-red-600 dark:text-red-400 text-sm"
    >
      {{ errorMessage }}
    </div>

    <!-- Table -->
    <div
      class="w-full overflow-hidden rounded-xl border border-stone-200 dark:border-stone-800 bg-surface-light dark:bg-surface-dark shadow-sm"
    >
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr
              class="bg-stone-50 dark:bg-stone-900/50 border-b border-stone-200 dark:border-stone-800"
            >
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400"
              >
                ID
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400"
              >
                Name
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400"
              >
                Office Location
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400"
              >
                Total Courses
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400"
              >
                Created At
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400"
              >
                Status
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 text-right"
              >
                Actions
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-200 dark:divide-stone-800">
            <tr
              v-if="isLoading"
              class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors"
            >
              <td colspan="7" class="p-8 text-center text-slate-500 dark:text-slate-400">
                Loading departments...
              </td>
            </tr>
            <tr
              v-else-if="departments.length === 0"
              class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors"
            >
              <td colspan="7" class="p-8 text-center text-slate-500 dark:text-slate-400">
                No departments found
              </td>
            </tr>
            <tr
              v-for="dept in departments"
              :key="dept.departmentId"
              class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors"
            >
              <td class="p-4 text-sm font-medium text-slate-500">
                {{ dept.departmentId }}
              </td>
              <td class="p-4 text-sm font-bold text-slate-900 dark:text-white">
                {{ dept.name }}
              </td>
              <td class="p-4 text-sm text-slate-600 dark:text-slate-300">
                <div v-if="dept.officeLocation" class="flex items-center gap-1.5">
                  <span class="material-symbols-outlined text-[18px] text-stone-400"
                    >location_on</span
                  >
                  {{ dept.officeLocation }}
                </div>
                <span v-else class="text-slate-400 dark:text-slate-500 italic">—</span>
              </td>
              <td class="p-4 text-sm text-slate-600 dark:text-slate-300">
                {{ dept.courseCount }}
              </td>
              <td class="p-4 text-sm text-slate-600 dark:text-slate-300">
                {{ formatDate(dept.createdAt) }}
              </td>
              <td class="p-4">
                <div class="flex items-center gap-2">
                  <span
                    v-if="dept.status === 'ACTIVE'"
                    class="inline-flex items-center justify-center gap-1 w-[70px] rounded-full bg-green-50 dark:bg-green-900/30 px-2 py-1 text-xs font-medium text-green-700 dark:text-green-400 ring-1 ring-inset ring-green-600/20 dark:ring-green-500/20"
                  >
                    <span class="w-1.5 h-1.5 rounded-full bg-green-600 dark:bg-green-400"></span>
                    Active
                  </span>
                  <span
                    v-else
                    class="inline-flex items-center justify-center gap-1 w-[70px] rounded-full bg-stone-100 dark:bg-stone-800 px-2 py-1 text-xs font-medium text-stone-600 dark:text-stone-400 ring-1 ring-inset ring-stone-500/20 dark:ring-stone-400/20"
                  >
                    <span class="w-1.5 h-1.5 rounded-full bg-stone-500 dark:bg-stone-400"></span>
                    Inactive
                  </span>
                  <button
                    :class="[
                      'p-1 rounded-md transition-colors',
                      dept.status === 'ACTIVE'
                        ? 'text-green-500 hover:text-green-700 hover:bg-green-50 dark:hover:bg-green-900/20'
                        : 'text-slate-400 hover:text-slate-600 hover:bg-stone-100 dark:hover:bg-stone-800',
                    ]"
                    :title="
                      dept.status === 'ACTIVE' ? 'Deactivate department' : 'Activate department'
                    "
                    @click="toggleDepartmentStatus(dept)"
                  >
                    <span class="material-symbols-outlined text-[20px]">
                      {{ dept.status === 'ACTIVE' ? 'toggle_on' : 'toggle_off' }}
                    </span>
                  </button>
                </div>
              </td>
              <td class="p-4 text-right space-x-2">
                <button
                  class="p-2 text-slate-400 hover:text-primary transition-colors"
                  @click="handleEditDepartment(dept)"
                >
                  <span class="material-symbols-outlined text-[20px]">edit</span>
                </button>
                <button
                  class="p-2 text-slate-400 hover:text-red-500 transition-colors"
                  @click="handleDeleteDepartment(dept)"
                >
                  <span class="material-symbols-outlined text-[20px]">delete</span>
                </button>
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
            >Page {{ currentPage }} of {{ totalPages }}</span
          >
          <div class="flex gap-1">
            <button
              class="w-9 h-9 flex items-center justify-center rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-400 hover:text-primary transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="currentPage <= 1"
              aria-label="Previous page"
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
              aria-label="Next page"
              @click="goToPage(currentPage + 1)"
            >
              <span class="material-symbols-outlined text-[18px]">chevron_right</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Add New Department Modal -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showAddDepartmentModal"
          class="fixed inset-0 z-50 flex items-center justify-center p-4"
          role="dialog"
        >
          <div
            class="absolute inset-0 bg-black/50 backdrop-blur-sm"
            @click="closeAddDepartmentModal"
          ></div>
          <div
            class="relative w-full max-w-md bg-white dark:bg-surface-dark rounded-2xl shadow-2xl p-6 flex flex-col gap-5"
          >
            <!-- Header -->
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-xl bg-orange-100 dark:bg-orange-900/20 text-primary">
                  <span class="material-symbols-outlined">add_business</span>
                </div>
                <h2 class="text-lg font-bold text-slate-900 dark:text-white">Add New Department</h2>
              </div>
              <button
                class="p-1.5 rounded-lg hover:bg-stone-100 dark:hover:bg-stone-800 text-slate-400 transition-colors"
                @click="closeAddDepartmentModal"
              >
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
            <!-- Form -->
            <form class="flex flex-col gap-4" @submit.prevent="submitNewDepartment">
              <p
                v-if="createDepartmentError"
                class="text-sm text-red-500 flex items-center gap-1.5"
              >
                <span class="material-symbols-outlined text-[16px]">error</span>
                {{ createDepartmentError }}
              </p>
              <div class="flex flex-col gap-1.5">
                <label
                  class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                  for="dept-add-name"
                >
                  Department Name <span class="text-red-500">*</span>
                </label>
                <input
                  id="dept-add-name"
                  v-model="newDepartment.name"
                  required
                  class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all"
                  placeholder="e.g. Computer Science"
                  type="text"
                />
              </div>
              <div class="flex flex-col gap-1.5">
                <label
                  class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                  for="dept-add-office"
                >
                  Office Location
                </label>
                <input
                  id="dept-add-office"
                  v-model="newDepartment.officeLocation"
                  class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all"
                  placeholder="e.g. Building A, Floor 3"
                  type="text"
                />
              </div>
              <!-- Footer -->
              <div class="flex justify-end gap-3 pt-1">
                <button
                  type="button"
                  class="px-4 py-2 text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-stone-100 dark:hover:bg-stone-800 rounded-xl transition-colors"
                  @click="closeAddDepartmentModal"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  :disabled="createDepartmentLoading"
                  class="flex items-center gap-2 px-5 py-2 bg-primary hover:bg-primary-dark disabled:opacity-60 text-white text-sm font-bold rounded-xl transition-all active:scale-95"
                >
                  <span
                    v-if="createDepartmentLoading"
                    class="material-symbols-outlined text-[18px] animate-spin"
                    >progress_activity</span
                  >
                  Create Department
                </button>
              </div>
            </form>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Edit Department Modal -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showEditDepartmentModal"
          class="fixed inset-0 z-50 flex items-center justify-center p-4"
          role="dialog"
        >
          <div
            class="absolute inset-0 bg-black/50 backdrop-blur-sm"
            @click="closeEditDepartmentModal"
          ></div>
          <div
            class="relative w-full max-w-md bg-white dark:bg-surface-dark rounded-2xl shadow-2xl p-6 flex flex-col gap-5"
          >
            <!-- Header -->
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-xl bg-orange-100 dark:bg-orange-900/20 text-primary">
                  <span class="material-symbols-outlined">edit</span>
                </div>
                <div>
                  <h2 class="text-lg font-bold text-slate-900 dark:text-white">Edit Department</h2>
                  <p class="text-xs text-slate-400">{{ editingDepartment?.name }}</p>
                </div>
              </div>
              <button
                class="p-1.5 rounded-lg hover:bg-stone-100 dark:hover:bg-stone-800 text-slate-400 transition-colors"
                @click="closeEditDepartmentModal"
              >
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
            <!-- Form -->
            <form class="flex flex-col gap-4" @submit.prevent="submitUpdateDepartment">
              <p
                v-if="updateDepartmentError"
                class="text-sm text-red-500 flex items-center gap-1.5"
              >
                <span class="material-symbols-outlined text-[16px]">error</span>
                {{ updateDepartmentError }}
              </p>
              <div class="flex flex-col gap-1.5">
                <label
                  class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                  for="dept-edit-name"
                >
                  Department Name <span class="text-red-500">*</span>
                </label>
                <input
                  id="dept-edit-name"
                  v-model="editDepartment.name"
                  required
                  class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all"
                  placeholder="e.g. Computer Science"
                  type="text"
                />
              </div>
              <div class="flex flex-col gap-1.5">
                <label
                  class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                  for="dept-edit-office"
                >
                  Office Location
                </label>
                <input
                  id="dept-edit-office"
                  v-model="editDepartment.officeLocation"
                  class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all"
                  placeholder="e.g. Building A, Floor 3"
                  type="text"
                />
              </div>
              <!-- Footer -->
              <div class="flex justify-end gap-3 pt-1">
                <button
                  type="button"
                  class="px-4 py-2 text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-stone-100 dark:hover:bg-stone-800 rounded-xl transition-colors"
                  @click="closeEditDepartmentModal"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  :disabled="updateDepartmentLoading"
                  class="flex items-center gap-2 px-5 py-2 bg-primary hover:bg-primary-dark disabled:opacity-60 text-white text-sm font-bold rounded-xl transition-all active:scale-95"
                >
                  <span
                    v-if="updateDepartmentLoading"
                    class="material-symbols-outlined text-[18px] animate-spin"
                    >progress_activity</span
                  >
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Delete Confirmation Modal -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showDeleteConfirmModal"
          class="fixed inset-0 z-50 flex items-center justify-center p-4"
          role="dialog"
        >
          <div
            class="absolute inset-0 bg-black/50 backdrop-blur-sm"
            @click="closeDeleteConfirmModal"
          ></div>
          <div
            class="relative w-full max-w-sm bg-white dark:bg-surface-dark rounded-2xl shadow-2xl p-6 flex flex-col gap-5"
          >
            <div class="flex flex-col items-center text-center gap-3">
              <div class="p-3 rounded-full bg-red-100 dark:bg-red-900/20">
                <span class="material-symbols-outlined text-3xl text-red-500">delete_forever</span>
              </div>
              <div>
                <h2 class="text-lg font-bold text-slate-900 dark:text-white">Delete Department</h2>
                <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">
                  Are you sure you want to delete
                  <span class="font-semibold text-slate-700 dark:text-slate-200">{{
                    deletingDepartment?.name
                  }}</span
                  >? This action cannot be undone.
                </p>
              </div>
              <div
                class="w-full flex items-start gap-2 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-xl p-3"
              >
                <span class="material-symbols-outlined text-amber-500 text-[18px] mt-0.5"
                  >warning</span
                >
                <p class="text-xs text-amber-700 dark:text-amber-300 text-left">
                  Cannot be deleted if it has active teachers, students, or courses.
                </p>
              </div>
            </div>
            <div class="flex gap-3">
              <button
                type="button"
                class="flex-1 px-4 py-2.5 text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-stone-100 dark:hover:bg-stone-800 border border-stone-200 dark:border-stone-700 rounded-xl transition-colors"
                :disabled="deleteDepartmentLoading"
                @click="closeDeleteConfirmModal"
              >
                Cancel
              </button>
              <button
                type="button"
                :disabled="deleteDepartmentLoading"
                class="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 bg-red-500 hover:bg-red-600 disabled:opacity-60 text-white text-sm font-bold rounded-xl transition-all active:scale-95"
                @click="confirmDeleteDepartment"
              >
                <span
                  v-if="deleteDepartmentLoading"
                  class="material-symbols-outlined text-[18px] animate-spin"
                  >progress_activity</span
                >
                Delete
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
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
.toast-enter-active {
  transition: all 0.3s ease;
}
.toast-leave-active {
  transition: all 0.25s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateY(-12px) scale(0.95);
}
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.97);
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes zoom-in {
  from {
    transform: scale(0.95);
  }
  to {
    transform: scale(1);
  }
}

.animate-in {
  animation:
    fade-in 0.2s ease,
    zoom-in 0.2s ease;
}
</style>
