<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { watchDebounced } from '@vueuse/core'
import {
  getAdminCourses,
  getAdminDepartments,
  updateAdminCourseStatus,
  createAdminCourse,
  type AdminCourseListItem,
  type AdminDepartmentItem,
} from '@/services/adminUserService'

const router = useRouter()

const searchQuery = ref('')
const statusFilter = ref('')
const sortBy = ref<string>('createdAt,desc')
const selectedDepartment = ref<string | number>('all')

const courses = ref<AdminCourseListItem[]>([])
const departments = ref<AdminDepartmentItem[]>([])
const isLoading = ref(false)
const error = ref<string | null>(null)

// Add Course Modal State
const showAddCourseModal = ref(false)
const createCourseLoading = ref(false)
const createCourseError = ref<string | null>(null)
const newCourse = ref({
  name: '',
  code: '',
  credits: 3.0,
  departmentId: '' as string | number,
  description: '',
})

// Modal state
const showResultModal = ref(false)
const resultSuccess = ref(false)
const resultMessage = ref('')

function closeResultModal() {
  showResultModal.value = false
}

// Pagination
const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(1)

// Stats (Partial)
const totalCourses = computed(() => totalElements.value)
const totalCredits = computed(() => 0) // API doesn't provide sum
const activeDepartments = computed(() => 0) // API doesn't provide active count

const paginationPages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const pages: number[] = []
  const start = Math.max(1, current - 1)
  const end = Math.min(total, current + 1)
  if (total <= 1) return [1]
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

async function fetchData() {
  await Promise.all([fetchCourses(), fetchDepartments()])
}

async function fetchDepartments() {
  try {
    departments.value = await getAdminDepartments()
  } catch (e) {
    console.error('Failed to fetch departments', e)
  }
}

async function fetchCourses() {
  isLoading.value = true
  error.value = null
  try {
    const deptId = selectedDepartment.value === 'all' ? '' : Number(selectedDepartment.value)

    const result = await getAdminCourses({
      page: currentPage.value,
      size: pageSize.value,
      search: searchQuery.value,
      departmentId: deptId || '',
      status: statusFilter.value,
      sort: sortBy.value || 'createdAt,desc',
    })

    courses.value = result.content
    totalElements.value = result.totalElements
    totalPages.value = result.totalPages
  } catch (e: unknown) {
    if (e && typeof e === 'object' && 'message' in e) {
       error.value = String((e as { message?: unknown }).message) || 'Failed to load courses'
    } else {
       error.value = 'Failed to load courses'
    }
    courses.value = []
  } finally {
    isLoading.value = false
  }
}

function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value && page !== currentPage.value) {
    currentPage.value = page
    fetchCourses()
  }
}

watchDebounced(
  [searchQuery, selectedDepartment, statusFilter, pageSize, sortBy],
  () => {
    currentPage.value = 1
    fetchCourses()
  },
  { debounce: 500, maxWait: 1000 },
)



onMounted(() => {
  fetchData()
})

async function toggleCourseStatus(course: AdminCourseListItem) {
  const originalStatus = course.status
  const newStatus = course.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'

  try {
    course.status = newStatus // Optimistic update
    await updateAdminCourseStatus(course.courseId, newStatus)
  } catch (e: unknown) {
    course.status = originalStatus // Revert on failure
    console.error('Failed to update status', e)
    if (e && typeof e === 'object' && 'message' in e) {
      resultMessage.value = String((e as { message?: unknown }).message)
    } else {
      resultMessage.value = 'Failed to update status'
    }
    resultSuccess.value = false
    showResultModal.value = true
  }
}

function handleAddCourse() {
  showAddCourseModal.value = true
  createCourseError.value = null
  newCourse.value = {
    name: '',
    code: '',
    credits: 3.0,
    departmentId: '',
    description: '',
  }
}

function closeAddCourseModal() {
  showAddCourseModal.value = false
  createCourseError.value = null
}

async function submitNewCourse() {
  createCourseLoading.value = true
  createCourseError.value = null

  try {
    if (!newCourse.value.name || !newCourse.value.code || !newCourse.value.departmentId) {
      createCourseError.value = 'Please fill in all required fields'
      createCourseLoading.value = false
      return
    }

    await createAdminCourse({
      name: newCourse.value.name,
      code: newCourse.value.code,
      credits: Number(newCourse.value.credits),
      departmentId: Number(newCourse.value.departmentId),
      description: newCourse.value.description,
    })

    closeAddCourseModal()
    fetchCourses()
    resultMessage.value = 'Course created successfully'
    resultSuccess.value = true
    showResultModal.value = true
  } catch (e: unknown) {
    if (e && typeof e === 'object' && 'message' in e) {
      createCourseError.value = String((e as { message?: unknown }).message)
    } else {
      createCourseError.value = 'Failed to create course'
    }
  } finally {
    createCourseLoading.value = false
  }
}

function clearFilters() {
  searchQuery.value = ''
  statusFilter.value = ''
  selectedDepartment.value = 'all'
}
</script>

<template>
  <div class="max-w-[1200px] w-full mx-auto p-6 md:p-10 flex flex-col gap-8">
    <!-- Header -->
    <div
      class="flex flex-col md:flex-row md:items-end justify-between gap-4 pb-4 border-b border-stone-200 dark:border-stone-800"
    >
      <div>
        <h1 class="text-slate-900 dark:text-white text-3xl font-bold leading-tight tracking-tight">
          Course Management
        </h1>
        <p class="text-slate-500 dark:text-slate-400 mt-2 text-sm">
          Manage curriculum, subjects, and credit allocations.
        </p>
      </div>
      <div class="flex items-center gap-3">
        <button
          class="flex items-center gap-2 rounded-lg h-10 px-4 bg-primary hover:bg-primary-dark text-white text-sm font-bold shadow-md shadow-orange-500/20 transition-all active:scale-95"
          @click="handleAddCourse"
        >
          <span class="material-symbols-outlined text-[20px]">add</span>
          <span>Add New Course</span>
        </button>
      </div>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div
        class="flex flex-col justify-between gap-4 rounded-xl p-6 bg-surface-light dark:bg-surface-dark border border-stone-200 dark:border-stone-800 shadow-sm hover:shadow-md transition-shadow group"
      >
        <div class="flex justify-between items-start">
          <div class="p-2 rounded-lg bg-orange-100 dark:bg-orange-900/20 text-primary">
            <span class="material-symbols-outlined">library_books</span>
          </div>
        </div>
        <div>
          <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">Total Courses</p>
          <p class="text-slate-900 dark:text-white text-3xl font-bold mt-1">
            {{ totalCourses }}
          </p>
        </div>
      </div>

      <div
        class="flex flex-col justify-between gap-4 rounded-xl p-6 bg-surface-light dark:bg-surface-dark border border-stone-200 dark:border-stone-800 shadow-sm hover:shadow-md transition-shadow"
      >
        <div class="flex justify-between items-start">
          <div class="p-2 rounded-lg bg-blue-100 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400">
            <span class="material-symbols-outlined">grade</span>
          </div>
        </div>
        <div>
          <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">Total Credits</p>
          <p class="text-slate-900 dark:text-white text-3xl font-bold mt-1">
            {{ totalCredits.toFixed(1) }}
          </p>
        </div>
      </div>

      <div
        class="flex flex-col justify-between gap-4 rounded-xl p-6 bg-surface-light dark:bg-surface-dark border border-stone-200 dark:border-stone-800 shadow-sm hover:shadow-md transition-shadow"
      >
        <div class="flex justify-between items-start">
          <div class="p-2 rounded-lg bg-purple-100 dark:bg-purple-900/20 text-purple-600 dark:text-purple-400">
            <span class="material-symbols-outlined">domain</span>
          </div>
        </div>
        <div>
          <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">Active Departments</p>
          <p class="text-slate-900 dark:text-white text-3xl font-bold mt-1">
            {{ activeDepartments }}
          </p>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div
      class="flex flex-col md:flex-row justify-between gap-4 bg-surface-light dark:bg-surface-dark p-4 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm"
    >
      <div class="relative w-full md:w-96">
        <span class="absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
          <span class="material-symbols-outlined text-[20px]">search</span>
        </span>
        <input
          v-model="searchQuery"
          class="w-full pl-10 pr-4 py-2 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary/50 text-sm"
          placeholder="Search courses..."
          type="text"
        />
      </div>

      <div class="flex flex-col md:flex-row gap-4 w-full md:w-auto">
        <div class="w-full md:w-48">
          <select
            v-model="statusFilter"
            class="w-full px-4 py-2 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary/50 text-sm cursor-pointer"
          >
            <option value="">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </div>
        <div class="w-full md:w-64">
          <select
            v-model="selectedDepartment"
            class="w-full px-4 py-2 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary/50 text-sm cursor-pointer"
          >
            <option disabled value="">Filter by Department</option>
            <option value="all">All Departments</option>
            <option v-for="dept in departments" :key="dept.departmentId" :value="dept.departmentId">
              {{ dept.name }}
            </option>
          </select>
        </div>
        <div class="w-full md:w-48">
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

    <!-- Table -->
    <div
      class="@container w-full overflow-hidden rounded-xl border border-stone-200 dark:border-stone-800 bg-surface-light dark:bg-surface-dark shadow-sm"
    >
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-stone-50 dark:bg-stone-900/50 border-b border-stone-200 dark:border-stone-800">
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Course Code
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Course Name
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Department
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Credits
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
              v-for="course in courses"
              :key="course.courseId"
              class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors"
            >
              <td class="p-4 text-sm font-medium text-slate-600 dark:text-slate-300 whitespace-nowrap">
                {{ course.code }}
              </td>
              <td class="p-4 text-sm font-bold text-slate-900 dark:text-white whitespace-nowrap">
                {{ course.name }}
              </td>
              <td class="p-4 text-sm text-slate-600 dark:text-slate-400">
                {{ course.departmentName }}
              </td>
              <td class="p-4 text-sm font-medium text-slate-900 dark:text-white">
                {{ course.credits.toFixed(1) }}
              </td>
              <td class="p-4">
                <div class="flex items-center gap-2">
                  <span
                    v-if="course.status === 'ACTIVE'"
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
                      course.status === 'ACTIVE'
                        ? 'text-green-500 hover:text-green-700 hover:bg-green-50 dark:hover:bg-green-900/20'
                        : 'text-slate-400 hover:text-slate-600 hover:bg-stone-100 dark:hover:bg-stone-800',
                    ]"
                    :title="course.status === 'ACTIVE' ? 'Deactivate course' : 'Activate course'"
                    @click="toggleCourseStatus(course)"
                  >
                    <span class="material-symbols-outlined text-[20px]">
                      {{ course.status === 'ACTIVE' ? 'toggle_on' : 'toggle_off' }}
                    </span>
                  </button>
                </div>
              </td>
              <td class="p-4 text-right">
                <div class="flex items-center justify-end gap-2">
                  <button
                    class="p-1 rounded-md text-slate-400 hover:text-primary hover:bg-orange-50 dark:hover:bg-orange-900/20 transition-colors"
                    @click="router.push({ name: 'admin-course-detail', params: { courseId: course.courseId } })"
                    title="View course"
                  >
                    <span class="material-symbols-outlined text-[20px]">visibility</span>
                  </button>

                  <button
                    class="p-1 rounded-md text-slate-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                    @click="() => {}"
                    title="Delete course"
                  >
                    <span class="material-symbols-outlined text-[20px]">delete</span>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="courses.length === 0 && !isLoading">
              <td colspan="6" class="p-6 text-center text-sm text-slate-500 dark:text-slate-400">
                No courses found
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Simple footer (static for now) -->
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
            <option :value="5">5</option>
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
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

    <!-- Add Course Modal -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showAddCourseModal" class="fixed inset-0 z-[120] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div class="bg-surface-light dark:bg-surface-dark w-full max-w-xl rounded-xl shadow-2xl border border-stone-200 dark:border-stone-800 flex flex-col max-h-[90vh]">
            <div class="p-6 border-b border-stone-100 dark:border-stone-800 flex items-center justify-between">
              <h2 class="text-xl font-bold text-slate-900 dark:text-white">Add New Course</h2>
              <button class="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition-colors" @click="closeAddCourseModal">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
            <div class="p-6 overflow-y-auto">
              <form class="grid grid-cols-1 md:grid-cols-2 gap-6" @submit.prevent="submitNewCourse">
                
                <div v-if="createCourseError" class="md:col-span-2 p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 text-sm">
                  {{ createCourseError }}
                </div>

                <div class="md:col-span-2">
                  <label class="block text-sm font-semibold text-slate-700 dark:text-slate-300 mb-1.5" for="course-name">Course Name <span class="text-red-500">*</span></label>
                  <input v-model="newCourse.name" required class="w-full px-4 py-2.5 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary text-sm transition-all shadow-sm" id="course-name" placeholder="e.g. Advanced Data Structures" type="text"/>
                </div>
                <div>
                  <label class="block text-sm font-semibold text-slate-700 dark:text-slate-300 mb-1.5" for="course-code">Course Code <span class="text-red-500">*</span></label>
                  <input v-model="newCourse.code" required class="w-full px-4 py-2.5 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary text-sm transition-all shadow-sm" id="course-code" placeholder="e.g. CS302" type="text"/>
                </div>
                <div>
                  <label class="block text-sm font-semibold text-slate-700 dark:text-slate-300 mb-1.5" for="credits">Credits</label>
                  <input v-model="newCourse.credits" required class="w-full px-4 py-2.5 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary text-sm transition-all shadow-sm" id="credits" placeholder="3.0" step="0.5" type="number"/>
                </div>
                <div class="md:col-span-2">
                  <label class="block text-sm font-semibold text-slate-700 dark:text-slate-300 mb-1.5" for="department">Department <span class="text-red-500">*</span></label>
                  <div class="relative">
                    <select v-model="newCourse.departmentId" required class="w-full px-4 py-2.5 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary text-sm transition-all shadow-sm appearance-none cursor-pointer" id="department">
                      <option disabled value="">Select Department</option>
                      <option v-for="dept in departments" :key="dept.departmentId" :value="dept.departmentId">{{ dept.name }}</option>
                    </select>
                    <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-slate-400">expand_more</span>
                  </div>
                </div>
                <div class="md:col-span-2">
                  <label class="block text-sm font-semibold text-slate-700 dark:text-slate-300 mb-1.5" for="description">Description</label>
                  <textarea v-model="newCourse.description" class="w-full px-4 py-2.5 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary text-sm transition-all shadow-sm resize-none" id="description" placeholder="Enter course description and learning objectives..." rows="4"></textarea>
                </div>
              </form>
            </div>
            <div class="p-6 border-t border-stone-100 dark:border-stone-800 flex items-center justify-end gap-3 bg-stone-50/50 dark:bg-stone-900/20 rounded-b-xl">
              <button @click="closeAddCourseModal" class="px-5 py-2.5 rounded-lg text-sm font-bold text-slate-600 dark:text-slate-400 hover:bg-stone-100 dark:hover:bg-stone-800 transition-colors">
                  Cancel
              </button>
              <button @click="submitNewCourse" :disabled="createCourseLoading" class="px-5 py-2.5 rounded-lg bg-primary hover:bg-primary-dark text-white text-sm font-bold shadow-md shadow-orange-500/20 transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed">
                  {{ createCourseLoading ? 'Creating...' : 'Create Course' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Result Modal -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showResultModal"
          class="fixed inset-0 z-[120] flex items-center justify-center p-4 backdrop-blur-md bg-stone-900/40"
        >
          <div
            class="bg-surface-light dark:bg-surface-dark w-full max-w-md rounded-xl shadow-2xl overflow-hidden border border-stone-200 dark:border-stone-800 max-h-[80vh] flex flex-col"
          >
            <div
              :class="[
                'px-6 py-4 flex items-center justify-between shrink-0',
                resultSuccess ? 'bg-emerald-600' : 'bg-red-600',
              ]"
            >
              <h2 class="text-white text-lg font-bold flex items-center gap-2">
                <span class="material-symbols-outlined">
                  {{ resultSuccess ? 'check_circle' : 'error' }}
                </span>
                {{ resultSuccess ? 'Success' : 'Error' }}
              </h2>
              <button class="text-white/80 hover:text-white transition-colors" type="button" @click="closeResultModal">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>

            <div class="p-6 flex-1 flex items-center">
              <p class="text-sm text-slate-700 dark:text-slate-200">
                {{ resultMessage }}
              </p>
            </div>

            <div class="px-6 py-4 border-t border-stone-200 dark:border-stone-800 bg-stone-50/60 dark:bg-stone-900/40 shrink-0 flex justify-end">
              <button
                type="button"
                class="px-5 py-2 rounded-lg bg-primary hover:bg-primary-dark text-white text-sm font-bold shadow-md shadow-primary/30 transition-all active:scale-95"
                @click="closeResultModal"
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
</style>




