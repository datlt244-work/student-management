<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { watchDebounced } from '@vueuse/core'
import {
  getAdminDepartmentList,
  type AdminDepartmentListItem,
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
        @click="() => {}"
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
                  @click="() => {}"
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
  </div>
</template>

