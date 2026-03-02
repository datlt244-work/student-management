<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { watchDebounced } from '@vueuse/core'
import {
  getAdminSemesterList,
  createAdminSemester,
  updateAdminSemester,
  deleteAdminSemester,
  setCurrentAdminSemester,
  type AdminSemesterListItem,
  type AdminCreateSemesterRequest,
  type SemesterName,
} from '@/services/adminUserService'
import { useToast } from '@/composables/useToast'

const { toast, showToast } = useToast()
const yearFilter = ref<number | ''>('')
const nameFilter = ref<SemesterName | ''>('')
const isCurrentFilter = ref<boolean | ''>('')
const pageSize = ref(10)
const currentPage = ref(1)
const totalPages = ref(0)
const totalElements = ref(0)

// ─── Data ─────────────────────────────────────────────────
const semesters = ref<AdminSemesterListItem[]>([])
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)

// ─── Stats ────────────────────────────────────────────────
const currentSemester = computed(() => semesters.value.find((s) => s.isCurrent))
const totalSemesters = computed(() => totalElements.value)

// ─── Add Modal ────────────────────────────────────────────

// ─── Add Modal ────────────────────────────────────────────
const showAddModal = ref(false)
const addForm = ref<AdminCreateSemesterRequest>({
  name: 'SPRING',
  year: new Date().getFullYear(),
  startDate: null,
  endDate: null,
})
const addLoading = ref(false)
const addError = ref<string | null>(null)

function openAddModal() {
  addForm.value = { name: 'SPRING', year: new Date().getFullYear(), startDate: null, endDate: null }
  addError.value = null
  showAddModal.value = true
}
function closeAddModal() {
  showAddModal.value = false
  addError.value = null
}

async function submitAdd() {
  addError.value = null
  if (!addForm.value.name || !addForm.value.year) {
    addError.value = 'Semester name and year are required.'
    return
  }
  if (addForm.value.year < 2000 || addForm.value.year > 2100) {
    addError.value = 'Year must be between 2000 and 2100.'
    return
  }
  if (
    addForm.value.startDate &&
    addForm.value.endDate &&
    addForm.value.startDate >= addForm.value.endDate
  ) {
    addError.value = 'End date must be after start date.'
    return
  }
  addLoading.value = true
  try {
    await createAdminSemester({
      ...addForm.value,
      startDate: addForm.value.startDate || null,
      endDate: addForm.value.endDate || null,
    })
    closeAddModal()
    await fetchSemesters()
    showToast('Semester created successfully.')
  } catch (err: unknown) {
    addError.value = err instanceof Error ? err.message : 'Failed to create semester.'
  } finally {
    addLoading.value = false
  }
}

// ─── Edit Modal ───────────────────────────────────────────
const showEditModal = ref(false)
const editingSemester = ref<AdminSemesterListItem | null>(null)
const editForm = ref({ startDate: '', endDate: '' })
const editLoading = ref(false)
const editError = ref<string | null>(null)

function openEditModal(sem: AdminSemesterListItem) {
  editingSemester.value = sem
  editForm.value = {
    startDate: sem.startDate?.substring(0, 10) ?? '',
    endDate: sem.endDate?.substring(0, 10) ?? '',
  }
  editError.value = null
  showEditModal.value = true
}
function closeEditModal() {
  showEditModal.value = false
  editingSemester.value = null
  editError.value = null
}

async function submitEdit() {
  if (!editingSemester.value) return
  editError.value = null
  if (
    editForm.value.startDate &&
    editForm.value.endDate &&
    editForm.value.startDate >= editForm.value.endDate
  ) {
    editError.value = 'End date must be after start date.'
    return
  }
  editLoading.value = true
  try {
    await updateAdminSemester(editingSemester.value.semesterId, {
      startDate: editForm.value.startDate || null,
      endDate: editForm.value.endDate || null,
    })
    closeEditModal()
    await fetchSemesters()
    showToast('Semester updated successfully.')
  } catch (err: unknown) {
    editError.value = err instanceof Error ? err.message : 'Failed to update semester.'
  } finally {
    editLoading.value = false
  }
}

// ─── Delete Modal ─────────────────────────────────────────
const showDeleteModal = ref(false)
const deletingSemester = ref<AdminSemesterListItem | null>(null)
const deleteLoading = ref(false)

function openDeleteModal(sem: AdminSemesterListItem) {
  deletingSemester.value = sem
  showDeleteModal.value = true
}
function closeDeleteModal() {
  showDeleteModal.value = false
  deletingSemester.value = null
}

async function confirmDelete() {
  if (!deletingSemester.value) return
  deleteLoading.value = true
  try {
    await deleteAdminSemester(deletingSemester.value.semesterId)
    closeDeleteModal()
    if (currentPage.value > 1 && semesters.value.length === 1) currentPage.value--
    await fetchSemesters()
    showToast('Semester deleted successfully.')
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Failed to delete semester.'
    showToast(msg, 'error')
    closeDeleteModal()
  } finally {
    deleteLoading.value = false
  }
}

// ─── Set Current ──────────────────────────────────────────
const setCurrentLoading = ref<number | null>(null)

async function handleSetCurrent(sem: AdminSemesterListItem) {
  if (sem.isCurrent) return
  setCurrentLoading.value = sem.semesterId
  try {
    await setCurrentAdminSemester(sem.semesterId)
    await fetchSemesters()
    showToast(`${sem.displayName} is now the current semester.`)
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Failed to set current semester.'
    showToast(msg, 'error')
  } finally {
    setCurrentLoading.value = null
  }
}

// ─── Fetch ────────────────────────────────────────────────
async function fetchSemesters() {
  isLoading.value = true
  errorMessage.value = null
  try {
    const yearVal = yearFilter.value
    const result = await getAdminSemesterList({
      page: currentPage.value - 1,
      size: pageSize.value,
      name: nameFilter.value || undefined,
      year: typeof yearVal === 'number' && !Number.isNaN(yearVal) ? yearVal : undefined,
      isCurrent: isCurrentFilter.value === '' ? undefined : isCurrentFilter.value === true,
    })
    semesters.value = result.content ?? []
    currentPage.value = (result.page ?? 0) + 1
    totalPages.value = result.totalPages ?? 0
    totalElements.value = result.totalElements ?? 0
  } catch (err: unknown) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load semesters.'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => fetchSemesters())

watchDebounced(
  [yearFilter, nameFilter, isCurrentFilter, pageSize],
  () => {
    currentPage.value = 1
    fetchSemesters()
  },
  { debounce: 400, maxWait: 1000 },
)

// ─── Pagination ───────────────────────────────────────────
function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value && page !== currentPage.value) {
    currentPage.value = page
    fetchSemesters()
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

// ─── Helpers ──────────────────────────────────────────────
function formatDate(dateString: string | null): string {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

/*
const getSemesterStatus = (startDate: string, endDate: string) => {
  const now = new Date()
  const start = new Date(startDate)
  const end = new Date(endDate)
  if (now < start) return 'Upcoming'
  if (now > end) return 'Completed'
  return 'Active'
}
*/
function clearSemesterFilters() {
  yearFilter.value = ''
  nameFilter.value = ''
  isCurrentFilter.value = ''
}
</script>

<template>
  <!-- ── Toast ─────────────────────────────────────────── -->
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
    <!-- ── Page Header ───────────────────────────────────── -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-slate-900 dark:text-white text-3xl font-bold leading-tight tracking-tight">
          Semester Management
        </h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">
          Organize academic periods, start dates, and term statuses.
        </p>
      </div>
      <div class="flex items-center gap-3">
        <button
          id="btn-add-semester"
          @click="openAddModal"
          class="flex items-center gap-2 px-6 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95"
        >
          <span class="material-symbols-outlined text-[20px]">add_circle</span>
          Add New Semester
        </button>
      </div>
    </div>

    <!-- ── Stats Cards ───────────────────────────────────── -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <!-- Total Semesters -->
      <div
        class="flex items-center gap-5 rounded-2xl p-6 bg-white dark:bg-surface-dark border border-stone-200 dark:border-stone-800 shadow-sm transition-all hover:shadow-md"
      >
        <div class="p-3 rounded-xl bg-orange-100 dark:bg-orange-900/20 text-primary flex-shrink-0">
          <span class="material-symbols-outlined text-3xl">calendar_month</span>
        </div>
        <div>
          <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">Total Semesters</p>
          <p class="text-slate-900 dark:text-white text-3xl font-bold mt-0.5">
            {{ isLoading ? '—' : totalSemesters }}
          </p>
        </div>
      </div>

      <!-- Current Semester -->
      <div
        class="flex items-center gap-5 rounded-2xl p-6 bg-white dark:bg-surface-dark border border-stone-200 dark:border-stone-800 border-l-4 border-l-green-500 shadow-sm transition-all hover:shadow-md"
      >
        <div
          class="p-3 rounded-xl bg-green-100 dark:bg-green-900/20 text-green-600 dark:text-green-400 flex-shrink-0"
        >
          <span class="material-symbols-outlined text-3xl">event_available</span>
        </div>
        <div>
          <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">
            Current Active Semester
          </p>
          <p class="text-slate-900 dark:text-white text-2xl font-bold mt-0.5">
            {{ currentSemester?.displayName ?? (isLoading ? '—' : 'Not configured') }}
          </p>
          <p v-if="currentSemester?.startDate" class="text-xs text-slate-400 mt-0.5">
            {{ formatDate(currentSemester.startDate) }} → {{ formatDate(currentSemester.endDate) }}
          </p>
        </div>
      </div>
    </div>

    <!-- ── Filters & Table ───────────────────────────────── -->
    <div class="flex flex-col gap-4">
      <!-- Filter Bar -->
      <div
        class="flex flex-col md:flex-row items-center gap-3 bg-white dark:bg-surface-dark p-4 rounded-2xl border border-stone-200 dark:border-stone-800 shadow-sm"
      >
        <div class="relative w-full md:w-32">
          <input
            v-model.number="yearFilter"
            class="w-full px-3 py-2.5 bg-stone-50 dark:bg-stone-800/50 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all"
            placeholder="Year..."
            type="number"
          />
        </div>
        <select
          v-model="nameFilter"
          class="w-full md:w-36 py-2.5 bg-stone-50 dark:bg-stone-800/50 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary"
        >
          <option value="">All Terms</option>
          <option value="SPRING">Spring</option>
          <option value="SUMMER">Summer</option>
          <option value="FALL">Fall</option>
        </select>
        <select
          v-model="isCurrentFilter"
          class="w-full md:w-40 py-2.5 bg-stone-50 dark:bg-stone-800/50 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary"
        >
          <option value="">All Status</option>
          <option :value="true">Active Only</option>
          <option :value="false">Inactive Only</option>
        </select>

        <div class="flex-1"></div>

        <button
          v-if="yearFilter !== '' || nameFilter !== '' || isCurrentFilter !== ''"
          class="flex items-center gap-1.5 px-3 py-2 text-sm text-slate-500 hover:text-slate-700 dark:hover:text-slate-200 bg-stone-100 dark:bg-stone-800 rounded-xl transition-colors whitespace-nowrap"
          @click="clearSemesterFilters"
        >
          <span class="material-symbols-outlined text-[18px]">filter_list_off</span>
          Clear Filters
        </button>
      </div>

      <!-- Table -->
      <div
        class="w-full overflow-hidden rounded-2xl border border-stone-200 dark:border-stone-800 bg-white dark:bg-surface-dark shadow-sm"
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
                  Semester
                </th>
                <th
                  class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400"
                >
                  Period
                </th>

                <th
                  class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400"
                >
                  Created At
                </th>
                <th
                  class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 text-right"
                >
                  Actions
                </th>
              </tr>
            </thead>

            <tbody class="divide-y divide-stone-100 dark:divide-stone-800">
              <!-- Loading skeleton -->
              <template v-if="isLoading">
                <tr v-for="i in 5" :key="i" class="animate-pulse">
                  <td class="p-4">
                    <div class="h-4 bg-stone-200 dark:bg-stone-700 rounded w-32"></div>
                  </td>
                  <td class="p-4">
                    <div class="h-4 bg-stone-200 dark:bg-stone-700 rounded w-40"></div>
                  </td>

                  <td class="p-4">
                    <div class="h-6 bg-stone-200 dark:bg-stone-700 rounded-full w-20"></div>
                  </td>
                  <td class="p-4 text-right">
                    <div class="h-4 bg-stone-200 dark:bg-stone-700 rounded w-20 ml-auto"></div>
                  </td>
                </tr>
              </template>

              <!-- Error -->
              <tr v-else-if="errorMessage">
                <td colspan="4" class="p-10 text-center">
                  <div class="flex flex-col items-center gap-3 text-red-500">
                    <span class="material-symbols-outlined text-4xl">error_outline</span>
                    <p class="text-sm font-medium">{{ errorMessage }}</p>
                    <button class="text-xs text-primary hover:underline" @click="fetchSemesters">
                      Try again
                    </button>
                  </div>
                </td>
              </tr>

              <!-- Empty -->
              <tr v-else-if="semesters.length === 0">
                <td colspan="4" class="p-12 text-center">
                  <div class="flex flex-col items-center gap-3 text-slate-400">
                    <span class="material-symbols-outlined text-5xl">calendar_month</span>
                    <p class="text-sm font-medium">No semesters found</p>
                    <p class="text-xs">Click "Add New Semester" to create one.</p>
                  </div>
                </td>
              </tr>

              <!-- Data rows -->
              <tr
                v-for="sem in semesters"
                v-else
                :key="sem.semesterId"
                class="hover:bg-stone-50/70 dark:hover:bg-stone-800/30 transition-colors group"
              >
                <!-- Semester name -->
                <td class="p-4">
                  <div class="flex items-center gap-2.5">
                    <div
                      :class="[
                        'w-2 h-2 rounded-full flex-shrink-0',
                        sem.isCurrent
                          ? 'bg-green-500 shadow-sm shadow-green-500'
                          : 'bg-stone-300 dark:bg-stone-600',
                      ]"
                    ></div>
                    <div>
                      <p class="text-sm font-bold text-slate-900 dark:text-white">
                        {{ sem.displayName }}
                      </p>
                      <p class="text-xs text-slate-400">ID: {{ sem.semesterId }}</p>
                    </div>
                  </div>
                </td>

                <!-- Period -->
                <td class="p-4">
                  <p class="text-sm text-slate-700 dark:text-slate-300">
                    {{ formatDate(sem.startDate) }}
                    <span class="text-slate-400 mx-1">→</span>
                    {{ formatDate(sem.endDate) }}
                  </p>
                </td>

                <!-- Created At -->
                <td class="p-4">
                  <p class="text-sm text-slate-500 dark:text-slate-400">
                    {{ formatDate(sem.createdAt) }}
                  </p>
                </td>

                <!-- Actions -->
                <td class="p-4 text-right">
                  <div
                    class="flex items-center justify-end gap-1 opacity-70 group-hover:opacity-100 transition-opacity"
                  >
                    <!-- Set current -->
                    <button
                      v-if="!sem.isCurrent"
                      :disabled="setCurrentLoading === sem.semesterId"
                      class="flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-slate-600 dark:text-slate-400 hover:text-green-600 dark:hover:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/20 rounded-lg transition-all disabled:opacity-40"
                      :title="`Set ${sem.displayName} as current`"
                      @click="handleSetCurrent(sem)"
                    >
                      <span
                        v-if="setCurrentLoading === sem.semesterId"
                        class="material-symbols-outlined text-[16px] animate-spin"
                        >progress_activity</span
                      >
                      <span v-else class="material-symbols-outlined text-[16px]"
                        >bookmark_star</span
                      >
                      <span class="hidden lg:inline">Set current</span>
                    </button>

                    <!-- Edit -->
                    <button
                      class="p-1.5 text-slate-500 hover:text-primary hover:bg-orange-50 dark:hover:bg-orange-900/20 rounded-lg transition-all"
                      title="Edit dates"
                      @click="openEditModal(sem)"
                    >
                      <span class="material-symbols-outlined text-[18px]">edit</span>
                    </button>

                    <!-- Delete -->
                    <button
                      :disabled="sem.isCurrent"
                      class="p-1.5 text-slate-500 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-all disabled:opacity-30 disabled:cursor-not-allowed"
                      :title="
                        sem.isCurrent ? 'Cannot delete the current semester' : 'Delete semester'
                      "
                      @click="openDeleteModal(sem)"
                    >
                      <span class="material-symbols-outlined text-[18px]">delete</span>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination footer -->
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
    </div>
  </div>

  <!-- ══════════════════════════════════════════════════════
       ADD MODAL
  ══════════════════════════════════════════════════════ -->
  <Transition name="modal">
    <div v-if="showAddModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/50 backdrop-blur-sm" @click="closeAddModal"></div>
      <div
        class="relative w-full max-w-md bg-white dark:bg-surface-dark rounded-2xl shadow-2xl p-6 flex flex-col gap-5"
      >
        <!-- Header -->
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="p-2 rounded-xl bg-orange-100 dark:bg-orange-900/20 text-primary">
              <span class="material-symbols-outlined">add_circle</span>
            </div>
            <h2 class="text-lg font-bold text-slate-900 dark:text-white">New Semester</h2>
          </div>
          <button
            class="p-1.5 rounded-lg hover:bg-stone-100 dark:hover:bg-stone-800 text-slate-400 transition-colors"
            @click="closeAddModal"
          >
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <!-- Form -->
        <div class="flex flex-col gap-4">
          <!-- Name + Year -->
          <div class="grid grid-cols-2 gap-3">
            <div class="flex flex-col gap-1.5">
              <label class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                >Term <span class="text-red-500">*</span></label
              >
              <select
                v-model="addForm.name"
                class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary"
              >
                <option value="SPRING">Spring</option>
                <option value="SUMMER">Summer</option>
                <option value="FALL">Fall</option>
              </select>
            </div>
            <div class="flex flex-col gap-1.5">
              <label class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                >Year <span class="text-red-500">*</span></label
              >
              <input
                v-model.number="addForm.year"
                class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary"
                type="number"
                min="2000"
                max="2100"
                placeholder="2026"
              />
            </div>
          </div>

          <!-- Start Date -->
          <div class="flex flex-col gap-1.5">
            <label class="text-xs font-semibold text-slate-600 dark:text-slate-300"
              >Start Date</label
            >
            <input
              v-model="addForm.startDate"
              class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary"
              type="date"
            />
          </div>

          <!-- End Date -->
          <div class="flex flex-col gap-1.5">
            <label class="text-xs font-semibold text-slate-600 dark:text-slate-300">End Date</label>
            <input
              v-model="addForm.endDate"
              class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary"
              type="date"
            />
          </div>

          <!-- Error -->
          <p v-if="addError" class="text-sm text-red-500 flex items-center gap-1.5">
            <span class="material-symbols-outlined text-[16px]">error</span>
            {{ addError }}
          </p>
        </div>

        <!-- Footer -->
        <div class="flex justify-end gap-3 pt-1">
          <button
            class="px-4 py-2 text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-stone-100 dark:hover:bg-stone-800 rounded-xl transition-colors"
            @click="closeAddModal"
          >
            Cancel
          </button>
          <button
            :disabled="addLoading"
            class="flex items-center gap-2 px-5 py-2 bg-primary hover:bg-primary-dark disabled:opacity-60 text-white text-sm font-bold rounded-xl transition-all active:scale-95"
            @click="submitAdd"
          >
            <span v-if="addLoading" class="material-symbols-outlined text-[18px] animate-spin"
              >progress_activity</span
            >
            Create Semester
          </button>
        </div>
      </div>
    </div>
  </Transition>

  <!-- ══════════════════════════════════════════════════════
       EDIT MODAL
  ══════════════════════════════════════════════════════ -->
  <Transition name="modal">
    <div
      v-if="showEditModal && editingSemester"
      class="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      <div class="absolute inset-0 bg-black/50 backdrop-blur-sm" @click="closeEditModal"></div>
      <div
        class="relative w-full max-w-md bg-white dark:bg-surface-dark rounded-2xl shadow-2xl p-6 flex flex-col gap-5"
      >
        <!-- Header -->
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="p-2 rounded-xl bg-orange-100 dark:bg-orange-900/20 text-primary">
              <span class="material-symbols-outlined">edit_calendar</span>
            </div>
            <div>
              <h2 class="text-lg font-bold text-slate-900 dark:text-white">Edit Semester</h2>
              <p class="text-xs text-slate-400">{{ editingSemester.displayName }}</p>
            </div>
          </div>
          <button
            class="p-1.5 rounded-lg hover:bg-stone-100 dark:hover:bg-stone-800 text-slate-400 transition-colors"
            @click="closeEditModal"
          >
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <!-- Note -->
        <div
          class="flex items-start gap-2 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-xl p-3"
        >
          <span class="material-symbols-outlined text-[18px] text-blue-500 mt-0.5">info</span>
          <p class="text-xs text-blue-700 dark:text-blue-300">
            Only start and end dates can be modified. Term name and year are locked.
          </p>
        </div>

        <!-- Form -->
        <div class="flex flex-col gap-4">
          <div class="flex flex-col gap-1.5">
            <label class="text-xs font-semibold text-slate-600 dark:text-slate-300"
              >Start Date</label
            >
            <input
              v-model="editForm.startDate"
              class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary"
              type="date"
            />
          </div>
          <div class="flex flex-col gap-1.5">
            <label class="text-xs font-semibold text-slate-600 dark:text-slate-300">End Date</label>
            <input
              v-model="editForm.endDate"
              class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary"
              type="date"
            />
          </div>
          <p v-if="editError" class="text-sm text-red-500 flex items-center gap-1.5">
            <span class="material-symbols-outlined text-[16px]">error</span>
            {{ editError }}
          </p>
        </div>

        <!-- Footer -->
        <div class="flex justify-end gap-3 pt-1">
          <button
            class="px-4 py-2 text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-stone-100 dark:hover:bg-stone-800 rounded-xl transition-colors"
            @click="closeEditModal"
          >
            Cancel
          </button>
          <button
            :disabled="editLoading"
            class="flex items-center gap-2 px-5 py-2 bg-primary hover:bg-primary-dark disabled:opacity-60 text-white text-sm font-bold rounded-xl transition-all active:scale-95"
            @click="submitEdit"
          >
            <span v-if="editLoading" class="material-symbols-outlined text-[18px] animate-spin"
              >progress_activity</span
            >
            Save Changes
          </button>
        </div>
      </div>
    </div>
  </Transition>

  <!-- ══════════════════════════════════════════════════════
       DELETE CONFIRM MODAL
  ══════════════════════════════════════════════════════ -->
  <Transition name="modal">
    <div
      v-if="showDeleteModal && deletingSemester"
      class="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      <div class="absolute inset-0 bg-black/50 backdrop-blur-sm" @click="closeDeleteModal"></div>
      <div
        class="relative w-full max-w-sm bg-white dark:bg-surface-dark rounded-2xl shadow-2xl p-6 flex flex-col gap-5"
      >
        <div class="flex flex-col items-center text-center gap-3">
          <div class="p-3 rounded-full bg-red-100 dark:bg-red-900/20">
            <span class="material-symbols-outlined text-3xl text-red-500">delete_forever</span>
          </div>
          <div>
            <h2 class="text-lg font-bold text-slate-900 dark:text-white">Delete Semester</h2>
            <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">
              Are you sure you want to delete
              <span class="font-semibold text-slate-700 dark:text-slate-200">{{
                deletingSemester.displayName
              }}</span
              >? This action cannot be undone.
            </p>
          </div>
        </div>
        <div class="flex gap-3">
          <button
            class="flex-1 px-4 py-2.5 text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-stone-100 dark:hover:bg-stone-800 border border-stone-200 dark:border-stone-700 rounded-xl transition-colors"
            @click="closeDeleteModal"
          >
            Cancel
          </button>
          <button
            :disabled="deleteLoading"
            class="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 bg-red-500 hover:bg-red-600 disabled:opacity-60 text-white text-sm font-bold rounded-xl transition-all active:scale-95"
            @click="confirmDelete"
          >
            <span v-if="deleteLoading" class="material-symbols-outlined text-[18px] animate-spin"
              >progress_activity</span
            >
            Delete
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(2rem);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(2rem);
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.95) translateY(0.5rem);
}
</style>
