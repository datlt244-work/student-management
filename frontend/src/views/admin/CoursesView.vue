<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { watchDebounced } from '@vueuse/core'
import {
  getAdminCourses,
  getAdminDepartments,
  updateAdminCourseStatus,
  createAdminCourse,
  getAdminCourse,
  updateAdminCourse,
  deleteAdminCourse,
  type AdminCourseListItem,
  type AdminDepartmentItem,
  type AdminUpdateCourseRequest,
} from '@/services/adminUserService'
import { useToast } from '@/composables/useToast'

const { toast, showToast } = useToast()

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

// Edit Course State
const showEditCourseModal = ref(false)
const editCourseLoading = ref(false)
const editCourseSubmitting = ref(false)
const editCourseError = ref<string | null>(null)
const editingCourseId = ref<number | null>(null)
const editingCourseData = ref<AdminUpdateCourseRequest>({
  name: '',
  code: '',
  credits: 0,
  departmentId: 0,
  description: '',
})
const editingCourseCode = ref('')

async function openEditModal(courseId: number) {
  editingCourseId.value = courseId
  showEditCourseModal.value = true
  editCourseLoading.value = true
  editCourseError.value = null

  try {
    const detail = await getAdminCourse(courseId)
    editingCourseCode.value = detail.code
    editingCourseData.value = {
      name: detail.name,
      code: detail.code,
      credits: detail.credits,
      departmentId: detail.departmentId ?? 0,
      description: detail.description || '',
    }
  } catch (e: unknown) {
    if (e && typeof e === 'object' && 'message' in e) {
      editCourseError.value = String((e as { message?: unknown }).message)
    } else {
      editCourseError.value = 'Failed to load course details'
    }
  } finally {
    editCourseLoading.value = false
  }
}

async function submitEditCourse() {
  if (!editingCourseId.value) return

  try {
    editCourseSubmitting.value = true
    editCourseError.value = null
    await updateAdminCourse(editingCourseId.value, editingCourseData.value)
    showEditCourseModal.value = false
    fetchCourses()
    showToast('Course updated successfully.')
  } catch (e: unknown) {
    if (e && typeof e === 'object' && 'message' in e) {
      editCourseError.value = String((e as { message?: unknown }).message)
    } else {
      editCourseError.value = 'Failed to update course'
    }
  } finally {
    editCourseSubmitting.value = false
  }
}

const newCourse = ref({
  name: '',
  code: '',
  credits: 3.0,
  departmentId: '' as string | number,
  description: '',
})

// Delete Course Modal State
const showDeleteConfirmModal = ref(false)
const deletingCourse = ref<AdminCourseListItem | null>(null)
const deleteCourseLoading = ref(false)

function handleDeleteCourse(course: AdminCourseListItem) {
  deletingCourse.value = course
  showDeleteConfirmModal.value = true
}

function closeDeleteConfirmModal() {
  showDeleteConfirmModal.value = false
  deletingCourse.value = null
}

async function confirmDeleteCourse() {
  if (!deletingCourse.value) return

  deleteCourseLoading.value = true
  try {
    await deleteAdminCourse(deletingCourse.value.courseId)
    closeDeleteConfirmModal()
    fetchCourses()
    showToast('Course deleted successfully.')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : 'Failed to delete course'
    showToast(msg, 'error')
    closeDeleteConfirmModal()
  } finally {
    deleteCourseLoading.value = false
  }
}

// Pagination

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
    const msg =
      e && typeof e === 'object' && 'message' in e
        ? String((e as { message?: unknown }).message)
        : 'Failed to update status'
    showToast(msg, 'error')
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
    showToast('Course created successfully.')
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
          <div
            class="p-2 rounded-lg bg-blue-100 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400"
          >
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
          <div
            class="p-2 rounded-lg bg-purple-100 dark:bg-purple-900/20 text-purple-600 dark:text-purple-400"
          >
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
            <tr
              class="bg-stone-50 dark:bg-stone-900/50 border-b border-stone-200 dark:border-stone-800"
            >
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
              <td
                class="p-4 text-sm font-medium text-slate-600 dark:text-slate-300 whitespace-nowrap"
              >
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
                    @click="openEditModal(course.courseId)"
                    title="Edit course"
                  >
                    <span class="material-symbols-outlined text-[20px]">edit</span>
                  </button>
                  <button
                    class="p-1 rounded-md text-slate-400 hover:text-primary hover:bg-orange-50 dark:hover:bg-orange-900/20 transition-colors"
                    @click="
                      router.push({
                        name: 'admin-course-detail',
                        params: { courseId: course.courseId },
                      })
                    "
                    title="View details"
                  >
                    <span class="material-symbols-outlined text-[20px]">visibility</span>
                  </button>

                  <button
                    class="p-1 rounded-md text-slate-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                    @click="handleDeleteCourse(course)"
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

    <!-- Add Course Modal -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showAddCourseModal"
          class="fixed inset-0 z-[120] flex items-center justify-center p-4"
        >
          <div
            class="absolute inset-0 bg-black/50 backdrop-blur-sm"
            @click="closeAddCourseModal"
          ></div>
          <div
            class="relative bg-white dark:bg-surface-dark w-full max-w-xl rounded-2xl shadow-2xl flex flex-col max-h-[90vh] p-6"
          >
            <!-- Header -->
            <div class="flex items-center justify-between mb-5">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-xl bg-orange-100 dark:bg-orange-900/20 text-primary">
                  <span class="material-symbols-outlined">menu_book</span>
                </div>
                <h2 class="text-xl font-bold text-slate-900 dark:text-white">Add New Course</h2>
              </div>
              <button
                class="p-1.5 rounded-lg hover:bg-stone-100 dark:hover:bg-stone-800 text-slate-400 transition-colors"
                @click="closeAddCourseModal"
              >
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
            <!-- Form body -->
            <div class="overflow-y-auto flex-1">
              <form class="grid grid-cols-1 md:grid-cols-2 gap-5" @submit.prevent="submitNewCourse">
                <p
                  v-if="createCourseError"
                  class="md:col-span-2 text-sm text-red-500 flex items-center gap-1.5"
                >
                  <span class="material-symbols-outlined text-[16px]">error</span>
                  {{ createCourseError }}
                </p>
                <div class="md:col-span-2 flex flex-col gap-1.5">
                  <label
                    class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                    for="course-name"
                    >Course Name <span class="text-red-500">*</span></label
                  >
                  <input
                    v-model="newCourse.name"
                    required
                    class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all"
                    id="course-name"
                    placeholder="e.g. Advanced Data Structures"
                    type="text"
                  />
                </div>
                <div class="flex flex-col gap-1.5">
                  <label
                    class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                    for="course-code"
                    >Course Code <span class="text-red-500">*</span></label
                  >
                  <input
                    v-model="newCourse.code"
                    required
                    class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all"
                    id="course-code"
                    placeholder="e.g. CS302"
                    type="text"
                  />
                </div>
                <div class="flex flex-col gap-1.5">
                  <label
                    class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                    for="credits"
                    >Credits</label
                  >
                  <input
                    v-model="newCourse.credits"
                    required
                    class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all"
                    id="credits"
                    placeholder="3"
                    step="1"
                    type="number"
                  />
                </div>
                <div class="md:col-span-2 flex flex-col gap-1.5">
                  <label
                    class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                    for="department"
                    >Department <span class="text-red-500">*</span></label
                  >
                  <div class="relative">
                    <select
                      v-model="newCourse.departmentId"
                      required
                      class="w-full py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all appearance-none cursor-pointer"
                      id="department"
                    >
                      <option disabled value="">Select Department</option>
                      <option
                        v-for="dept in departments"
                        :key="dept.departmentId"
                        :value="dept.departmentId"
                      >
                        {{ dept.name }}
                      </option>
                    </select>
                    <span
                      class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-slate-400"
                      >expand_more</span
                    >
                  </div>
                </div>
                <div class="md:col-span-2 flex flex-col gap-1.5">
                  <label
                    class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                    for="description"
                    >Description</label
                  >
                  <textarea
                    v-model="newCourse.description"
                    class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all resize-none"
                    id="description"
                    placeholder="Enter course description and learning objectives..."
                    rows="3"
                  ></textarea>
                </div>
              </form>
            </div>
            <!-- Footer -->
            <div
              class="flex items-center justify-end gap-3 pt-5 mt-3 border-t border-stone-100 dark:border-stone-800"
            >
              <button
                @click="closeAddCourseModal"
                class="px-4 py-2 text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-stone-100 dark:hover:bg-stone-800 rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button
                @click="submitNewCourse"
                :disabled="createCourseLoading"
                class="flex items-center gap-2 px-5 py-2 bg-primary hover:bg-primary-dark disabled:opacity-60 text-white text-sm font-bold rounded-xl transition-all active:scale-95"
              >
                <span
                  v-if="createCourseLoading"
                  class="material-symbols-outlined text-[18px] animate-spin"
                  >progress_activity</span
                >
                Create Course
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Edit Course Modal -->
    <Teleport to="body">
      <div
        v-if="showEditCourseModal"
        class="fixed inset-0 z-50 flex items-center justify-center px-4"
      >
        <div
          class="absolute inset-0 bg-black/50 backdrop-blur-sm"
          @click="showEditCourseModal = false"
        ></div>
        <div
          class="relative bg-white dark:bg-surface-dark w-full max-w-2xl rounded-2xl shadow-2xl flex flex-col max-h-[90vh] p-6"
        >
          <!-- Header -->
          <div class="flex items-center justify-between mb-5">
            <div class="flex items-center gap-3">
              <div class="p-2 rounded-xl bg-orange-100 dark:bg-orange-900/20 text-primary">
                <span class="material-symbols-outlined">edit_note</span>
              </div>
              <h2 class="text-xl font-bold text-slate-900 dark:text-white">Edit Course</h2>
            </div>
            <button
              @click="showEditCourseModal = false"
              class="p-1.5 rounded-lg hover:bg-stone-100 dark:hover:bg-stone-800 text-slate-400 transition-colors"
            >
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div v-if="editCourseLoading" class="py-12 flex justify-center">
            <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-primary"></div>
          </div>

          <form
            v-else
            @submit.prevent="submitEditCourse"
            class="flex flex-col flex-1 overflow-hidden gap-5"
          >
            <div class="overflow-y-auto flex-1">
              <p v-if="editCourseError" class="mb-4 text-sm text-red-500 flex items-center gap-1.5">
                <span class="material-symbols-outlined text-[16px]">error</span>
                {{ editCourseError }}
              </p>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div class="md:col-span-2 flex flex-col gap-1.5">
                  <label
                    class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                    for="edit-course-name"
                    >Course Name</label
                  >
                  <input
                    v-model="editingCourseData.name"
                    class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all"
                    id="edit-course-name"
                    type="text"
                    required
                  />
                </div>
                <div class="flex flex-col gap-1.5">
                  <label
                    class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                    for="edit-course-code"
                    >Course Code</label
                  >
                  <input
                    v-model="editingCourseData.code"
                    class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all"
                    id="edit-course-code"
                    type="text"
                    required
                  />
                </div>
                <div class="flex flex-col gap-1.5">
                  <label
                    class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                    for="edit-credits"
                    >Credits</label
                  >
                  <input
                    v-model.number="editingCourseData.credits"
                    class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all"
                    id="edit-credits"
                    step="0.5"
                    type="number"
                    required
                  />
                </div>
                <div class="md:col-span-2 flex flex-col gap-1.5">
                  <label
                    class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                    for="edit-department"
                    >Department</label
                  >
                  <select
                    v-model.number="editingCourseData.departmentId"
                    class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all appearance-none cursor-pointer"
                    id="edit-department"
                    required
                  >
                    <option
                      v-for="dept in departments"
                      :key="dept.departmentId"
                      :value="dept.departmentId"
                    >
                      {{ dept.name }}
                    </option>
                  </select>
                </div>
                <div class="md:col-span-2 flex flex-col gap-1.5">
                  <label
                    class="text-xs font-semibold text-slate-600 dark:text-slate-300"
                    for="edit-description"
                    >Description</label
                  >
                  <textarea
                    v-model="editingCourseData.description"
                    class="py-2.5 px-3 bg-stone-50 dark:bg-stone-800 border-stone-200 dark:border-stone-700 rounded-xl text-sm focus:ring-primary focus:border-primary transition-all resize-none"
                    id="edit-description"
                    rows="4"
                  ></textarea>
                </div>
              </div>
            </div>
            <div
              class="border-t border-stone-100 dark:border-stone-800 pt-4 flex items-center justify-end gap-3"
            >
              <button
                type="button"
                @click="showEditCourseModal = false"
                class="px-4 py-2 text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-stone-100 dark:hover:bg-stone-800 rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                :disabled="editCourseSubmitting"
                class="flex items-center gap-2 px-5 py-2 bg-primary hover:bg-primary-dark disabled:opacity-60 text-white text-sm font-bold rounded-xl transition-all active:scale-95"
              >
                <span
                  v-if="editCourseSubmitting"
                  class="material-symbols-outlined text-[18px] animate-spin"
                  >progress_activity</span
                >
                Save Changes
              </button>
            </div>
          </form>
        </div>
      </div>
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
                <h2 class="text-lg font-bold text-slate-900 dark:text-white">Delete Course</h2>
                <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">
                  Are you sure you want to delete
                  <span class="font-semibold text-slate-700 dark:text-slate-200">{{
                    deletingCourse?.name
                  }}</span
                  >? This action cannot be undone.
                </p>
              </div>
            </div>
            <div class="flex gap-3">
              <button
                type="button"
                class="flex-1 px-4 py-2.5 text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-stone-100 dark:hover:bg-stone-800 border border-stone-200 dark:border-stone-700 rounded-xl transition-colors"
                :disabled="deleteCourseLoading"
                @click="closeDeleteConfirmModal"
              >
                Cancel
              </button>
              <button
                type="button"
                :disabled="deleteCourseLoading"
                class="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 bg-red-500 hover:bg-red-600 disabled:opacity-60 text-white text-sm font-bold rounded-xl transition-all active:scale-95"
                @click="confirmDeleteCourse"
              >
                <span
                  v-if="deleteCourseLoading"
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
</style>

