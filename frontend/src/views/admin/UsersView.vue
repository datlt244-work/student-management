<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { watchDebounced } from '@vueuse/core'
import { useRouter } from 'vue-router'
import {
  createAdminUser,
  getAdminUsers,
  getAdminDepartments,
  deleteAdminUser,
  type AdminUserListItem,
  type AdminCreateUserRequest,
  type AdminDepartmentItem,
  type UserStatus,
} from '@/services/adminUserService'

const router = useRouter()

// Filters & pagination
const searchQuery = ref('')
const statusFilter = ref<UserStatus | ''>('')
const roleFilter = ref('')
const sortBy = ref<string>('createdAt,desc')
const pageSize = ref(10)
const currentPage = ref(1) // UI 1-based
const totalPages = ref(0)
const totalElements = ref(0)

// Loading & error state
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)

// Server data
const users = ref<AdminUserListItem[]>([])

// Delete user modal state
const showDeleteConfirmModal = ref(false)
const showDeleteResultModal = ref(false)
const deleteTargetUser = ref<AdminUserListItem | null>(null)
const deleteLoading = ref(false)
const deleteError = ref<string | null>(null)
const deleteResultMessage = ref('')
const deleteResultSuccess = ref(false)

async function fetchUsers() {
  try {
    isLoading.value = true
    errorMessage.value = null

    const result = await getAdminUsers({
      page: currentPage.value - 1,
      size: pageSize.value,
      sort: sortBy.value || 'createdAt,desc',
      search: searchQuery.value || undefined,
      status: statusFilter.value,
      roleId: roleFilter.value ? Number(roleFilter.value) : undefined,
    })

    users.value = result.content
    currentPage.value = result.page + 1
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'message' in err) {
       
      errorMessage.value = String((err as { message?: unknown }).message) || 'Failed to load users'
    } else {
      errorMessage.value = 'Failed to load users'
    }
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchUsers()
})

// Debounce tất cả filter/search để tránh gọi API trùng lặp (đặc biệt khi clearFilters)
watchDebounced(
  [searchQuery, statusFilter, roleFilter, sortBy, pageSize],
  () => {
    currentPage.value = 1
    fetchUsers()
  },
  { debounce: 500, maxWait: 1000 },
)

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

// Add New User modal state
const showAddUserModal = ref(false)
const newUserRole = ref<'TEACHER' | 'STUDENT'>('TEACHER')

// Teacher form model
const newTeacher = ref({
  firstName: '',
  lastName: '',
  email: '',
  teacherCode: '',
  phone: '',
  departmentId: null as number | null,
  specialization: '',
  academicRank: '',
  officeRoom: '',
  degreesQualification: '',
})

// Student form model
const newStudent = ref({
  firstName: '',
  lastName: '',
  email: '',
  studentCode: '',
  dob: '',
  gender: '' as '' | 'MALE' | 'FEMALE' | 'OTHER',
  major: '',
  phone: '',
  departmentId: null as number | null,
  address: '',
  year: null as number | null,
  manageClass: '',
})

// Create user submit state
const createUserLoading = ref(false)
const createUserError = ref<string | null>(null)

// Create user result modal state
const showCreateResultModal = ref(false)
const createResultSuccess = ref(false)
const createResultMessage = ref('')

// Departments for select (fetch when opening Add User modal)
const departments = ref<AdminDepartmentItem[]>([])
const departmentsLoading = ref(false)

const filteredUsers = computed(() => {
  return users.value
})

function clearFilters() {
  searchQuery.value = ''
  statusFilter.value = ''
  roleFilter.value = ''
  sortBy.value = 'createdAt,desc'
}

function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value && page !== currentPage.value) {
    currentPage.value = page
    fetchUsers()
  }
}

function mapRoleToId(roleName: string): string {
  if (roleName === 'ADMIN') return '1'
  if (roleName === 'TEACHER') return '2'
  if (roleName === 'STUDENT') return '3'
  return ''
}

function getRolePillClasses(roleName: string): string {
  switch (roleName) {
    case 'ADMIN':
      return 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'
    case 'TEACHER':
      return 'bg-purple-50 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400'
    case 'STUDENT':
      return 'bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400'
    default:
      return 'bg-stone-100 dark:bg-stone-800 text-stone-700 dark:text-stone-300'
  }
}

function getStatusPillClasses(status: UserStatus): { container: string; dot: string } {
  switch (status) {
    case 'ACTIVE':
      return {
        container: 'bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-400 ring-green-600/20',
        dot: 'bg-green-500',
      }
    case 'INACTIVE':
      return {
        container: 'bg-stone-100 dark:bg-stone-800 text-stone-600 dark:text-stone-400 ring-stone-500/20',
        dot: 'bg-stone-400',
      }
    case 'BLOCKED':
      return {
        container: 'bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-400 ring-red-600/20',
        dot: 'bg-red-500',
      }
    case 'PENDING_VERIFICATION':
    default:
      return {
        container: 'bg-yellow-50 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400 ring-yellow-600/20',
        dot: 'bg-yellow-500',
      }
  }
}

function formatCreatedAt(value: string): string {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: '2-digit' })
}

function buildDisplayName(email: string): string {
  const parts = email.split('@')
  const localPart = parts[0] || ''
  return localPart
    .split(/[._]/)
    .map((s) => (s && s.length > 0 ? s[0]!.toUpperCase() + s.slice(1) : ''))
    .join(' ')
}

function handleEdit(user: AdminUserListItem) {
  if (user.role.roleName === 'STUDENT' || user.role.roleName === 'TEACHER') {
    const uid = user.userId
    router.push({ name: 'admin-user-detail', params: { userId: uid } })
  } else {
    // TODO: mở modal/form edit cho admin
    console.log('Edit', user)
  }
}

async function handleDelete(user: AdminUserListItem) {
  deleteTargetUser.value = user
  deleteError.value = null
  deleteLoading.value = false
  showDeleteConfirmModal.value = true
}

async function confirmDeleteUser() {
  if (!deleteTargetUser.value) return
  deleteLoading.value = true
  deleteError.value = null
  try {
    await deleteAdminUser(deleteTargetUser.value.userId)
    await fetchUsers()
    showDeleteConfirmModal.value = false
    deleteResultSuccess.value = true
    deleteResultMessage.value = 'User has been deleted successfully.'
    showDeleteResultModal.value = true
  } catch (e) {
    const msg = e instanceof Error ? e.message : 'Failed to delete user'
    deleteResultSuccess.value = false
    deleteResultMessage.value = msg
    showDeleteConfirmModal.value = false
    showDeleteResultModal.value = true
  } finally {
    deleteLoading.value = false
  }
}

function closeDeleteModals() {
  showDeleteConfirmModal.value = false
  showDeleteResultModal.value = false
  deleteTargetUser.value = null
  deleteError.value = null
  deleteResultMessage.value = ''
}

function closeCreateResultModal() {
  showCreateResultModal.value = false
}

async function handleAddUser() {
  showAddUserModal.value = true
  newUserRole.value = 'TEACHER'
  createUserError.value = null
  departmentsLoading.value = true
  try {
    departments.value = await getAdminDepartments()
  } catch {
    departments.value = []
  } finally {
    departmentsLoading.value = false
  }
  newTeacher.value = {
    firstName: '',
    lastName: '',
    email: '',
    teacherCode: '',
    phone: '',
    departmentId: null,
    specialization: '',
    academicRank: '',
    officeRoom: '',
    degreesQualification: '',
  }
  newStudent.value = {
    firstName: '',
    lastName: '',
    email: '',
    studentCode: '',
    dob: '',
    gender: '',
    major: '',
    phone: '',
    departmentId: null,
    address: '',
    year: null,
    manageClass: '',
  }
}

function closeAddUserModal() {
  showAddUserModal.value = false
  createUserError.value = null
}

async function submitNewUser() {
  createUserError.value = null
  const isTeacher = newUserRole.value === 'TEACHER'

  // Validation: all fields required for the selected role
  if (isTeacher) {
    const t = newTeacher.value
    if (
      !t.firstName?.trim() ||
      !t.lastName?.trim() ||
      !t.email?.trim() ||
      !t.teacherCode?.trim() ||
      t.departmentId == null ||
      t.departmentId < 1 ||
      !t.phone?.trim() ||
      !t.specialization?.trim() ||
      !t.academicRank?.trim() ||
      !t.officeRoom?.trim() ||
      !t.degreesQualification?.trim()
    ) {
      createUserError.value = 'Please fill in all fields.'
      return
    }
  } else {
    const s = newStudent.value
    if (
      !s.firstName?.trim() ||
      !s.lastName?.trim() ||
      !s.email?.trim() ||
      !s.studentCode?.trim() ||
      s.departmentId == null ||
      s.departmentId < 1 ||
      !s.major?.trim() ||
      s.year == null ||
      !s.dob ||
      !s.gender ||
      !s.phone?.trim() ||
      !s.manageClass?.trim() ||
      !s.address?.trim()
    ) {
      createUserError.value = 'Please fill in all fields.'
      return
    }
  }

  const deptId = isTeacher ? newTeacher.value.departmentId : newStudent.value.departmentId
  const payload: AdminCreateUserRequest = isTeacher
    ? {
        role: 'TEACHER',
        email: newTeacher.value.email.trim(),
        departmentId: deptId!,
        firstName: newTeacher.value.firstName.trim(),
        lastName: newTeacher.value.lastName.trim(),
        phone: newTeacher.value.phone?.trim() || undefined,
        teacherCode: newTeacher.value.teacherCode.trim(),
        specialization: newTeacher.value.specialization?.trim() || undefined,
        academicRank: newTeacher.value.academicRank?.trim() || undefined,
        officeRoom: newTeacher.value.officeRoom?.trim() || undefined,
        degreesQualification: newTeacher.value.degreesQualification?.trim() || undefined,
      }
    : {
        role: 'STUDENT',
        email: newStudent.value.email.trim(),
        departmentId: deptId!,
        firstName: newStudent.value.firstName.trim(),
        lastName: newStudent.value.lastName.trim(),
        phone: newStudent.value.phone?.trim() || undefined,
        studentCode: newStudent.value.studentCode.trim(),
        dob: newStudent.value.dob || undefined,
        gender: newStudent.value.gender || undefined,
        major: newStudent.value.major?.trim() || undefined,
        address: newStudent.value.address?.trim() || undefined,
        year: newStudent.value.year ?? undefined,
        manageClass: newStudent.value.manageClass?.trim() || undefined,
      }

  try {
    createUserLoading.value = true
    await createAdminUser(payload)
    closeAddUserModal()
    await fetchUsers()
    createResultSuccess.value = true
    createResultMessage.value = 'User has been created successfully.'
    showCreateResultModal.value = true
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Failed to create user'
    createUserError.value = msg
    createResultSuccess.value = false
    createResultMessage.value = msg
    showCreateResultModal.value = true
  } finally {
    createUserLoading.value = false
  }
}

// Import Excel modal state
const showImportExcelModal = ref(false)
const importFile = ref<File | null>(null)
const importFileInputRef = ref<HTMLInputElement | null>(null)

function handleImportFromExcel() {
  showImportExcelModal.value = true
  importFile.value = null
}

function closeImportExcelModal() {
  showImportExcelModal.value = false
  importFile.value = null
}

function downloadTeacherTemplate() {
  // TODO: tải template Excel cho Teacher
  console.log('Download teacher template')
}

function downloadStudentTemplate() {
  // TODO: tải template Excel cho Student
  console.log('Download student template')
}

function triggerImportFileInput() {
  importFileInputRef.value?.click()
}

function handleImportFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    const ext = file.name.toLowerCase().split('.').pop()
    if (ext === 'xlsx' || ext === 'xls') {
      importFile.value = file
    } else {
      importFile.value = null
      alert('Chỉ chấp nhận file Excel (.xlsx, .xls)')
    }
  }
  target.value = ''
}

function handleImportDrop(event: DragEvent) {
  event.preventDefault()
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    const ext = file.name.toLowerCase().split('.').pop()
    if (ext === 'xlsx' || ext === 'xls') {
      importFile.value = file
    } else {
      importFile.value = null
      alert('Chỉ chấp nhận file Excel (.xlsx, .xls)')
    }
  }
}

function handleImportDragOver(event: DragEvent) {
  event.preventDefault()
}

function processImport() {
  if (!importFile.value) return
  // TODO: gọi API upload và xử lý import
  console.log('Process import', importFile.value.name)
  closeImportExcelModal()
}
</script>

<template>
  <div class="max-w-[1400px] w-full mx-auto p-6 md:p-10 flex flex-col gap-8">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-slate-900 dark:text-white text-3xl font-bold leading-tight tracking-tight">User Management</h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">
          Manage student, teacher, and administrator accounts across the system.
        </p>
      </div>
      <div class="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center">
        <div
          v-if="isLoading"
          class="flex items-center gap-2 px-3 py-1.5 rounded-full bg-stone-100 dark:bg-stone-800 text-xs font-medium text-slate-600 dark:text-slate-300"
        >
          <span class="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin"></span>
          <span>Loading...</span>
        </div>
        <button
          class="flex items-center justify-center gap-2 px-4 py-2.5 bg-surface-light dark:bg-surface-dark border border-stone-200 dark:border-stone-700 text-slate-700 dark:text-slate-200 rounded-lg text-sm font-semibold shadow-sm hover:bg-stone-50 dark:hover:bg-stone-800 transition-all shrink-0"
          type="button"
          @click="handleImportFromExcel"
        >
          <span class="material-symbols-outlined text-[18px]">upload_file</span>
          Import from Excel
        </button>
        <button
          class="flex items-center gap-2 px-6 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95 shrink-0"
          type="button"
          @click="handleAddUser"
        >
          <span class="material-symbols-outlined text-[20px]">person_add</span>
          Add New User
        </button>
      </div>
    </div>

    <!-- Filters -->
    <div
      class="bg-surface-light dark:bg-surface-dark p-4 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex flex-col lg:flex-row gap-4"
    >
      <div class="relative flex-1 min-w-0">
        <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]">search</span>
        <input
          v-model="searchQuery"
          class="w-full pl-10 pr-4 h-11 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
          placeholder="Search by email..."
          type="text"
        />
      </div>
      <div class="flex flex-wrap gap-2 lg:gap-3 items-center justify-end shrink-0">
        <select
          v-model="statusFilter"
          class="h-10 px-2 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-xs md:text-sm focus:ring-primary focus:border-primary w-[110px]"
        >
          <option value="">Status</option>
          <option value="ACTIVE">ACTIVE</option>
          <option value="INACTIVE">INACTIVE</option>
          <option value="BLOCKED">BLOCKED</option>
          <option value="PENDING_VERIFICATION">PENDING_VERIFICATION</option>
        </select>
        <select
          v-model="roleFilter"
          class="h-10 px-2 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-xs md:text-sm focus:ring-primary focus:border-primary w-[100px]"
        >
          <option value="">Role</option>
          <option :value="mapRoleToId('ADMIN')">ADMIN</option>
          <option :value="mapRoleToId('TEACHER')">TEACHER</option>
          <option :value="mapRoleToId('STUDENT')">STUDENT</option>
        </select>
        <select
          v-model="sortBy"
          class="w-full md:w-48 py-2 bg-stone-50 dark:bg-stone-800/50 border-stone-200 dark:border-stone-700 rounded-lg text-sm focus:ring-primary focus:border-primary"
        >
          <option value="createdAt,desc">Newest First</option>
          <option value="createdAt,asc">Oldest First</option>
        </select>
        <button
          class="flex items-center justify-center gap-1 h-10 px-2 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-slate-700 dark:text-slate-300 rounded-lg font-semibold transition-all text-xs md:text-sm"
          @click="clearFilters"
        >
          <span class="material-symbols-outlined text-[18px]">filter_list</span>
          <span class="hidden md:inline">Clear</span>
        </button>
      </div>
    </div>

    <!-- Table -->
    <div class="w-full overflow-hidden rounded-xl border border-stone-200 dark:border-stone-800 bg-surface-light dark:bg-surface-dark shadow-sm">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-stone-50 dark:bg-stone-900/50 border-b border-stone-200 dark:border-stone-800">
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap">ID</th>
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap">User</th>
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap">Role</th>
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap">Status</th>
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap">Created At</th>
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap text-right">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-200 dark:divide-stone-800">
            <tr
              v-for="user in filteredUsers"
              :key="user.userId"
              class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors"
            >
              <td class="p-4 text-sm font-medium text-slate-500 whitespace-nowrap">
                {{ user.userId.substring(0, 8) }}
              </td>
              <td class="p-4 whitespace-nowrap">
                <div class="flex items-center gap-3">
                  <div
                    class="w-10 h-10 rounded-full overflow-hidden border border-primary/20 flex items-center justify-center bg-orange-100 dark:bg-orange-900/20"
                  >
                    <img
                      v-if="user.profilePictureUrl"
                      :src="user.profilePictureUrl"
                      :alt="user.email"
                      class="w-full h-full object-cover"
                    />
                    <span v-else class="text-primary font-bold text-sm">
                      {{ (user.fullName && user.fullName.length > 0 ? user.fullName : buildDisplayName(user.email)).substring(0, 2).toUpperCase() }}
                    </span>
                  </div>
                  <div class="flex flex-col min-w-0">
                    <span class="text-sm font-bold text-slate-900 dark:text-white leading-none truncate">
                      {{ user.fullName && user.fullName.length > 0 ? user.fullName : buildDisplayName(user.email) }}
                    </span>
                    <span class="text-xs text-slate-500 dark:text-slate-400 mt-1 truncate">{{ user.email }}</span>
                  </div>
                </div>
              </td>
              <td class="p-4">
                <span
                  :class="['text-xs font-bold px-2 py-1 rounded-md', getRolePillClasses(user.role.roleName)]"
                >
                  {{ user.role.roleName }}
                </span>
              </td>
              <td class="p-4">
                <span
                  :class="[
                    'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-bold ring-1 ring-inset',
                    getStatusPillClasses(user.status).container,
                  ]"
                >
                  <span
                    :class="['w-1.5 h-1.5 rounded-full', getStatusPillClasses(user.status).dot]"
                  ></span>
                  {{ user.status }}
                </span>
              </td>
              <td class="p-4 text-sm text-slate-600 dark:text-slate-400 whitespace-nowrap">
                {{ formatCreatedAt(user.createdAt) }}
              </td>
              <td class="p-4 text-right whitespace-nowrap">
                <div class="flex items-center justify-end gap-2">
                  <button
                    class="p-2 text-slate-400 hover:text-primary transition-colors rounded-lg hover:bg-stone-100 dark:hover:bg-stone-800"
                    aria-label="View detail"
                    @click="handleEdit(user)"
                  >
                    <span class="material-symbols-outlined text-[20px]">visibility</span>
                  </button>
                  <button
                    class="p-2 text-slate-400 hover:text-red-500 transition-colors rounded-lg hover:bg-stone-100 dark:hover:bg-stone-800"
                    aria-label="Delete user"
                    @click="handleDelete(user)"
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

  <!-- Add New User Modal -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="showAddUserModal"
        class="fixed inset-0 z-100 flex items-center justify-center p-4 backdrop-blur-md bg-stone-900/40"
      >
        <div
          class="bg-surface-light dark:bg-surface-dark w-full max-w-2xl rounded-xl shadow-2xl overflow-hidden border border-stone-200 dark:border-stone-800 max-h-[90vh] flex flex-col"
        >
          <div class="bg-primary px-6 py-4 flex items-center justify-between shrink-0">
            <h2 class="text-white text-xl font-bold flex items-center gap-2">
              <span class="material-symbols-outlined">person_add</span>
              Add New User
            </h2>
            <button class="text-white/80 hover:text-white transition-colors" @click="closeAddUserModal">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <form class="p-6 overflow-y-auto space-y-6 flex-1" @submit.prevent="submitNewUser">
            <div v-if="createUserError" class="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 text-sm">
              {{ createUserError }}
            </div>
            <div class="space-y-3">
              <label class="text-sm font-semibold text-slate-700 dark:text-slate-300">Role Selection</label>
              <div class="flex p-1 bg-stone-100 dark:bg-stone-800 rounded-lg gap-1">
                <button
                  type="button"
                  :class="[
                    'flex-1 text-center py-2 text-sm font-medium rounded-md cursor-pointer transition-all border',
                    newUserRole === 'TEACHER'
                      ? 'bg-primary text-white border-primary'
                      : 'bg-transparent text-slate-600 dark:text-slate-400 border-transparent',
                  ]"
                  @click="newUserRole = 'TEACHER'"
                >
                  Teacher
                </button>
                <button
                  type="button"
                  :class="[
                    'flex-1 text-center py-2 text-sm font-medium rounded-md cursor-pointer transition-all border',
                    newUserRole === 'STUDENT'
                      ? 'bg-primary text-white border-primary'
                      : 'bg-transparent text-slate-600 dark:text-slate-400 border-transparent',
                  ]"
                  @click="newUserRole = 'STUDENT'"
                >
                  Student
                </button>
              </div>

              <!-- Teacher fields -->
              <div v-if="newUserRole === 'TEACHER'" class="pt-4 space-y-4">
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      First Name <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newTeacher.firstName"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="John"
                      type="text"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Last Name <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newTeacher.lastName"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="Doe"
                      type="text"
                    />
                  </div>
                  <div class="space-y-1.5 sm:col-span-2">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Email Address <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newTeacher.email"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="j.doe@fpt.edu.vn"
                      type="email"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Teacher Code <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newTeacher.teacherCode"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="HJ170001"
                      type="text"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Department <span class="text-red-500">*</span>
                    </label>
                    <select
                      v-model.number="newTeacher.departmentId"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                    >
                      <option :value="null">— Select department —</option>
                      <option
                        v-for="d in departments"
                        :key="d.departmentId"
                        :value="d.departmentId"
                      >
                        {{ d.name }}
                      </option>
                    </select>
                    <p v-if="departmentsLoading" class="text-xs text-slate-500 mt-1">Loading departments…</p>
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Phone Number <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newTeacher.phone"
                      required
                      inputmode="numeric"
                      @input="newTeacher.phone = newTeacher.phone.replace(/\\D/g, '')"
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="0901000001"
                      type="tel"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Specialization <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newTeacher.specialization"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="Artificial Intelligence"
                      type="text"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Academic Rank <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newTeacher.academicRank"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="Professor"
                      type="text"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Office Room <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newTeacher.officeRoom"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="A-301"
                      type="text"
                    />
                  </div>
                  <div class="space-y-1.5 sm:col-span-2">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Degrees / Qualifications <span class="text-red-500">*</span>
                    </label>
                    <textarea
                      v-model="newTeacher.degreesQualification"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all resize-none"
                      placeholder="Ph.D. in Computer Science"
                      rows="2"
                    />
                  </div>
                </div>
              </div>

              <!-- Student fields -->
              <div v-else class="pt-4 space-y-4">
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      First Name <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newStudent.firstName"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="Sarah"
                      type="text"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Last Name <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newStudent.lastName"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="Miller"
                      type="text"
                    />
                  </div>
                  <div class="space-y-1.5 sm:col-span-2">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Email Address <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newStudent.email"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="s.miller@fpt.edu.vn"
                      type="email"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Student Code <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newStudent.studentCode"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="HE170001"
                      type="text"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Department <span class="text-red-500">*</span>
                    </label>
                    <select
                      v-model.number="newStudent.departmentId"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                    >
                      <option :value="null">— Select department —</option>
                      <option
                        v-for="d in departments"
                        :key="d.departmentId"
                        :value="d.departmentId"
                      >
                        {{ d.name }}
                      </option>
                    </select>
                    <p v-if="departmentsLoading" class="text-xs text-slate-500 mt-1">Loading departments…</p>
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Major <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newStudent.major"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="Software Engineering"
                      type="text"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Year (1-4) <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model.number="newStudent.year"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="2"
                      type="number"
                      min="1"
                      max="4"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Date of Birth <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newStudent.dob"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      type="date"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Gender <span class="text-red-500">*</span>
                    </label>
                    <select
                      v-model="newStudent.gender"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                    >
                      <option value="">— Select —</option>
                      <option value="MALE">Male</option>
                      <option value="FEMALE">Female</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Phone Number <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newStudent.phone"
                      required
                      inputmode="numeric"
                      @input="newStudent.phone = newStudent.phone.replace(/\\D/g, '')"
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="0912000001"
                      type="tel"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Manage Class <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newStudent.manageClass"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="SE1701"
                      type="text"
                    />
                  </div>
                  <div class="space-y-1.5 sm:col-span-2">
                    <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Home Address <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="newStudent.address"
                      required
                      class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary transition-all"
                      placeholder="100 Le Loi, HCM"
                      type="text"
                    />
                  </div>
                </div>
              </div>
            </div>

            <div class="flex items-center justify-end gap-3 pt-6 border-t border-stone-200 dark:border-stone-800 shrink-0">
              <button
                type="button"
                class="px-5 py-2.5 rounded-lg text-slate-600 dark:text-slate-400 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 font-bold transition-all"
                @click="closeAddUserModal"
              >
                Cancel
              </button>
              <button
                type="submit"
                :disabled="createUserLoading"
                class="px-5 py-2.5 rounded-lg bg-primary hover:bg-primary-dark text-white font-bold shadow-lg shadow-primary/20 transition-all active:scale-95 disabled:opacity-60 disabled:cursor-not-allowed"
              >
                {{ createUserLoading ? 'Creating…' : 'Create User' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- Import Excel Modal -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="showImportExcelModal"
        class="fixed inset-0 z-100 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4"
      >
        <div
          class="bg-surface-light dark:bg-surface-dark w-full max-w-2xl rounded-2xl shadow-2xl flex flex-col overflow-hidden border border-stone-200 dark:border-stone-800"
        >
          <div class="px-8 py-6 border-b border-stone-100 dark:border-stone-800 flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary">
                <span class="material-symbols-outlined">upload_file</span>
              </div>
              <div>
                <h2 class="text-xl font-bold text-slate-900 dark:text-white">Bulk Import Users</h2>
                <p class="text-sm text-slate-500 dark:text-slate-400">Import multiple accounts using Excel templates</p>
              </div>
            </div>
            <button class="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition-colors" @click="closeImportExcelModal">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div class="px-8 py-8 flex flex-col gap-8 max-h-[70vh] overflow-y-auto">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div class="p-5 rounded-xl border border-stone-200 dark:border-stone-800 bg-stone-50/50 dark:bg-stone-900/30 flex flex-col gap-4">
                <div class="flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary">school</span>
                  <h3 class="font-bold text-slate-800 dark:text-slate-200">Teacher Template</h3>
                </div>
                <p class="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
                  Download the Excel format specialized for teacher profiles, departments, and payroll IDs.
                </p>
                <button
                  type="button"
                  class="w-full flex items-center justify-center gap-2 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg text-sm font-bold transition-all active:scale-[0.98]"
                  @click="downloadTeacherTemplate"
                >
                  <span class="material-symbols-outlined text-lg">download</span>
                  Download Template
                </button>
              </div>
              <div class="p-5 rounded-xl border border-stone-200 dark:border-stone-800 bg-stone-50/50 dark:bg-stone-900/30 flex flex-col gap-4">
                <div class="flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary">group</span>
                  <h3 class="font-bold text-slate-800 dark:text-slate-200">Student Template</h3>
                </div>
                <p class="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
                  Download the Excel format specialized for student data, parent contact, and grade levels.
                </p>
                <button
                  type="button"
                  class="w-full flex items-center justify-center gap-2 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg text-sm font-bold transition-all active:scale-[0.98]"
                  @click="downloadStudentTemplate"
                >
                  <span class="material-symbols-outlined text-lg">download</span>
                  Download Template
                </button>
              </div>
            </div>

            <div class="flex flex-col gap-3">
              <label class="text-sm font-bold text-slate-700 dark:text-slate-300">Upload Filled Template</label>
              <input
                ref="importFileInputRef"
                type="file"
                accept=".xlsx,.xls"
                class="hidden"
                @change="handleImportFileSelect"
              />
              <div
                class="border-2 border-dashed border-primary/40 hover:border-primary bg-primary/5 rounded-xl p-10 flex flex-col items-center justify-center gap-4 cursor-pointer transition-all group"
                @click="triggerImportFileInput"
                @drop="handleImportDrop"
                @dragover="handleImportDragOver"
              >
                <div class="w-14 h-14 rounded-full bg-white dark:bg-stone-800 shadow-md flex items-center justify-center text-primary group-hover:scale-110 transition-transform">
                  <span class="material-symbols-outlined text-3xl">cloud_upload</span>
                </div>
                <div class="text-center">
                  <p v-if="!importFile" class="text-sm font-bold text-slate-900 dark:text-white">
                    Click to upload or drag and drop
                  </p>
                  <p v-else class="text-sm font-bold text-primary">{{ importFile.name }}</p>
                  <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">Excel files only (.xlsx, .xls) up to 10MB</p>
                </div>
              </div>
            </div>
          </div>

          <div class="px-8 py-5 border-t border-stone-100 dark:border-stone-800 bg-stone-50/50 dark:bg-stone-900/50 flex items-center justify-end gap-3">
            <button
              type="button"
              class="px-6 py-2.5 text-slate-600 dark:text-slate-400 font-bold hover:bg-stone-200 dark:hover:bg-stone-800 rounded-lg transition-all"
              @click="closeImportExcelModal"
            >
              Cancel
            </button>
            <button
              type="button"
              :class="[
                'px-8 py-2.5 font-bold rounded-lg transition-all',
                importFile
                  ? 'bg-primary hover:bg-primary-dark text-white cursor-pointer'
                  : 'bg-primary/50 text-white cursor-not-allowed',
              ]"
              :disabled="!importFile"
              @click="processImport"
            >
              Process Import
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- Delete User Confirm Modal -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="showDeleteConfirmModal"
        class="fixed inset-0 z-120 flex items-center justify-center p-4 backdrop-blur-md bg-stone-900/40"
      >
        <div
          class="bg-surface-light dark:bg-surface-dark w-full max-w-md rounded-xl shadow-2xl overflow-hidden border border-stone-200 dark:border-stone-800 max-h-[90vh] flex flex-col"
        >
          <div class="bg-red-600 px-6 py-4 flex items-center justify-between shrink-0">
            <h2 class="text-white text-lg font-bold flex items-center gap-2">
              <span class="material-symbols-outlined">delete</span>
              Delete User
            </h2>
            <button class="text-white/80 hover:text-white transition-colors" type="button" @click="showDeleteConfirmModal = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div class="p-6 space-y-4 flex-1">
            <p class="text-sm text-slate-700 dark:text-slate-200">
              Are you sure you want to delete this user? This action will
              <span class="font-bold">soft delete</span>
              the account and log the user out from all devices.
            </p>
            <div class="rounded-lg bg-stone-50 dark:bg-stone-900/40 border border-stone-200 dark:border-stone-800 p-3 text-sm">
              <p class="font-semibold text-slate-900 dark:text-white">
                {{ deleteTargetUser?.fullName || deleteTargetUser?.email }}
              </p>
              <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">
                {{ deleteTargetUser?.email }} • {{ deleteTargetUser?.role.roleName }}
              </p>
            </div>
            <div
              v-if="deleteError"
              class="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 text-sm"
            >
              {{ deleteError }}
            </div>

            <div class="flex items-center justify-end gap-3 pt-4 border-t border-stone-200 dark:border-stone-800 shrink-0">
              <button
                type="button"
                class="px-4 py-2 rounded-lg text-slate-600 dark:text-slate-400 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-sm font-bold transition-all"
                @click="showDeleteConfirmModal = false"
              >
                Cancel
              </button>
              <button
                type="button"
                :disabled="deleteLoading"
                class="px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 text-white text-sm font-bold shadow-lg shadow-red-600/30 transition-all active:scale-95 disabled:opacity-60 disabled:cursor-not-allowed"
                @click="confirmDeleteUser"
              >
                {{ deleteLoading ? 'Deleting…' : 'Delete User' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- Delete User Result Modal -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="showDeleteResultModal"
        class="fixed inset-0 z-120 flex items-center justify-center p-4 backdrop-blur-md bg-stone-900/40"
      >
        <div
          class="bg-surface-light dark:bg-surface-dark w-full max-w-md rounded-xl shadow-2xl overflow-hidden border border-stone-200 dark:border-stone-800 max-h-[80vh] flex flex-col"
        >
          <div
            :class="[
              'px-6 py-4 flex items-center justify-between shrink-0',
              deleteResultSuccess ? 'bg-emerald-600' : 'bg-red-600',
            ]"
          >
            <h2 class="text-white text-lg font-bold flex items-center gap-2">
              <span class="material-symbols-outlined">
                {{ deleteResultSuccess ? 'check_circle' : 'error' }}
              </span>
              {{ deleteResultSuccess ? 'Deleted Successfully' : 'Delete Failed' }}
            </h2>
            <button class="text-white/80 hover:text-white transition-colors" type="button" @click="closeDeleteModals">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div class="p-6 flex-1 flex items-center">
            <p class="text-sm text-slate-700 dark:text-slate-200">
              {{ deleteResultMessage }}
            </p>
          </div>

          <div class="px-6 py-4 border-t border-stone-200 dark:border-stone-800 bg-stone-50/60 dark:bg-stone-900/40 shrink-0 flex justify-end">
            <button
              type="button"
              class="px-5 py-2 rounded-lg bg-primary hover:bg-primary-dark text-white text-sm font-bold shadow-md shadow-primary/30 transition-all active:scale-95"
              @click="closeDeleteModals"
            >
              OK
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- Create User Result Modal -->
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
              {{ createResultSuccess ? 'User Created' : 'Create Failed' }}
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
