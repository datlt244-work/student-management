<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { watchDebounced } from '@vueuse/core'
import {
  getAdminDepartmentList,
  createAdminDepartment,
  updateAdminDepartment,
  type AdminDepartmentListItem,
  type AdminCreateDepartmentRequest,
  type AdminUpdateDepartmentRequest,
} from '@/services/adminUserService'

// Filters & pagination
const searchQuery = ref('')
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

// Stats (computed from data)
const totalDepartments = computed(() => totalElements.value)

// Add Department modal state
const showAddDepartmentModal = ref(false)
const newDepartment = ref({
  name: '',
  officeLocation: '',
})

// Create department submit state
const createDepartmentLoading = ref(false)
const createDepartmentError = ref<string | null>(null)

// Create department result modal state
const showCreateResultModal = ref(false)
const createResultSuccess = ref(false)
const createResultMessage = ref('')

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

async function fetchDepartments() {
  try {
    isLoading.value = true
    errorMessage.value = null

    const result = await getAdminDepartmentList({
      page: currentPage.value - 1, // Convert to 0-based
      size: pageSize.value,
      sort: sortBy.value || 'createdAt,desc',
      search: searchQuery.value || undefined,
    })

    departments.value = result.content
    currentPage.value = result.page + 1
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'message' in err) {
      errorMessage.value = String((err as { message?: unknown }).message) || 'Failed to load departments'
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
  [searchQuery, sortBy, pageSize],
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
    createResultSuccess.value = true
    createResultMessage.value = 'Department has been created successfully.'
    showCreateResultModal.value = true
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Failed to create department'
    createDepartmentError.value = msg
    createResultSuccess.value = false
    createResultMessage.value = msg
    showCreateResultModal.value = true
  } finally {
    createDepartmentLoading.value = false
  }
}

function closeCreateResultModal() {
  showCreateResultModal.value = false
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
    createResultSuccess.value = true
    createResultMessage.value = 'Department has been updated successfully.'
    showCreateResultModal.value = true
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Failed to update department'
    updateDepartmentError.value = msg
    createResultSuccess.value = false
    createResultMessage.value = msg
    showCreateResultModal.value = true
  } finally {
    updateDepartmentLoading.value = false
  }
}

</script>

<template>
  <div class="max-w-[1400px] w-full mx-auto p-6 md:p-10 flex flex-col gap-8">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-6 pb-4 border-b border-stone-200 dark:border-stone-800">
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
      <div class="flex items-center gap-5 rounded-xl p-6 bg-surface-light dark:bg-surface-dark border border-stone-200 dark:border-stone-800 shadow-sm">
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
    </div>

    <!-- Filters & Search -->
    <div class="flex flex-col md:flex-row items-center gap-4 bg-white dark:bg-surface-dark p-4 rounded-xl border border-stone-200 dark:border-stone-800">
      <div class="relative flex-1 w-full">
        <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]">search</span>
        <input
          v-model="searchQuery"
          class="w-full pl-10 pr-4 py-2 bg-stone-50 dark:bg-stone-800/50 border-stone-200 dark:border-stone-700 rounded-lg text-sm focus:ring-primary focus:border-primary transition-all"
          placeholder="Search department name..."
          type="text"
        />
      </div>
      <div class="flex items-center gap-4 w-full md:w-auto">
        <select
          v-model="sortBy"
          class="w-full md:w-48 py-2 bg-stone-50 dark:bg-stone-800/50 border-stone-200 dark:border-stone-700 rounded-lg text-sm focus:ring-primary focus:border-primary"
        >
          <option value="createdAt,desc">Newest First</option>
          <option value="createdAt,asc">Oldest First</option>
          <option value="name,asc">Name A-Z</option>
          <option value="name,desc">Name Z-A</option>
        </select>
      </div>
    </div>

    <!-- Error Message -->
    <div v-if="errorMessage" class="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-red-600 dark:text-red-400 text-sm">
      {{ errorMessage }}
    </div>

    <!-- Table -->
    <div class="w-full overflow-hidden rounded-xl border border-stone-200 dark:border-stone-800 bg-surface-light dark:bg-surface-dark shadow-sm">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-stone-50 dark:bg-stone-900/50 border-b border-stone-200 dark:border-stone-800">
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">ID</th>
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Name</th>
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Office Location</th>
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Created At</th>
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 text-right">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-200 dark:divide-stone-800">
            <tr v-if="isLoading" class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors">
              <td colspan="5" class="p-8 text-center text-slate-500 dark:text-slate-400">
                Loading departments...
              </td>
            </tr>
            <tr v-else-if="departments.length === 0" class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors">
              <td colspan="5" class="p-8 text-center text-slate-500 dark:text-slate-400">
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
                  <span class="material-symbols-outlined text-[18px] text-stone-400">location_on</span>
                  {{ dept.officeLocation }}
                </div>
                <span v-else class="text-slate-400 dark:text-slate-500 italic">—</span>
              </td>
              <td class="p-4 text-sm text-slate-600 dark:text-slate-300">
                {{ formatDate(dept.createdAt) }}
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
                  @click="() => {}"
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
          <span class="text-sm font-medium text-slate-700 dark:text-slate-300 mr-2">Page {{ currentPage }} of {{ totalPages }}</span>
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
          class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm bg-stone-900/50"
          role="dialog"
          @click.self="closeAddDepartmentModal"
        >
          <div class="w-full max-w-md bg-surface-light dark:bg-surface-dark rounded-xl shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">
            <div class="bg-primary px-6 py-4 flex items-center justify-between">
              <h2 class="text-white text-lg font-bold">Add New Department</h2>
              <button
                class="text-white/80 hover:text-white transition-colors rounded-lg p-1 hover:bg-white/10"
                @click="closeAddDepartmentModal"
              >
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
            <form class="p-6 flex flex-col gap-5" @submit.prevent="submitNewDepartment">
              <div v-if="createDepartmentError" class="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 text-sm">
                {{ createDepartmentError }}
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-slate-700 dark:text-slate-300" for="name">
                  Department Name <span class="text-red-500">*</span>
                </label>
                <input
                  id="name"
                  v-model="newDepartment.name"
                  required
                  class="w-full px-4 py-2.5 bg-stone-50 dark:bg-stone-800/50 border border-stone-200 dark:border-stone-700 rounded-lg text-sm text-slate-900 dark:text-white focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-all placeholder:text-slate-400"
                  placeholder="e.g. Computer Science"
                  type="text"
                />
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-slate-700 dark:text-slate-300" for="officeLocation">
                  Office Location
                </label>
                <input
                  id="officeLocation"
                  v-model="newDepartment.officeLocation"
                  class="w-full px-4 py-2.5 bg-stone-50 dark:bg-stone-800/50 border border-stone-200 dark:border-stone-700 rounded-lg text-sm text-slate-900 dark:text-white focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-all placeholder:text-slate-400"
                  placeholder="e.g. Building A, Floor 3"
                  type="text"
                />
              </div>
              <div class="px-0 py-0 bg-stone-50 dark:bg-stone-900/30 border-t border-stone-100 dark:border-stone-800 flex items-center justify-end gap-3 mt-2 pt-4">
                <button
                  type="button"
                  class="px-4 py-2 rounded-lg text-sm font-semibold text-slate-600 dark:text-slate-400 hover:bg-stone-200 dark:hover:bg-stone-800 transition-colors"
                  @click="closeAddDepartmentModal"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  :disabled="createDepartmentLoading"
                  class="px-4 py-2 rounded-lg text-sm font-bold text-white bg-primary hover:bg-primary-dark shadow-md shadow-orange-500/20 transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {{ createDepartmentLoading ? 'Creating...' : 'Create Department' }}
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
          class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm bg-stone-900/50"
          role="dialog"
          @click.self="closeEditDepartmentModal"
        >
          <div class="w-full max-w-md bg-surface-light dark:bg-surface-dark rounded-xl shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">
            <div class="bg-primary px-6 py-4 flex items-center justify-between">
              <h2 class="text-white text-lg font-bold">Edit Department</h2>
              <button
                class="text-white/80 hover:text-white transition-colors rounded-lg p-1 hover:bg-white/10"
                @click="closeEditDepartmentModal"
              >
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
            <form class="p-6 flex flex-col gap-5" @submit.prevent="submitUpdateDepartment">
              <div v-if="updateDepartmentError" class="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 text-sm">
                {{ updateDepartmentError }}
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-slate-700 dark:text-slate-300" for="edit-name">
                  Department Name <span class="text-red-500">*</span>
                </label>
                <input
                  id="edit-name"
                  v-model="editDepartment.name"
                  required
                  class="w-full px-4 py-2.5 bg-stone-50 dark:bg-stone-800/50 border border-stone-200 dark:border-stone-700 rounded-lg text-sm text-slate-900 dark:text-white focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-all placeholder:text-slate-400"
                  placeholder="e.g. Computer Science"
                  type="text"
                />
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-slate-700 dark:text-slate-300" for="edit-officeLocation">
                  Office Location
                </label>
                <input
                  id="edit-officeLocation"
                  v-model="editDepartment.officeLocation"
                  class="w-full px-4 py-2.5 bg-stone-50 dark:bg-stone-800/50 border border-stone-200 dark:border-stone-700 rounded-lg text-sm text-slate-900 dark:text-white focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-all placeholder:text-slate-400"
                  placeholder="e.g. Building A, Floor 3"
                  type="text"
                />
              </div>
              <div class="px-0 py-0 bg-stone-50 dark:bg-stone-900/30 border-t border-stone-100 dark:border-stone-800 flex items-center justify-end gap-3 mt-2 pt-4">
                <button
                  type="button"
                  class="px-4 py-2 rounded-lg text-sm font-semibold text-slate-600 dark:text-slate-400 hover:bg-stone-200 dark:hover:bg-stone-800 transition-colors"
                  @click="closeEditDepartmentModal"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  :disabled="updateDepartmentLoading"
                  class="px-4 py-2 rounded-lg text-sm font-bold text-white bg-primary hover:bg-primary-dark shadow-md shadow-orange-500/20 transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {{ updateDepartmentLoading ? 'Updating...' : 'Update Department' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Create Department Result Modal -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showCreateResultModal"
          class="fixed inset-0 z-120 flex items-center justify-center p-4 backdrop-blur-md bg-stone-900/40"
        >
          <div
            class="bg-surface-light dark:bg-surface-dark w-full max-w-md rounded-xl shadow-2xl overflow-hidden border border-stone-200 dark:border-stone-800 max-h-[80vh] flex flex-col"
          >
            <div
              :class="[
                'px-6 py-4 flex items-center justify-between shrink-0',
                createResultSuccess ? 'bg-emerald-600' : 'bg-red-600',
              ]"
            >
              <h2 class="text-white text-lg font-bold flex items-center gap-2">
                <span class="material-symbols-outlined">
                  {{ createResultSuccess ? 'check_circle' : 'error' }}
                </span>
                {{ createResultSuccess ? 'Department Created' : 'Create Failed' }}
              </h2>
              <button class="text-white/80 hover:text-white transition-colors" type="button" @click="closeCreateResultModal">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>

            <div class="p-6 flex-1 flex items-center">
              <p class="text-sm text-slate-700 dark:text-slate-200">
                {{ createResultMessage }}
              </p>
            </div>

            <div class="px-6 py-4 border-t border-stone-200 dark:border-stone-800 bg-stone-50/60 dark:bg-stone-900/40 shrink-0 flex justify-end">
              <button
                type="button"
                class="px-5 py-2 rounded-lg bg-primary hover:bg-primary-dark text-white text-sm font-bold shadow-md shadow-primary/30 transition-all active:scale-95"
                @click="closeCreateResultModal"
              >
                OK
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
  animation: fade-in 0.2s ease, zoom-in 0.2s ease;
}
</style>

