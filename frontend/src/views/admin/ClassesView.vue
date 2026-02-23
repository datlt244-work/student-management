<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import {
  getAdminClasses,
  createAdminClass,
  type AdminClassListItem,
} from '@/services/adminClassService'
import {
  getAdminSemesterList,
  getAdminCourses,
  getAdminTeachersByDepartment,
  type AdminSemesterListItem,
  type TeacherSimpleResponse,
} from '@/services/adminUserService'
import { useToast } from '@/composables/useToast'

const { toast, showToast } = useToast()

const stats = [
  { label: 'Total Classes', value: '124', icon: 'class', color: 'blue' },
  { label: 'Ongoing Classes', value: '8', icon: 'timelapse', color: 'orange' },
  { label: 'Active Students', value: '3,852', icon: 'groups', color: 'green' },
]

const loading = ref(false)
const classes = ref<AdminClassListItem[]>([])
const semesters = ref<AdminSemesterListItem[]>([])
const statuses = ['OPEN', 'CLOSED', 'CANCELLED']

const searchQuery = ref('')
const filterStatus = ref('')
const filterSemester = ref<number | ''>('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)

// Modal state
const showAddClassModal = ref(false)
const showEditClassModal = ref(false)
const createLoading = ref(false)
const createError = ref<string | null>(null)

const newClass = ref({
  courseId: '' as string | number,
  teacherId: '',
  semesterId: '' as string | number,
  roomNumber: '',
  dayOfWeek: 1,
  startTime: '08:00',
  endTime: '10:00',
  maxStudents: 40,
})

const days = [
  { value: 1, label: 'Monday' },
  { value: 2, label: 'Tuesday' },
  { value: 3, label: 'Wednesday' },
  { value: 4, label: 'Thursday' },
  { value: 5, label: 'Friday' },
  { value: 6, label: 'Saturday' },
  { value: 7, label: 'Sunday' },
]

const courses = ref<any[]>([])
const teachers = ref<TeacherSimpleResponse[]>([])
const loadingTeachers = ref(false)

const editingClass = ref<AdminClassListItem | null>(null)

// Teachers will be loaded dynamically based on course department

async function fetchClasses() {
  try {
    loading.value = true
    const response = await getAdminClasses({
      search: searchQuery.value,
      status: filterStatus.value || undefined,
      semesterId: filterSemester.value || undefined,
      page: currentPage.value - 1,
      size: pageSize.value,
      sort: 'createdAt,desc',
    })
    classes.value = response.content
    totalElements.value = response.totalElements
    totalPages.value = response.totalPages
  } catch (err) {
    console.error('Failed to fetch classes:', err)
  } finally {
    loading.value = false
  }
}

async function loadSemesters() {
  try {
    const res = await getAdminSemesterList({ size: 100 })
    semesters.value = res.content
    // Set default semester for new class to current semester if available
    const current = semesters.value.find((s) => s.isCurrent)
    if (current && !newClass.value.semesterId) {
      newClass.value.semesterId = current.semesterId
    }
  } catch (err) {
    console.error('Failed to load semesters:', err)
  }
}

async function loadCourses() {
  try {
    const res = await getAdminCourses({ size: 200, status: 'ACTIVE' })
    courses.value = res.content
  } catch (err) {
    console.error('Failed to load courses:', err)
  }
}

async function handleCourseChange() {
  const selectedCourse = courses.value.find((c) => c.courseId === newClass.value.courseId)
  newClass.value.teacherId = ''
  teachers.value = []

  if (selectedCourse && selectedCourse.departmentId) {
    try {
      loadingTeachers.value = true
      teachers.value = await getAdminTeachersByDepartment(selectedCourse.departmentId)
    } catch (err) {
      console.error('Failed to load teachers:', err)
      showToast('Failed to load teachers for this department', 'error')
    } finally {
      loadingTeachers.value = false
    }
  }
}

onMounted(() => {
  fetchClasses()
  loadSemesters()
  loadCourses()
})

watch([searchQuery, filterStatus, filterSemester, pageSize], () => {
  currentPage.value = 1
  fetchClasses()
})

watch(currentPage, () => {
  fetchClasses()
})

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
  if (
    page >= 1 &&
    (totalPages.value === 0 || page <= totalPages.value) &&
    page !== currentPage.value
  ) {
    currentPage.value = page
    fetchClasses()
  }
}

function handleAddClass() {
  showAddClassModal.value = true
  createError.value = null

  const currentSemester = semesters.value.find((s) => s.isCurrent)

  newClass.value = {
    courseId: '',
    teacherId: '',
    semesterId: currentSemester ? currentSemester.semesterId : '',
    roomNumber: '',
    dayOfWeek: 1,
    startTime: '08:00',
    endTime: '10:00',
    maxStudents: 40,
  }
  teachers.value = []
}

function handleEditClass(cls: AdminClassListItem) {
  // Not fully implemented in this step, but setting basic fields
  editingClass.value = { ...cls }
  // We'd need to fetch the course/teacher details to map correctly
  // For now, focusing on Add Class
  showEditClassModal.value = true
  createError.value = null
}

function clearFilters() {
  searchQuery.value = ''
  filterStatus.value = ''
  filterSemester.value = ''
}

function closeAddClassModal() {
  showAddClassModal.value = false
  createError.value = null
}

function closeEditClassModal() {
  showEditClassModal.value = false
  editingClass.value = null
  createError.value = null
}

async function submitNewClass() {
  try {
    createLoading.value = true
    createError.value = null

    await createAdminClass({
      courseId: Number(newClass.value.courseId),
      teacherId: newClass.value.teacherId,
      semesterId: Number(newClass.value.semesterId),
      roomNumber: newClass.value.roomNumber,
      dayOfWeek: newClass.value.dayOfWeek,
      startTime: newClass.value.startTime,
      endTime: newClass.value.endTime,
      maxStudents: newClass.value.maxStudents,
    })

    showToast('Class created successfully')
    closeAddClassModal()
    fetchClasses()
  } catch (err: any) {
    createError.value = err.message || 'Failed to create class'
  } finally {
    createLoading.value = false
  }
}

async function submitEditClass() {
  // Logic remains similar but will call update API in next step
  console.log('Submit edit class:', newClass.value)
  closeEditClassModal()
}
</script>

<template>
  <div class="max-w-[1400px] w-full mx-auto p-6 md:p-10 flex flex-col gap-8">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-slate-900 dark:text-white text-3xl font-bold leading-tight tracking-tight">
          Class Management
        </h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">
          Organize and manage physical and virtual class schedules.
        </p>
      </div>
      <div class="flex items-center gap-3">
        <button
          @click="handleAddClass"
          class="flex items-center gap-2 px-6 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95"
        >
          <span class="material-symbols-outlined text-[20px]">add_circle</span>
          Add New Class
        </button>
      </div>
    </div>

    <!-- Stats Grid -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div
        class="bg-surface-light dark:bg-surface-dark p-5 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex items-center gap-4"
      >
        <div class="p-3 rounded-lg bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400">
          <span class="material-symbols-outlined text-2xl">class</span>
        </div>
        <div>
          <p class="text-sm font-medium text-slate-500 dark:text-slate-400">Total Classes</p>
          <h3 class="text-2xl font-bold text-slate-900 dark:text-white">
            {{ loading ? '...' : totalElements }}
          </h3>
        </div>
      </div>
      <div
        class="bg-surface-light dark:bg-surface-dark p-5 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex items-center gap-4"
      >
        <div
          class="p-3 rounded-lg bg-orange-50 dark:bg-orange-900/20 text-orange-600 dark:text-orange-400"
        >
          <span class="material-symbols-outlined text-2xl">timelapse</span>
        </div>
        <div>
          <p class="text-sm font-medium text-slate-500 dark:text-slate-400">Ongoing Classes</p>
          <h3 class="text-2xl font-bold text-slate-900 dark:text-white">8</h3>
        </div>
      </div>
      <div
        class="bg-surface-light dark:bg-surface-dark p-5 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex items-center gap-4"
      >
        <div
          class="p-3 rounded-lg bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400"
        >
          <span class="material-symbols-outlined text-2xl">groups</span>
        </div>
        <div>
          <p class="text-sm font-medium text-slate-500 dark:text-slate-400">Active Students</p>
          <h3 class="text-2xl font-bold text-slate-900 dark:text-white">3,852</h3>
        </div>
      </div>
    </div>

    <!-- Filters Bar -->
    <div
      class="bg-surface-light dark:bg-surface-dark p-4 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex flex-col lg:flex-row gap-4"
    >
      <div class="relative flex-1">
        <span
          class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
          >search</span
        >
        <input
          v-model="searchQuery"
          class="w-full pl-10 pr-4 h-11 bg-white dark:bg-stone-900 border-stone-200 dark:border-stone-700 text-sm rounded-lg focus:ring-primary focus:border-primary transition-all text-slate-900 dark:text-white"
          placeholder="Search by course name, code or teacher..."
          type="text"
        />
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <select
          v-model="filterSemester"
          class="h-11 bg-white dark:bg-stone-900 border-stone-200 dark:border-stone-700 text-sm rounded-lg focus:ring-primary focus:border-primary transition-all text-slate-900 dark:text-white"
        >
          <option value="">All Semesters</option>
          <option v-for="s in semesters" :key="s.semesterId" :value="s.semesterId">
            {{ s.displayName }}
          </option>
        </select>
        <select
          v-model="filterStatus"
          class="h-11 bg-white dark:bg-stone-900 border-stone-200 dark:border-stone-700 text-sm rounded-lg focus:ring-primary focus:border-primary transition-all text-slate-900 dark:text-white"
        >
          <option value="">All Statuses</option>
          <option v-for="st in statuses" :key="st" :value="st">{{ st }}</option>
        </select>
        <button
          @click="clearFilters"
          class="flex items-center justify-center gap-2 h-11 px-4 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-slate-700 dark:text-slate-300 rounded-lg font-semibold transition-all"
        >
          <span class="material-symbols-outlined text-[20px]">filter_list_off</span>
          Clear
        </button>
      </div>
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
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Class ID
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Class Name
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Course
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Teacher
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Schedule
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Room
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Capacity
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap text-right"
              >
                Actions
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-200 dark:divide-stone-800">
            <tr v-if="loading">
              <td colspan="8" class="p-8 text-center">
                <div class="flex flex-col items-center gap-2">
                  <div
                    class="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"
                  ></div>
                  <span class="text-sm text-slate-500 font-medium">Loading classes...</span>
                </div>
              </td>
            </tr>
            <tr v-else-if="classes.length === 0">
              <td colspan="8" class="p-12 text-center">
                <div class="flex flex-col items-center gap-3">
                  <span class="material-symbols-outlined text-5xl text-slate-300">search_off</span>
                  <p class="text-slate-500 font-medium">No classes found matching your criteria.</p>
                </div>
              </td>
            </tr>
            <tr
              v-else
              v-for="cls in classes"
              :key="cls.classId"
              class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors"
            >
              <td class="p-4 text-sm font-medium text-slate-500 whitespace-nowrap">
                #CL-{{ cls.classId }}
              </td>
              <td class="p-4 whitespace-nowrap">
                <span class="text-sm font-bold text-slate-900 dark:text-white leading-none">{{
                  cls.courseCode
                }}</span>
              </td>
              <td class="p-4 whitespace-nowrap">
                <span class="text-sm text-slate-600 dark:text-slate-400">{{ cls.courseName }}</span>
              </td>
              <td class="p-4 whitespace-nowrap">
                <div class="flex items-center gap-2">
                  <div
                    class="w-6 h-6 rounded-full bg-orange-100 dark:bg-orange-900/20 flex items-center justify-center overflow-hidden"
                  >
                    <span class="material-symbols-outlined text-[16px] text-orange-600"
                      >person</span
                    >
                  </div>
                  <span class="text-sm text-slate-700 dark:text-slate-300">{{
                    cls.teacherName
                  }}</span>
                </div>
              </td>
              <td class="p-4 text-sm text-slate-600 dark:text-slate-400 whitespace-nowrap">
                {{ cls.schedule }}
              </td>
              <td class="p-4 text-sm text-slate-600 dark:text-slate-400 whitespace-nowrap">
                {{ cls.roomNumber }}
              </td>
              <td class="p-4">
                <div class="flex items-center gap-2 text-slate-900 dark:text-white">
                  <div
                    class="w-20 bg-stone-200 dark:bg-stone-700 rounded-full h-1.5 overflow-hidden"
                  >
                    <div
                      class="h-1.5 rounded-full"
                      :class="{
                        'bg-red-500': cls.studentCount / cls.maxStudents >= 0.9,
                        'bg-amber-500':
                          cls.studentCount / cls.maxStudents >= 0.7 &&
                          cls.studentCount / cls.maxStudents < 0.9,
                        'bg-primary': cls.studentCount / cls.maxStudents < 0.7,
                      }"
                      :style="{
                        width: Math.min(100, (cls.studentCount / cls.maxStudents) * 100) + '%',
                      }"
                    ></div>
                  </div>
                  <span class="text-xs font-medium text-slate-500 dark:text-slate-400"
                    >{{ cls.studentCount }}/{{ cls.maxStudents }}</span
                  >
                </div>
              </td>
              <td class="p-4 text-right whitespace-nowrap">
                <div class="flex items-center justify-end gap-1">
                  <span
                    class="px-2 py-0.5 rounded text-[10px] font-bold uppercase mr-2"
                    :class="{
                      'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400':
                        cls.status === 'OPEN',
                      'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400':
                        cls.status === 'CANCELLED',
                      'bg-stone-100 text-stone-700 dark:bg-stone-900/30 dark:text-stone-400':
                        cls.status === 'CLOSED',
                    }"
                  >
                    {{ cls.status }}
                  </span>
                  <router-link
                    :to="{
                      name: 'admin-class-detail',
                      params: { classId: cls.classId },
                    }"
                    class="p-2 text-slate-400 hover:text-blue-500 transition-colors"
                    title="View Students"
                  >
                    <span class="material-symbols-outlined text-[20px]">group</span>
                  </router-link>
                  <button
                    @click="handleEditClass(cls)"
                    class="p-2 text-slate-400 hover:text-primary transition-colors"
                    title="Edit"
                  >
                    <span class="material-symbols-outlined text-[20px]">edit</span>
                  </button>
                  <button
                    class="p-2 text-slate-400 hover:text-red-500 transition-colors"
                    title="Delete"
                  >
                    <span class="material-symbols-outlined text-[20px]">delete</span>
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
              :disabled="currentPage <= 1 || loading"
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
              :disabled="currentPage >= totalPages || loading"
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

  <!-- Add/Edit Class Modal (Shared Form) -->
  <Teleport to="body">
    <!-- Toast Notification -->
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

    <Transition name="fade">
      <div
        v-if="showAddClassModal || showEditClassModal"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4"
      >
        <div
          class="absolute inset-0 bg-black/50 backdrop-blur-md"
          @click="showAddClassModal ? closeAddClassModal() : closeEditClassModal()"
        ></div>
        <div
          class="relative bg-white dark:bg-surface-dark w-full max-w-xl rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]"
        >
          <!-- Header -->
          <div
            class="flex items-center justify-between px-6 py-5 border-b border-stone-100 dark:border-stone-800"
          >
            <div class="flex items-center gap-3">
              <div class="p-2 rounded-xl bg-primary/10 text-primary">
                <span class="material-symbols-outlined">{{
                  showEditClassModal ? 'edit' : 'add_circle'
                }}</span>
              </div>
              <h2 class="text-xl font-bold text-slate-900 dark:text-white">
                {{ showEditClassModal ? 'Edit Class' : 'Add New Class' }}
              </h2>
            </div>
            <button
              @click="showAddClassModal ? closeAddClassModal() : closeEditClassModal()"
              class="p-1.5 rounded-lg hover:bg-stone-100 dark:hover:bg-stone-800 text-slate-400 transition-colors"
            >
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <!-- Form Content -->
          <form
            @submit.prevent="showEditClassModal ? submitEditClass() : submitNewClass()"
            class="p-6 overflow-y-auto space-y-5"
          >
            <div
              v-if="createError"
              class="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 text-sm"
            >
              {{ createError }}
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div class="space-y-1.5">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider"
                  >Course <span class="text-red-500">*</span></label
                >
                <select
                  v-model="newClass.courseId"
                  @change="handleCourseChange"
                  required
                  class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
                >
                  <option value="">Select Course</option>
                  <option v-for="c in courses" :key="c.courseId" :value="c.courseId">
                    [{{ c.code }}] {{ c.name }}
                  </option>
                </select>
              </div>
              <div class="space-y-1.5">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider"
                  >Semester</label
                >
                <div
                  class="w-full px-3 py-2 border border-stone-100 dark:border-stone-800 rounded-lg bg-stone-50 dark:bg-stone-900/50 text-sm italic text-slate-500"
                >
                  {{
                    semesters.find((s) => s.semesterId === newClass.semesterId)?.displayName ||
                    'Current'
                  }}
                </div>
              </div>
            </div>

            <div class="space-y-1.5">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider"
                >Teacher <span class="text-red-500">*</span></label
              >
              <select
                v-model="newClass.teacherId"
                required
                :disabled="loadingTeachers || !newClass.courseId"
                class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary disabled:opacity-50"
              >
                <option value="" disabled>
                  {{ loadingTeachers ? 'Loading teachers...' : 'Select Teacher (Required)' }}
                </option>
                <option v-for="t in teachers" :key="t.teacherId" :value="t.teacherId">
                  [{{ t.teacherCode }}] {{ t.fullName }}
                </option>
              </select>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div class="space-y-1.5">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider"
                  >Room <span class="text-red-500">*</span></label
                >
                <input
                  v-model="newClass.roomNumber"
                  required
                  type="text"
                  placeholder="e.g. Rm 304"
                  class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
                />
              </div>
              <div class="space-y-1.5">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider"
                  >Max Students</label
                >
                <input
                  v-model="newClass.maxStudents"
                  type="number"
                  min="1"
                  class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
                />
              </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-3 gap-5">
              <div class="space-y-1.5">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider"
                  >Day <span class="text-red-500">*</span></label
                >
                <select
                  v-model="newClass.dayOfWeek"
                  required
                  class="w-full px-2 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
                >
                  <option v-for="d in days" :key="d.value" :value="d.value">{{ d.label }}</option>
                </select>
              </div>
              <div class="space-y-1.5">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider"
                  >Starts <span class="text-red-500">*</span></label
                >
                <input
                  v-model="newClass.startTime"
                  required
                  type="time"
                  class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
                />
              </div>
              <div class="space-y-1.5">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider"
                  >Ends <span class="text-red-500">*</span></label
                >
                <input
                  v-model="newClass.endTime"
                  required
                  type="time"
                  class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
                />
              </div>
            </div>

            <!-- Footer -->
            <div
              class="pt-4 flex items-center justify-end gap-3 border-t border-stone-100 dark:border-stone-800"
            >
              <button
                type="button"
                @click="showAddClassModal ? closeAddClassModal() : closeEditClassModal()"
                class="px-5 py-2.5 rounded-lg text-sm font-bold text-slate-600 dark:text-slate-400 hover:bg-stone-100 dark:hover:bg-stone-800 transition-all"
              >
                Cancel
              </button>
              <button
                type="submit"
                :disabled="createLoading"
                class="flex items-center gap-2 px-8 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span
                  v-if="createLoading"
                  class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"
                ></span>
                <span>{{
                  createLoading
                    ? showEditClassModal
                      ? 'Updating...'
                      : 'Creating...'
                    : showEditClassModal
                      ? 'Save Changes'
                      : 'Create Class'
                }}</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
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
