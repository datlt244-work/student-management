<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import {
  getAdminDepartments,
  getAdminUserById,
  updateAdminUserProfile,
  updateAdminUserStatus,
  type AdminDepartmentItem,
  type AdminUpdateUserProfileRequest,
  type AdminUpdateUserStatusRequest,
  type AdminUserDetailResult,
} from '@/services/adminUserService'

interface StudentCourse {
  code: string
  name: string
  instructor: string
  schedule: string
  status: string
}

interface UserDetail {
  id: string
  userId: string
  name: string
  email: string
  avatar?: string
  role: string
  status: string
  studentCode?: string
  courses?: StudentCourse[]
  [key: string]: unknown
}

const route = useRoute()

const activeTab = ref<'personal' | 'academic' | 'grades'>('personal')
const loading = ref(false)
const fetchError = ref<string | null>(null)

// Raw API detail (for edit)
const detail = ref<AdminUserDetailResult | null>(null)

// Edit modal state
const showEditModal = ref(false)
const editLoading = ref(false)
const editError = ref<string | null>(null)

// Departments for select
const departments = ref<AdminDepartmentItem[]>([])
const departmentsLoading = ref(false)

// Edit form models
const editTeacher = ref({
  firstName: '',
  lastName: '',
  teacherCode: '',
  phone: '',
  departmentId: null as number | null,
  specialization: '',
  academicRank: '',
  officeRoom: '',
  degreesQualification: '',
})

const editStudent = ref({
  firstName: '',
  lastName: '',
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

// Block modal state
const showBlockModal = ref(false)
const blockReason = ref('')
const blockLoading = ref(false)
const blockError = ref<string | null>(null)

// Update profile result modal
const showUpdateResultModal = ref(false)
const updateResultSuccess = ref(false)
const updateResultMessage = ref('')

const defaultStudent: UserDetail & {
  studentCode: string
  phone: string
  dob: string
  gender: string
  address: string
  attendance: number
  credits: number
  creditsTotal: number
  creditsProgress: number
  major: string
  year: string
  advisor: string
  gpa: number
  gpaMax: number
  currentSemester: string
  courses: StudentCourse[]
} = {
  id: 'US-003',
  userId: '00000000-0000-0000-0000-000000000003',
  name: 'Michael Ross',
  email: 'm.ross@student.edu',
  avatar: 'https://ui-avatars.com/api/?name=Michael+Ross&background=4f46e5&color=fff',
  role: 'STUDENT',
  status: 'ACTIVE',
  studentCode: 'STU-12345',
  phone: '+1 (555) 123-4567',
  dob: 'May 14, 2002',
  gender: 'Male',
  address: '789 University Ave, Suite 402,\nCambridge, MA 02138, USA',
  attendance: 94,
  credits: 72,
  creditsTotal: 120,
  creditsProgress: 60,
  major: 'Computer Science',
  year: 'Year 3',
  advisor: 'Dr. Sarah Chen',
  gpa: 3.8,
  gpaMax: 4.0,
  currentSemester: 'Fall 2023',
  courses: [
    { code: 'CS-301', name: 'Operating Systems', instructor: 'Dr. Robert Thorne', schedule: 'Mon, Wed 10:00 AM', status: 'ENROLLED' },
    { code: 'CS-305', name: 'Database Management', instructor: 'Prof. Alexander Wright', schedule: 'Tue, Thu 02:00 PM', status: 'ENROLLED' },
    { code: 'MATH-210', name: 'Linear Algebra', instructor: 'Dr. Sarah Chen', schedule: 'Fri 09:00 AM', status: 'ENROLLED' },
  ],
}

/** Map API user detail to view UserDetail shape */
function mapDetailToUserDetail(d: AdminUserDetailResult): UserDetail {
  const base = {
    id: d.userId,
    userId: d.userId,
    email: d.email,
    role: d.role.roleName,
    status: d.status,
    avatar: undefined as string | undefined,
  }

  if (d.studentProfile) {
    const p = d.studentProfile
    const name = [p.firstName, p.lastName].filter(Boolean).join(' ') || d.email
    const dobFormatted = p.dob
      ? new Date(p.dob + 'T00:00:00').toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
      : '—'
    return {
      ...base,
      ...defaultStudent,
      name,
      userId: d.userId,
      email: p.email ?? d.email,
      role: 'STUDENT',
      status: d.status,
      studentCode: p.studentCode ?? '—',
      phone: p.phone ?? '—',
      dob: dobFormatted,
      gender: p.gender ?? '—',
      address: p.address ?? '—',
      major: p.major ?? '—',
      gpa: p.gpa ?? 0,
      gpaMax: 4.0,
      year: p.year != null ? `Year ${p.year}` : '—',
      manageClass: p.manageClass ?? '—',
      advisor: '—',
      currentSemester: '—',
      attendance: 0,
      credits: 0,
      creditsTotal: 0,
      creditsProgress: 0,
      courses: [],
    }
  }

  if (d.teacherProfile) {
    const p = d.teacherProfile
    const name = [p.firstName, p.lastName].filter(Boolean).join(' ') || d.email
    return {
      ...base,
      ...teacherData,
      name,
      userId: d.userId,
      email: p.email ?? d.email,
      role: 'TEACHER',
      status: d.status,
      title: p.academicRank ?? '—',
      employeeId: p.teacherCode ?? '—',
      department: p.department?.name ?? '—',
      phone: p.phone ?? '—',
      office: p.officeRoom ?? '—',
      specializations: p.specialization ? [p.specialization] : [],
      degreesQualification: p.degreesQualification ?? null,
      classes: [],
      schedule: [],
    }
  }

  // ADMIN or no profile
  return {
    ...base,
    name: d.email,
    userId: d.userId,
    role: d.role.roleName,
    status: d.status,
  } as UserDetail
}

// Placeholder data — overwritten by fetch
const user = ref<UserDetail>(defaultStudent)

// Teacher-specific data
const teacherData: UserDetail = {
  id: 'US-002',
  userId: '00000000-0000-0000-0000-000000000002',
  name: 'Sarah Chen, Ph.D.',
  email: 's.chen@school.org',
  avatar: 'https://ui-avatars.com/api/?name=Sarah+Chen&background=9333ea&color=fff',
  role: 'TEACHER',
  status: 'ACTIVE',
  title: 'Senior Mathematics Educator',
  employeeId: '#EMP-2024-042',
  department: 'Mathematics',
  phone: '+1 (555) 012-3456',
  office: 'Science Building, Room 402',
  degreesQualification: 'Ph.D. in Theoretical Mathematics - Stanford University\nM.Sc. in Applied Statistics - MIT\nB.A. in Mathematics - UC Berkeley',
  specializations: ['Calculus', 'Linear Algebra', 'Graph Theory', 'Number Theory'],
  experience: 'Over 12 years of teaching experience in higher education. Joined the institution in 2018. Previously held research positions at the Global Math Research Institute.',
  classes: [
    { name: 'Advanced Calculus III', code: 'MATH-401-A', students: 32 },
    { name: 'Linear Algebra II', code: 'MATH-302-B', students: 45 },
    { name: 'Discrete Mathematics', code: 'MATH-201-C', students: 28 },
  ],
  schedule: [
    { day: 'Mon', slots: [{ time: '09:00 AM', course: 'MATH-401', color: 'blue' }, { time: '02:00 PM', course: 'MATH-201', color: 'orange' }] },
    { day: 'Tue', slots: [{ time: '11:00 AM', course: 'MATH-302', color: 'purple' }] },
    { day: 'Wed', slots: [{ time: '09:00 AM', course: 'MATH-401', color: 'blue' }, { type: 'break' }, { time: '02:00 PM', course: 'MATH-201', color: 'orange' }] },
    { day: 'Thu', slots: [{ time: '11:00 AM', course: 'MATH-302', color: 'purple' }] },
    { day: 'Fri', slots: [{ time: '10:00 AM', course: 'Faculty Meeting', color: 'stone' }] },
  ],
}

const statusBadgeClasses = computed(() => {
  const s = user.value.status
  if (s === 'ACTIVE') return 'bg-green-500'
  if (s === 'INACTIVE') return 'bg-stone-400'
  if (s === 'BLOCKED') return 'bg-red-500'
  return 'bg-yellow-500'
})

function handleBlock() {
  const currentStatus = user.value.status
  if (currentStatus === 'BLOCKED') {
    // Unblock without reason
    void submitStatusChange('ACTIVE')
  } else {
    blockError.value = null
    blockReason.value = ''
    showBlockModal.value = true
  }
}

function handleEdit() {
  editError.value = null
  showEditModal.value = true

  // Load departments (best-effort)
  if (departments.value.length === 0) {
    departmentsLoading.value = true
    getAdminDepartments()
      .then((d) => {
        departments.value = d
      })
      .catch(() => {
        departments.value = []
      })
      .finally(() => {
        departmentsLoading.value = false
      })
  }

  const d = detail.value
  if (!d) return

  if (d.teacherProfile) {
    const p = d.teacherProfile
    editTeacher.value = {
      firstName: p.firstName ?? '',
      lastName: p.lastName ?? '',
      teacherCode: p.teacherCode ?? '',
      phone: p.phone ?? '',
      departmentId: p.department?.departmentId ?? null,
      specialization: p.specialization ?? '',
      academicRank: p.academicRank ?? '',
      officeRoom: p.officeRoom ?? '',
      degreesQualification: p.degreesQualification ?? '',
    }
  }

  if (d.studentProfile) {
    const p = d.studentProfile
    editStudent.value = {
      firstName: p.firstName ?? '',
      lastName: p.lastName ?? '',
      studentCode: p.studentCode ?? '',
      dob: p.dob ?? '',
      gender: (p.gender as '' | 'MALE' | 'FEMALE' | 'OTHER') ?? '',
      major: p.major ?? '',
      phone: p.phone ?? '',
      departmentId: p.department?.departmentId ?? null,
      address: p.address ?? '',
      year: p.year ?? null,
      manageClass: p.manageClass ?? '',
    }
  }
}

function viewTranscript() {
  // TODO: navigate to transcript
  console.log('View transcript')
}

const isTeacher = computed(() => user.value.role === 'TEACHER')

const studentCourses = computed<StudentCourse[]>(() => {
  const c = user.value.courses
  return Array.isArray(c) ? c : []
})

async function fetchUserDetail(userId: string) {
  loading.value = true
  fetchError.value = null
  try {
    const data = await getAdminUserById(userId)
    detail.value = data
    user.value = mapDetailToUserDetail(data)
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Failed to load user'
    fetchError.value = message === 'USER_NOT_FOUND' ? 'User not found.' : message
    user.value = defaultStudent
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function submitStatusChange(targetStatus: 'ACTIVE' | 'INACTIVE' | 'BLOCKED') {
  const userId = route.params.userId as string | undefined
  if (!userId) return

  const payload: AdminUpdateUserStatusRequest = {
    status: targetStatus,
    banReason: targetStatus === 'BLOCKED' ? blockReason.value.trim() || undefined : undefined,
  }

  try {
    blockLoading.value = true
    blockError.value = null
    await updateAdminUserStatus(userId, payload)
    showBlockModal.value = false
    await fetchUserDetail(userId)
  } catch (e) {
    blockError.value = e instanceof Error ? e.message : 'Failed to update status'
  } finally {
    blockLoading.value = false
  }
}

function closeEditModal() {
  showEditModal.value = false
  editError.value = null
}

async function submitUpdate() {
  const userId = route.params.userId as string | undefined
  if (!userId) return

  editError.value = null
  const isTeacherRole = detail.value?.role?.roleName === 'TEACHER'

  const payload: AdminUpdateUserProfileRequest = isTeacherRole
    ? {
        firstName: editTeacher.value.firstName,
        lastName: editTeacher.value.lastName,
        phone: editTeacher.value.phone,
        departmentId: editTeacher.value.departmentId ?? undefined,
        teacherCode: editTeacher.value.teacherCode,
        specialization: editTeacher.value.specialization,
        academicRank: editTeacher.value.academicRank,
        officeRoom: editTeacher.value.officeRoom,
        degreesQualification: editTeacher.value.degreesQualification,
      }
    : {
        firstName: editStudent.value.firstName,
        lastName: editStudent.value.lastName,
        phone: editStudent.value.phone,
        departmentId: editStudent.value.departmentId ?? undefined,
        studentCode: editStudent.value.studentCode,
        dob: editStudent.value.dob || undefined,
        gender: (editStudent.value.gender || undefined) as 'MALE' | 'FEMALE' | 'OTHER' | undefined,
        major: editStudent.value.major,
        address: editStudent.value.address,
        year: editStudent.value.year ?? undefined,
        manageClass: editStudent.value.manageClass,
      }

  try {
    editLoading.value = true
    await updateAdminUserProfile(userId, payload)
    closeEditModal()
    await fetchUserDetail(userId)
    updateResultSuccess.value = true
    updateResultMessage.value = 'Profile has been updated successfully.'
    showUpdateResultModal.value = true
  } catch (e) {
    editError.value = e instanceof Error ? e.message : 'Failed to update user'
    updateResultSuccess.value = false
    updateResultMessage.value = editError.value
    showUpdateResultModal.value = true
  } finally {
    editLoading.value = false
  }
}

// Fetch user khi route thay đổi
watch(
  () => route.params.userId as string | undefined,
  (userId) => {
    if (userId) {
      fetchUserDetail(userId)
    }
  },
  { immediate: true },
)
</script>

<template>
  <div class="max-w-[1400px] w-full mx-auto p-6 md:p-10 flex flex-col gap-6">
    <!-- Breadcrumb -->
    <nav class="flex items-center gap-2 text-sm font-medium">
      <RouterLink :to="{ name: 'admin-users' }" class="text-slate-500 hover:text-primary transition-colors">
        User Management
      </RouterLink>
      <span class="material-symbols-outlined text-slate-400 text-[18px]">chevron_right</span>
      <span class="text-slate-900 dark:text-white">{{ isTeacher ? 'Teacher Profile' : 'Student Details' }}</span>
    </nav>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-16">
      <span class="material-symbols-outlined animate-spin text-4xl text-primary">progress_activity</span>
    </div>

    <!-- Error -->
    <div v-else-if="fetchError" class="rounded-xl border border-red-200 dark:border-red-900/50 bg-red-50 dark:bg-red-900/10 p-4 flex items-center gap-3">
      <span class="material-symbols-outlined text-red-500">error</span>
      <p class="text-red-700 dark:text-red-400 font-medium">{{ fetchError }}</p>
      <RouterLink :to="{ name: 'admin-users' }" class="ml-auto text-red-600 dark:text-red-400 hover:underline font-semibold">Back to list</RouterLink>
    </div>

    <!-- ========== STUDENT LAYOUT ========== -->
    <template v-else-if="!isTeacher">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <h1 class="text-slate-900 dark:text-white text-3xl font-bold leading-tight tracking-tight">Student Profile</h1>
      <div class="flex items-center gap-3 shrink-0">
        <button
          class="flex items-center gap-2 px-5 py-2.5 border border-red-500 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/10 rounded-lg font-bold transition-all active:scale-95"
          @click="handleBlock"
        >
          <span class="material-symbols-outlined text-[20px]">{{ user.status === 'BLOCKED' ? 'how_to_reg' : 'block' }}</span>
          {{ user.status === 'BLOCKED' ? 'Unblock Account' : 'Block Account' }}
        </button>
        <button
          class="flex items-center gap-2 px-6 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95"
          @click="handleEdit"
        >
          <span class="material-symbols-outlined text-[20px]">edit</span>
          Edit Profile
        </button>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
      <!-- Left sidebar -->
      <div class="lg:col-span-3 flex flex-col gap-6">
        <!-- Avatar card -->
        <div class="bg-surface-light dark:bg-surface-dark p-8 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex flex-col items-center text-center">
          <div class="relative mb-6">
            <div class="w-32 h-32 rounded-full border-4 border-stone-50 dark:border-stone-800 overflow-hidden shadow-xl ring-4 ring-primary/10">
              <img
                v-if="user.avatar"
                :src="user.avatar"
                :alt="user.name"
                class="w-full h-full object-cover"
              />
              <div
                v-else
                class="w-full h-full bg-primary/10 flex items-center justify-center text-primary font-bold text-3xl"
              >
                {{ user.name.substring(0, 2).toUpperCase() }}
              </div>
            </div>
            <div class="absolute -bottom-2 left-1/2 -translate-x-1/2">
              <span
                :class="[
                  'inline-flex items-center gap-1.5 rounded-full px-4 py-1 text-xs font-bold text-white shadow-lg ring-2 ring-white dark:ring-stone-900',
                  statusBadgeClasses,
                ]"
              >
                {{ user.status }}
              </span>
            </div>
          </div>
          <h2 class="text-xl font-bold text-slate-900 dark:text-white">{{ user.name }}</h2>
          <p class="text-slate-500 dark:text-slate-400 font-medium mt-1">{{ user.studentCode }}</p>
        </div>

        <!-- Quick Summary -->
        <div class="bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex flex-col gap-4">
          <h3 class="text-sm font-bold text-slate-900 dark:text-white uppercase tracking-wider">Quick Summary</h3>
          <div class="space-y-3">
            <div class="flex justify-between items-center text-sm">
              <span class="text-slate-500">Attendance</span>
              <span class="font-bold text-green-600">{{ user.attendance }}%</span>
            </div>
            <div class="flex justify-between items-center text-sm">
              <span class="text-slate-500">Credits</span>
              <span class="font-bold text-slate-900 dark:text-white">{{ user.credits }} / {{ user.creditsTotal }}</span>
            </div>
            <div class="w-full bg-stone-100 dark:bg-stone-800 h-2 rounded-full overflow-hidden mt-1">
              <div class="bg-primary h-full transition-all" :style="{ width: user.creditsProgress + '%' }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Main content -->
      <div class="lg:col-span-9 flex flex-col gap-6">
        <!-- Tabs -->
        <div class="bg-surface-light dark:bg-surface-dark rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm overflow-hidden">
          <div class="border-b border-stone-200 dark:border-stone-800 bg-stone-50/50 dark:bg-stone-900/30">
            <nav class="flex px-4 overflow-x-auto">
              <button
                :class="[
                  'border-b-2 px-6 py-4 text-sm font-medium transition-all whitespace-nowrap',
                  activeTab === 'personal'
                    ? 'border-primary text-primary font-bold'
                    : 'border-transparent text-slate-500 hover:text-slate-700 dark:hover:text-slate-300',
                ]"
                @click="activeTab = 'personal'"
              >
                Personal Info
              </button>
              <button
                :class="[
                  'border-b-2 px-6 py-4 text-sm font-medium transition-all whitespace-nowrap',
                  activeTab === 'academic'
                    ? 'border-primary text-primary font-bold'
                    : 'border-transparent text-slate-500 hover:text-slate-700 dark:hover:text-slate-300',
                ]"
                @click="activeTab = 'academic'"
              >
                Academic
              </button>
              <button
                :class="[
                  'border-b-2 px-6 py-4 text-sm font-medium transition-all whitespace-nowrap',
                  activeTab === 'grades'
                    ? 'border-primary text-primary font-bold'
                    : 'border-transparent text-slate-500 hover:text-slate-700 dark:hover:text-slate-300',
                ]"
                @click="activeTab = 'grades'"
              >
                Grades Summary
              </button>
            </nav>
          </div>

          <div class="p-8">
            <!-- Personal Info tab -->
            <div v-show="activeTab === 'personal'" class="space-y-8">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-y-8 gap-x-12">
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Email Address</p>
                  <p class="text-slate-900 dark:text-white font-medium">{{ user.email }}</p>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Phone Number</p>
                  <p class="text-slate-900 dark:text-white font-medium">{{ user.phone }}</p>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Date of Birth</p>
                  <p class="text-slate-900 dark:text-white font-medium">{{ user.dob }}</p>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Gender</p>
                  <p class="text-slate-900 dark:text-white font-medium">{{ user.gender }}</p>
                </div>
                <div class="md:col-span-2">
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Address</p>
                  <p class="text-slate-900 dark:text-white font-medium leading-relaxed whitespace-pre-line">{{ user.address }}</p>
                </div>
              </div>

              <div class="mt-12 pt-8 border-t border-stone-100 dark:border-stone-800">
                <h3 class="text-lg font-bold text-slate-900 dark:text-white mb-6">Academic Overview</h3>
                <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                  <div class="bg-stone-50 dark:bg-stone-900/50 p-5 rounded-lg border border-stone-100 dark:border-stone-800">
                    <p class="text-xs font-bold text-slate-400 uppercase mb-1">Major</p>
                    <p class="text-slate-900 dark:text-white font-bold">{{ user.major }}</p>
                  </div>
                  <div class="bg-stone-50 dark:bg-stone-900/50 p-5 rounded-lg border border-stone-100 dark:border-stone-800">
                    <p class="text-xs font-bold text-slate-400 uppercase mb-1">Year</p>
                    <p class="text-slate-900 dark:text-white font-bold">{{ user.year }}</p>
                  </div>
                  <div class="bg-stone-50 dark:bg-stone-900/50 p-5 rounded-lg border border-stone-100 dark:border-stone-800">
                    <p class="text-xs font-bold text-slate-400 uppercase mb-1">Class</p>
                    <p class="text-slate-900 dark:text-white font-bold">{{ user.manageClass ?? '—' }}</p>
                  </div>
                </div>
              </div>

              <div class="mt-8">
                <div class="bg-primary/5 p-6 rounded-xl border border-primary/20 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                  <div class="flex items-center gap-4">
                    <div class="w-12 h-12 bg-primary rounded-lg flex items-center justify-center text-white shrink-0">
                      <span class="material-symbols-outlined">analytics</span>
                    </div>
                    <div>
                      <p class="text-sm font-medium text-primary">Cumulative GPA</p>
                      <p class="text-2xl font-bold text-slate-900 dark:text-white">
                        {{ user.gpa }} <span class="text-sm font-normal text-slate-500">/ {{ user.gpaMax }}</span>
                      </p>
                    </div>
                  </div>
                  <button class="text-primary font-bold text-sm hover:underline" @click="viewTranscript">View Full Transcript</button>
                </div>
              </div>
            </div>

            <!-- Academic tab (placeholder) -->
            <div v-show="activeTab === 'academic'">
              <p class="text-slate-500 dark:text-slate-400">Academic records and enrollment history.</p>
            </div>

            <!-- Grades tab (placeholder) -->
            <div v-show="activeTab === 'grades'">
              <p class="text-slate-500 dark:text-slate-400">Grades summary and transcript.</p>
            </div>
          </div>
        </div>

        <!-- Current Semester Courses -->
        <div class="bg-surface-light dark:bg-surface-dark rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm overflow-hidden">
          <div class="px-6 py-5 border-b border-stone-200 dark:border-stone-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
            <h3 class="text-lg font-bold text-slate-900 dark:text-white">Current Semester Courses</h3>
            <span class="text-xs font-bold px-2.5 py-1 bg-stone-100 dark:bg-stone-800 text-slate-600 dark:text-slate-400 rounded-md uppercase">
              {{ user.currentSemester }}
            </span>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-stone-50 dark:bg-stone-900/50 border-b border-stone-200 dark:border-stone-800">
                  <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Course Code</th>
                  <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Course Name</th>
                  <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Instructor</th>
                  <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Schedule</th>
                  <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 text-right">Status</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-stone-200 dark:divide-stone-800">
                <tr
                  v-for="course in studentCourses"
                  :key="course.code"
                  class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors"
                >
                  <td class="p-4 text-sm font-bold text-slate-900 dark:text-white">{{ course.code }}</td>
                  <td class="p-4 text-sm text-slate-600 dark:text-slate-400">{{ course.name }}</td>
                  <td class="p-4 text-sm text-slate-600 dark:text-slate-400">{{ course.instructor }}</td>
                  <td class="p-4 text-sm text-slate-600 dark:text-slate-400 whitespace-nowrap">{{ course.schedule }}</td>
                  <td class="p-4 text-right">
                    <span class="inline-flex items-center rounded-full bg-blue-50 dark:bg-blue-900/30 px-2.5 py-0.5 text-xs font-bold text-blue-700 dark:text-blue-400">
                      {{ course.status }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
    </template>

    <!-- ========== TEACHER LAYOUT ========== -->
    <template v-else>
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div class="flex items-center gap-2 text-sm">
        <span class="text-slate-900 dark:text-white font-semibold">Teacher Profile</span>
      </div>
      <div class="flex items-center gap-3 flex-wrap">
        <button
          class="flex items-center gap-2 px-4 py-2 border border-red-200 dark:border-red-900/30 bg-white dark:bg-stone-900 text-red-600 dark:text-red-400 rounded-lg text-sm font-semibold hover:bg-red-50 dark:hover:bg-red-900/10 transition-all"
          @click="handleBlock"
        >
          <span class="material-symbols-outlined text-[18px]">{{ user.status === 'BLOCKED' ? 'how_to_reg' : 'block' }}</span>
          {{ user.status === 'BLOCKED' ? 'Unblock Account' : 'Block Account' }}
        </button>
        <button
          class="flex items-center gap-2 px-4 py-2 bg-primary hover:bg-primary-dark text-white rounded-lg text-sm font-bold shadow-lg shadow-primary/20 transition-all active:scale-95"
          @click="handleEdit"
        >
          <span class="material-symbols-outlined text-[18px]">edit</span>
          Edit Profile
        </button>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
      <!-- Left sidebar -->
      <div class="lg:col-span-4 flex flex-col gap-6">
        <div class="bg-surface-light dark:bg-surface-dark p-8 rounded-2xl border border-stone-200 dark:border-stone-800 shadow-sm flex flex-col items-center text-center">
          <div class="relative mb-6">
            <div class="w-40 h-40 rounded-full border-4 border-primary/20 p-1 overflow-hidden">
              <img
                v-if="user.avatar"
                :src="user.avatar as string"
                :alt="user.name as string"
                class="w-full h-full rounded-full object-cover shadow-xl"
              />
              <div v-else class="w-full h-full rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-4xl">
                {{ (user.name as string)?.substring(0, 2).toUpperCase() }}
              </div>
            </div>
            <span class="absolute bottom-2 right-2 w-6 h-6 bg-green-500 border-4 border-white dark:border-surface-dark rounded-full"></span>
          </div>
          <h2 class="text-2xl font-bold text-slate-900 dark:text-white">{{ user.name }}</h2>
          <p class="text-primary font-semibold mt-1">{{ user.title }}</p>
          <div class="mt-6 w-full space-y-4 pt-6 border-t border-stone-100 dark:border-stone-800">
            <div class="flex justify-between items-center text-sm">
              <span class="text-slate-500 dark:text-slate-400">Employee ID</span>
              <span class="font-bold text-slate-900 dark:text-white">{{ user.employeeId }}</span>
            </div>
            <div class="flex justify-between items-center text-sm">
              <span class="text-slate-500 dark:text-slate-400">Department</span>
              <span class="font-bold text-slate-900 dark:text-white">{{ user.department }}</span>
            </div>
            <div class="flex justify-between items-center text-sm">
              <span class="text-slate-500 dark:text-slate-400">Status</span>
              <span class="inline-flex items-center gap-1.5 rounded-full bg-green-50 dark:bg-green-900/30 px-3 py-1 text-xs font-bold text-green-700 dark:text-green-400 ring-1 ring-inset ring-green-600/20">
                {{ user.status }}
              </span>
            </div>
          </div>
          <div class="mt-8 flex gap-3 w-full">
            <button class="flex-1 px-4 py-2 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-slate-700 dark:text-slate-300 rounded-lg text-sm font-semibold transition-all">
              Send Email
            </button>
            <button class="flex-1 px-4 py-2 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-slate-700 dark:text-slate-300 rounded-lg text-sm font-semibold transition-all" @click="handleEdit">
              Edit Profile
            </button>
          </div>
        </div>

        <div class="bg-surface-light dark:bg-surface-dark p-6 rounded-2xl border border-stone-200 dark:border-stone-800 shadow-sm">
          <div class="flex items-center gap-3 mb-6">
            <div class="p-2 bg-primary/10 rounded-lg text-primary">
              <span class="material-symbols-outlined block">contact_mail</span>
            </div>
            <h3 class="font-bold text-slate-900 dark:text-white">Contact Details</h3>
          </div>
          <div class="space-y-4">
            <div class="flex items-start gap-4">
              <span class="material-symbols-outlined text-slate-400 text-[20px] mt-0.5">mail</span>
              <div>
                <p class="text-xs text-slate-500">Email Address</p>
                <p class="text-sm font-medium text-slate-900 dark:text-white">{{ user.email }}</p>
              </div>
            </div>
            <div class="flex items-start gap-4">
              <span class="material-symbols-outlined text-slate-400 text-[20px] mt-0.5">call</span>
              <div>
                <p class="text-xs text-slate-500">Phone Number</p>
                <p class="text-sm font-medium text-slate-900 dark:text-white">{{ user.phone }}</p>
              </div>
            </div>
            <div class="flex items-start gap-4">
              <span class="material-symbols-outlined text-slate-400 text-[20px] mt-0.5">location_on</span>
              <div>
                <p class="text-xs text-slate-500">Office</p>
                <p class="text-sm font-medium text-slate-900 dark:text-white">{{ user.office }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Main content -->
      <div class="lg:col-span-8 flex flex-col gap-8">
        <div class="bg-surface-light dark:bg-surface-dark p-6 rounded-2xl border border-stone-200 dark:border-stone-800 shadow-sm">
          <div class="flex items-center gap-3 mb-6">
            <div class="p-2 bg-primary/10 rounded-lg text-primary">
              <span class="material-symbols-outlined block">history_edu</span>
            </div>
            <h3 class="font-bold text-slate-900 dark:text-white">Professional Background</h3>
          </div>
          <div class="grid md:grid-cols-2 gap-6">
            <div>
              <h4 class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3">Degrees & Qualifications</h4>
              <p class="text-sm text-slate-700 dark:text-slate-300 whitespace-pre-line">{{ user.degreesQualification ?? '—' }}</p>
            </div>
            <div>
              <h4 class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3">Specializations</h4>
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="spec in (user.specializations as string[])"
                  :key="spec"
                  class="px-3 py-1 bg-stone-100 dark:bg-stone-800 text-slate-600 dark:text-slate-400 rounded-lg text-xs font-semibold"
                >
                  {{ spec }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-surface-light dark:bg-surface-dark rounded-2xl border border-stone-200 dark:border-stone-800 shadow-sm overflow-hidden">
          <div class="p-6 border-b border-stone-100 dark:border-stone-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
            <div class="flex items-center gap-3">
              <div class="p-2 bg-primary/10 rounded-lg text-primary">
                <span class="material-symbols-outlined block">class</span>
              </div>
              <h3 class="font-bold text-slate-900 dark:text-white">Currently Assigned Classes</h3>
            </div>
            <span class="bg-primary/10 text-primary text-xs font-bold px-3 py-1 rounded-full">{{ (user.classes as unknown[])?.length ?? 0 }} Active Classes</span>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full text-left">
              <thead class="bg-stone-50 dark:bg-stone-900/50">
                <tr>
                  <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-500">Class Name</th>
                  <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-500">Course Code</th>
                  <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-500">Students</th>
                  <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-500 text-right">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-stone-100 dark:divide-stone-800">
                <tr
                  v-for="cls in (user.classes as { name: string; code: string; students: number }[])"
                  :key="cls.code"
                  class="hover:bg-stone-50/50 dark:hover:bg-stone-800/50"
                >
                  <td class="px-6 py-4 text-sm font-semibold text-slate-900 dark:text-white">{{ cls.name }}</td>
                  <td class="px-6 py-4 text-sm text-slate-500">{{ cls.code }}</td>
                  <td class="px-6 py-4 text-sm text-slate-500">{{ cls.students }} Students</td>
                  <td class="px-6 py-4 text-right">
                    <a class="text-primary hover:text-primary-dark font-bold text-sm cursor-pointer">Manage Class</a>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="bg-surface-light dark:bg-surface-dark p-6 rounded-2xl border border-stone-200 dark:border-stone-800 shadow-sm">
          <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 mb-6">
            <div class="flex items-center gap-3">
              <div class="p-2 bg-primary/10 rounded-lg text-primary">
                <span class="material-symbols-outlined block">calendar_month</span>
              </div>
              <h3 class="font-bold text-slate-900 dark:text-white">Teaching Schedule Preview</h3>
            </div>
            <button class="text-sm font-bold text-primary hover:underline">View Full Calendar</button>
          </div>
          <div class="grid grid-cols-2 sm:grid-cols-5 gap-4 overflow-x-auto">
            <div
              v-for="day in (user.schedule as { day: string; slots: { time?: string; course?: string; type?: string; color?: string }[] }[])"
              :key="day.day"
              class="flex flex-col gap-3 min-w-[80px]"
            >
              <span class="text-[10px] font-bold text-slate-400 uppercase text-center">{{ day.day }}</span>
              <template v-for="(slot, si) in day.slots" :key="si">
                <div
                  v-if="slot.type === 'break'"
                  class="h-10 border border-dashed border-stone-200 dark:border-stone-800 rounded-lg flex items-center justify-center"
                >
                  <span class="text-[10px] text-slate-400 font-medium italic">Break</span>
                </div>
                <div
                  v-else
                  :class="[
                    'p-2 rounded-lg border-l-4',
                    slot.color === 'blue' ? 'bg-blue-50 dark:bg-blue-900/20 border-blue-500' : '',
                    slot.color === 'orange' ? 'bg-orange-50 dark:bg-orange-900/20 border-primary' : '',
                    slot.color === 'purple' ? 'bg-purple-50 dark:bg-purple-900/20 border-purple-500' : '',
                    slot.color === 'stone' ? 'bg-stone-100 dark:bg-stone-800 border-stone-400' : '',
                  ]"
                >
                  <p :class="['text-[10px] font-bold', slot.color === 'blue' ? 'text-blue-700 dark:text-blue-400' : slot.color === 'orange' ? 'text-primary' : slot.color === 'purple' ? 'text-purple-700 dark:text-purple-400' : 'text-slate-500']">{{ slot.time }}</p>
                  <p class="text-[11px] font-semibold text-slate-900 dark:text-white leading-tight mt-0.5 truncate">{{ slot.course }}</p>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
    </template>
  </div>

  <!-- Edit Profile Modal -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="showEditModal"
        class="fixed inset-0 z-110 flex items-center justify-center p-4 backdrop-blur-md bg-stone-900/40"
      >
        <div
          class="bg-surface-light dark:bg-surface-dark w-full max-w-2xl rounded-xl shadow-2xl overflow-hidden border border-stone-200 dark:border-stone-800 max-h-[90vh] flex flex-col"
        >
          <div class="bg-primary px-6 py-4 flex items-center justify-between shrink-0">
            <h2 class="text-white text-xl font-bold flex items-center gap-2">
              <span class="material-symbols-outlined">edit</span>
              Update Profile
            </h2>
            <button class="text-white/80 hover:text-white transition-colors" type="button" @click="closeEditModal">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <form class="p-6 overflow-y-auto space-y-6 flex-1" @submit.prevent="submitUpdate">
            <div
              v-if="editError"
              class="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 text-sm"
            >
              {{ editError }}
            </div>

            <div class="p-3 rounded-lg bg-stone-50 dark:bg-stone-900/40 border border-stone-200 dark:border-stone-800 text-sm text-slate-600 dark:text-slate-300">
              Email / Role / Status are read-only in this use case.
            </div>

            <!-- Teacher form -->
            <div v-if="detail?.teacherProfile" class="space-y-4">
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">First Name</label>
                  <input v-model="editTeacher.firstName" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="text" />
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Last Name</label>
                  <input v-model="editTeacher.lastName" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="text" />
                </div>
                <div class="space-y-1.5 sm:col-span-2">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Teacher Code</label>
                  <input v-model="editTeacher.teacherCode" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="text" placeholder="HJ170001" />
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Department</label>
                  <select v-model.number="editTeacher.departmentId" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm">
                    <option :value="null">— Select department —</option>
                    <option v-for="d in departments" :key="d.departmentId" :value="d.departmentId">{{ d.name }}</option>
                  </select>
                  <p v-if="departmentsLoading" class="text-xs text-slate-500 mt-1">Loading departments…</p>
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Phone</label>
                  <input v-model="editTeacher.phone" inputmode="numeric" @input="editTeacher.phone = editTeacher.phone.replace(/\\D/g, '')" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="tel" />
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Specialization</label>
                  <input v-model="editTeacher.specialization" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="text" />
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Academic Rank</label>
                  <input v-model="editTeacher.academicRank" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="text" />
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Office Room</label>
                  <input v-model="editTeacher.officeRoom" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="text" />
                </div>
                <div class="space-y-1.5 sm:col-span-2">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Degrees / Qualifications</label>
                  <textarea v-model="editTeacher.degreesQualification" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm resize-none" rows="3" />
                </div>
              </div>
            </div>

            <!-- Student form -->
            <div v-else-if="detail?.studentProfile" class="space-y-4">
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">First Name</label>
                  <input v-model="editStudent.firstName" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="text" />
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Last Name</label>
                  <input v-model="editStudent.lastName" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="text" />
                </div>
                <div class="space-y-1.5 sm:col-span-2">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Student Code</label>
                  <input v-model="editStudent.studentCode" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="text" placeholder="HE170001" />
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Department</label>
                  <select v-model.number="editStudent.departmentId" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm">
                    <option :value="null">— Select department —</option>
                    <option v-for="d in departments" :key="d.departmentId" :value="d.departmentId">{{ d.name }}</option>
                  </select>
                  <p v-if="departmentsLoading" class="text-xs text-slate-500 mt-1">Loading departments…</p>
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Phone</label>
                  <input v-model="editStudent.phone" inputmode="numeric" @input="editStudent.phone = editStudent.phone.replace(/\\D/g, '')" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="tel" />
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Date of Birth</label>
                  <input v-model="editStudent.dob" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="date" />
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Gender</label>
                  <select v-model="editStudent.gender" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm">
                    <option value="">— Select —</option>
                    <option value="MALE">Male</option>
                    <option value="FEMALE">Female</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Major</label>
                  <input v-model="editStudent.major" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="text" />
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Year</label>
                  <input v-model.number="editStudent.year" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="number" min="1" max="4" />
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Manage Class</label>
                  <input v-model="editStudent.manageClass" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="text" />
                </div>
                <div class="space-y-1.5 sm:col-span-2">
                  <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Address</label>
                  <input v-model="editStudent.address" class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm" type="text" />
                </div>
              </div>
            </div>

            <div class="flex items-center justify-end gap-3 pt-6 border-t border-stone-200 dark:border-stone-800 shrink-0">
              <button
                type="button"
                class="px-5 py-2.5 rounded-lg text-slate-600 dark:text-slate-400 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 font-bold transition-all"
                @click="closeEditModal"
              >
                Cancel
              </button>
              <button
                type="submit"
                :disabled="editLoading"
                class="px-5 py-2.5 rounded-lg bg-primary hover:bg-primary-dark text-white font-bold shadow-lg shadow-primary/20 transition-all active:scale-95 disabled:opacity-60 disabled:cursor-not-allowed"
              >
                {{ editLoading ? 'Saving…' : 'Save Changes' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- Block User Modal -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="showBlockModal"
        class="fixed inset-0 z-120 flex items-center justify-center p-4 backdrop-blur-md bg-stone-900/40"
      >
        <div
          class="bg-surface-light dark:bg-surface-dark w-full max-w-md rounded-xl shadow-2xl overflow-hidden border border-stone-200 dark:border-stone-800 max-h-[90vh] flex flex-col"
        >
          <div class="bg-red-600 px-6 py-4 flex items-center justify-between shrink-0">
            <h2 class="text-white text-lg font-bold flex items-center gap-2">
              <span class="material-symbols-outlined">block</span>
              Block User
            </h2>
            <button class="text-white/80 hover:text-white transition-colors" type="button" @click="showBlockModal = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <form class="p-6 space-y-4 flex-1" @submit.prevent="submitStatusChange('BLOCKED')">
            <div
              v-if="blockError"
              class="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 text-sm"
            >
              {{ blockError }}
            </div>
            <p class="text-sm text-slate-600 dark:text-slate-300">
              Please provide a reason for blocking this account. The user will be logged out from all devices.
            </p>
            <div class="space-y-1.5">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">
                Ban Reason <span class="text-red-500">*</span>
              </label>
              <textarea
                v-model="blockReason"
                rows="3"
                class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-red-500 focus:border-red-500 transition-all"
                placeholder="Vi phạm nội quy trường…"
              />
            </div>

            <div class="flex items-center justify-end gap-3 pt-4 border-t border-stone-200 dark:border-stone-800 shrink-0">
              <button
                type="button"
                class="px-4 py-2 rounded-lg text-slate-600 dark:text-slate-400 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-sm font-bold transition-all"
                @click="showBlockModal = false"
              >
                Cancel
              </button>
              <button
                type="submit"
                :disabled="blockLoading || !blockReason.trim()"
                class="px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 text-white text-sm font-bold shadow-lg shadow-red-600/30 transition-all active:scale-95 disabled:opacity-60 disabled:cursor-not-allowed"
              >
                {{ blockLoading ? 'Blocking…' : 'Confirm Block' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- Update Profile Result Modal -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="showUpdateResultModal"
        class="fixed inset-0 z-130 flex items-center justify-center p-4 backdrop-blur-md bg-stone-900/40"
      >
        <div
          class="bg-surface-light dark:bg-surface-dark w-full max-w-md rounded-xl shadow-2xl overflow-hidden border border-stone-200 dark:border-stone-800 max-h-[80vh] flex flex-col"
        >
          <div
            :class="[
              'px-6 py-4 flex items-center justify-between shrink-0',
              updateResultSuccess ? 'bg-emerald-600' : 'bg-red-600',
            ]"
          >
            <h2 class="text-white text-lg font-bold flex items-center gap-2">
              <span class="material-symbols-outlined">
                {{ updateResultSuccess ? 'check_circle' : 'error' }}
              </span>
              {{ updateResultSuccess ? 'Profile Updated' : 'Update Failed' }}
            </h2>
            <button class="text-white/80 hover:text-white transition-colors" type="button" @click="showUpdateResultModal = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div class="p-6 flex-1 flex items-center">
            <p class="text-sm text-slate-700 dark:text-slate-200">
              {{ updateResultMessage }}
            </p>
          </div>

          <div class="px-6 py-4 border-t border-stone-200 dark:border-stone-800 bg-stone-50/60 dark:bg-stone-900/40 shrink-0 flex justify-end">
            <button
              type="button"
              class="px-5 py-2 rounded-lg bg-primary hover:bg-primary-dark text-white text-sm font-bold shadow-md shadow-primary/30 transition-all active:scale-95"
              @click="showUpdateResultModal = false"
            >
              OK
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
