<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  getAdminClassDetail,
  type AdminClassDetail,
  enrollStudent,
  getEligibleStudents,
  unenrollStudent,
  type AdminEligibleStudent,
  type AdminClassStudent,
} from '@/services/adminClassService'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const { toast, showToast } = useToast()
const classId = Number(route.params.classId)

const isLoading = ref(false)
const classInfo = ref<AdminClassDetail | null>(null)
const searchQuery = ref('')
const showAddStudentModal = ref(false)

// Student Search state
const studentSearchQuery = ref('')
const searchLoading = ref(false)
const eligibleStudents = ref<AdminEligibleStudent[]>([])
const selectedStudent = ref<AdminEligibleStudent | null>(null)
const enrollLoading = ref(false)

// Unenrollment state
const showUnenrollModal = ref(false)
const studentToUnenroll = ref<AdminClassStudent | null>(null)
const unenrollLoading = ref(false)

const searchResults = computed(() => {
  const query = studentSearchQuery.value.toLowerCase().trim()
  if (!query) return eligibleStudents.value
  return eligibleStudents.value.filter(
    (s) =>
      s.fullName.toLowerCase().includes(query) ||
      s.studentCode.toLowerCase().includes(query) ||
      s.email.toLowerCase().includes(query),
  )
})

async function fetchClassDetail() {
  try {
    isLoading.value = true
    classInfo.value = await getAdminClassDetail(classId)
  } catch (err) {
    showToast(err instanceof Error ? err.message : 'Failed to load class details', 'error')
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchClassDetail()
})

watch(showAddStudentModal, async (val) => {
  if (val) {
    try {
      searchLoading.value = true
      eligibleStudents.value = await getEligibleStudents(classId)
    } catch {
      showToast('Failed to fetch eligible students', 'error')
    } finally {
      searchLoading.value = false
    }
  } else {
    // Reset state
    studentSearchQuery.value = ''
    selectedStudent.value = null
  }
})

async function handleEnroll() {
  if (!selectedStudent.value) return

  try {
    enrollLoading.value = true
    await enrollStudent({
      classId: classId,
      studentId: selectedStudent.value.userId,
    })

    showToast('Student enrolled successfully', 'success')
    showAddStudentModal.value = false
    studentSearchQuery.value = ''
    selectedStudent.value = null
    fetchClassDetail()
  } catch (err) {
    showToast(err instanceof Error ? err.message : 'Failed to enroll student', 'error')
  } finally {
    enrollLoading.value = false
  }
}

function confirmUnenroll(student: AdminClassStudent) {
  studentToUnenroll.value = student
  showUnenrollModal.value = true
}

async function handleUnenroll() {
  if (!studentToUnenroll.value) return

  try {
    unenrollLoading.value = true
    await unenrollStudent(classId, studentToUnenroll.value.studentId)
    showToast('Student removed from class', 'success')
    showUnenrollModal.value = false
    studentToUnenroll.value = null
    fetchClassDetail()
  } catch (err) {
    showToast(err instanceof Error ? err.message : 'Failed to remove student', 'error')
  } finally {
    unenrollLoading.value = false
  }
}

const filteredStudents = computed(() => {
  if (!classInfo.value?.students) return []
  const query = searchQuery.value.toLowerCase().trim()
  if (!query) return classInfo.value.students

  return classInfo.value.students.filter(
    (s) =>
      s.fullName.toLowerCase().includes(query) ||
      s.studentCode.toLowerCase().includes(query) ||
      s.email.toLowerCase().includes(query),
  )
})

function formatDate(dateString: string): string {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

const formattedSchedules = computed(() => {
  if (!classInfo.value?.sessions || classInfo.value.sessions.length === 0) return 'N/A'
  return classInfo.value.sessions.map(s => `T${s.dayOfWeek} ${s.startTime.slice(0, 5)}-${s.endTime.slice(0, 5)}`).join(', ')
})

const formattedRooms = computed(() => {
  if (!classInfo.value?.sessions || classInfo.value.sessions.length === 0) return 'N/A'
  const rooms = new Set(classInfo.value.sessions.map(s => s.roomName))
  return Array.from(rooms).join(', ')
})
</script>

<template>
  <div class="max-w-[1400px] w-full mx-auto p-6 md:p-10 flex flex-col gap-8">
    <!-- Breadcrumbs & Header -->
    <div class="flex flex-col gap-6" v-if="classInfo">
      <div class="flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
        <router-link :to="{ name: 'admin-classes' }" class="hover:text-primary transition-colors"
          >Classes</router-link
        >
        <span class="material-symbols-outlined text-xs">chevron_right</span>
        <span class="font-medium text-slate-900 dark:text-white">#CL-{{ classInfo.classId }}</span>
      </div>
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1
            class="text-slate-900 dark:text-white text-3xl font-bold leading-tight tracking-tight"
          >
            [{{ classInfo.courseCode }}] {{ classInfo.courseName }}
          </h1>
          <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">
            {{ classInfo.semesterName }} • Class ID: #CL-{{ classInfo.classId }}
          </p>
        </div>
        <div class="flex items-center gap-3">
          <button
            @click="showAddStudentModal = true"
            class="flex items-center gap-2 px-6 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95"
          >
            <span class="material-symbols-outlined text-[20px]">person_add</span>
            Add Student
          </button>
        </div>
      </div>
    </div>

    <!-- Info Cards -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6" v-if="classInfo">
      <div
        class="lg:col-span-2 grid grid-cols-1 sm:grid-cols-3 gap-4 bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm"
      >
        <div class="flex flex-col gap-1">
          <span
            class="text-xs uppercase tracking-wider font-bold text-slate-500 dark:text-slate-400"
            >Teacher</span
          >
          <div class="flex items-center gap-2 mt-1">
            <div
              class="w-8 h-8 rounded-full bg-stone-100 dark:bg-stone-800 flex items-center justify-center"
            >
              <span class="material-symbols-outlined text-slate-400 text-[20px]">person</span>
            </div>
            <span class="font-semibold text-slate-900 dark:text-white">{{
              classInfo.teacherName
            }}</span>
          </div>
        </div>
        <div class="flex flex-col gap-1 border-stone-100 dark:border-stone-800 sm:border-l sm:pl-4">
          <span
            class="text-xs uppercase tracking-wider font-bold text-slate-500 dark:text-slate-400"
            >Schedule</span
          >
          <div class="flex items-center gap-2 mt-1">
            <span class="material-symbols-outlined text-primary text-[20px]">schedule</span>
            <span class="font-medium text-slate-900 dark:text-white">{{ formattedSchedules }}</span>
          </div>
        </div>
        <div class="flex flex-col gap-1 border-stone-100 dark:border-stone-800 sm:border-l sm:pl-4">
          <span
            class="text-xs uppercase tracking-wider font-bold text-slate-500 dark:text-slate-400"
            >Room</span
          >
          <div class="flex items-center gap-2 mt-1">
            <span class="material-symbols-outlined text-primary text-[20px]">meeting_room</span>
            <span class="font-medium text-slate-900 dark:text-white">{{
              formattedRooms
            }}</span>
          </div>
        </div>
      </div>
      <div
        class="bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex flex-col justify-center"
      >
        <div class="flex items-center justify-between mb-2">
          <span class="text-sm font-semibold text-slate-700 dark:text-slate-200"
            >Class Capacity</span
          >
          <span class="text-sm font-bold text-primary"
            >{{ classInfo.studentCount }}/{{ classInfo.maxStudents }} Filled</span
          >
        </div>
        <div class="w-full bg-stone-100 dark:bg-stone-800 rounded-full h-3 overflow-hidden">
          <div
            class="bg-primary h-3 rounded-full transition-all duration-500"
            :style="{
              width: Math.min((classInfo.studentCount / classInfo.maxStudents) * 100, 100) + '%',
            }"
          ></div>
        </div>
        <p class="text-xs text-slate-500 dark:text-slate-400 mt-2">
          Only {{ Math.max(classInfo.maxStudents - classInfo.studentCount, 0) }} seats remaining for
          this semester.
        </p>
      </div>
    </div>

    <!-- Students Table -->
    <div
      v-if="classInfo"
      class="@container w-full overflow-hidden rounded-xl border border-stone-200 dark:border-stone-800 bg-surface-light dark:bg-surface-dark shadow-sm"
    >
      <div
        class="p-4 border-b border-stone-200 dark:border-stone-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
      >
        <h3 class="font-bold text-lg text-slate-900 dark:text-white">
          Enrolled Students ({{ classInfo.studentCount }})
        </h3>
        <div class="relative w-full sm:w-64">
          <span
            class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
            >search</span
          >
          <input
            v-model="searchQuery"
            class="w-full pl-10 pr-4 h-10 bg-stone-50 dark:bg-stone-900 border-stone-200 dark:border-stone-700 rounded-lg text-sm transition-all focus:ring-2 focus:ring-primary focus:border-primary text-slate-900 dark:text-white"
            placeholder="Search student code or name..."
            type="text"
          />
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr
              class="bg-stone-50 dark:bg-stone-900/50 border-b border-stone-200 dark:border-stone-800"
            >
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Student ID
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Student Name
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Email
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Enrollment Date
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
              v-for="student in filteredStudents"
              :key="student.studentId"
              class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors"
            >
              <td class="p-4 text-sm font-medium text-slate-500 whitespace-nowrap">
                {{ student.studentCode }}
              </td>
              <td class="p-4 whitespace-nowrap">
                <div class="flex items-center gap-3">
                  <div
                    class="w-8 h-8 rounded-full bg-indigo-100 dark:bg-indigo-900/20 text-indigo-600 flex items-center justify-center font-bold overflow-hidden border border-indigo-200/20"
                  >
                    <span class="material-symbols-outlined text-lg">person</span>
                  </div>
                  <span class="text-sm font-bold text-slate-900 dark:text-white">{{
                    student.fullName
                  }}</span>
                </div>
              </td>
              <td class="p-4 text-sm text-slate-600 dark:text-slate-400">{{ student.email }}</td>
              <td class="p-4 text-sm text-slate-600 dark:text-slate-400">
                {{ formatDate(student.enrollmentDate) }}
              </td>
              <td class="p-4 text-right whitespace-nowrap">
                <button
                  @click="confirmUnenroll(student)"
                  class="text-red-500 hover:text-red-700 dark:hover:text-red-400 text-xs font-medium border border-red-200 dark:border-red-900/30 bg-red-50 dark:bg-red-900/10 px-3 py-1.5 rounded-md transition-colors"
                >
                  Remove
                </button>
              </td>
            </tr>
            <tr v-if="filteredStudents.length === 0">
              <td colspan="5" class="p-8 text-center text-slate-500">
                No students found in this class.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="flex flex-col items-center justify-center py-20 gap-4">
      <div
        class="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin"
      ></div>
      <p class="text-slate-500 font-medium">Loading class details...</p>
    </div>

    <!-- Toast Notification -->
    <Teleport to="body">
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

      <!-- Add Student Modal -->
      <div
        v-if="showAddStudentModal"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
      >
        <div
          class="absolute inset-0 bg-slate-900/60 backdrop-blur-sm"
          @click="showAddStudentModal = false"
        ></div>
        <div
          class="relative w-full max-w-lg bg-surface-light dark:bg-surface-dark rounded-2xl shadow-2xl border border-stone-200 dark:border-stone-800 overflow-hidden"
        >
          <div
            class="p-6 border-b border-stone-100 dark:border-stone-800 flex items-center justify-between"
          >
            <h3 class="text-xl font-bold text-slate-900 dark:text-white">Enroll New Student</h3>
            <button
              @click="showAddStudentModal = false"
              class="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition-colors"
            >
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div class="p-6 flex flex-col gap-6">
            <div class="flex flex-col gap-2">
              <label class="text-xs font-bold uppercase tracking-wider text-slate-500"
                >Search Student</label
              >
              <div class="relative">
                <span
                  class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                  >search</span
                >
                <input
                  v-model="studentSearchQuery"
                  class="w-full pl-10 pr-4 h-11 bg-stone-50 dark:bg-stone-900 border border-stone-200 dark:border-stone-700 rounded-xl text-sm transition-all focus:ring-2 focus:ring-primary focus:ring-offset-0 text-slate-900 dark:text-white"
                  placeholder="Enter student code, name or email..."
                  type="text"
                />
              </div>
            </div>

            <div class="flex flex-col gap-2 max-h-60 overflow-y-auto pr-2 custom-scrollbar">
              <div v-if="searchLoading" class="flex items-center justify-center py-4">
                <div
                  class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"
                ></div>
              </div>

              <template v-else-if="searchResults.length > 0">
                <div
                  v-for="student in searchResults"
                  :key="student.userId"
                  @click="selectedStudent = student"
                  class="p-3 rounded-xl border transition-all cursor-pointer flex items-center gap-3"
                  :class="
                    selectedStudent?.userId === student.userId
                      ? 'border-primary bg-primary/5 shadow-sm'
                      : 'border-transparent hover:bg-stone-50 dark:hover:bg-stone-800'
                  "
                >
                  <div
                    class="w-10 h-10 rounded-full bg-stone-100 dark:bg-stone-800 flex items-center justify-center font-bold text-slate-500"
                  >
                    {{ student.fullName?.[0] || 'S' }}
                  </div>
                  <div class="flex flex-col">
                    <span class="text-sm font-bold text-slate-900 dark:text-white">{{
                      student.fullName
                    }}</span>
                    <span class="text-xs text-slate-500"
                      >{{ student.email }} • {{ student.studentCode }}</span
                    >
                  </div>
                  <div
                    v-if="selectedStudent?.userId === student.userId"
                    class="ml-auto text-primary"
                  >
                    <span class="material-symbols-outlined">check_circle</span>
                  </div>
                </div>
              </template>

              <div
                v-else-if="studentSearchQuery.length >= 2"
                class="py-10 text-center text-slate-500"
              >
                <span class="material-symbols-outlined text-4xl mb-2 opacity-20">person_off</span>
                <p class="text-sm">No students found matching your search.</p>
              </div>
            </div>
          </div>

          <div
            class="p-6 bg-stone-50 dark:bg-stone-900/50 border-t border-stone-100 dark:border-stone-800 flex items-center justify-end gap-3"
          >
            <button
              @click="showAddStudentModal = false"
              class="px-4 py-2 text-sm font-bold text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors"
            >
              Cancel
            </button>
            <button
              @click="handleEnroll"
              :disabled="!selectedStudent || enrollLoading"
              class="px-6 py-2 bg-primary hover:bg-primary-dark disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95 flex items-center gap-2"
            >
              <span
                v-if="enrollLoading"
                class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"
              ></span>
              Enroll Student
            </button>
          </div>
        </div>
      </div>

      <!-- Unenroll Confirmation Modal -->
      <div v-if="showUnenrollModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div
          class="absolute inset-0 bg-slate-900/60 backdrop-blur-sm"
          @click="showUnenrollModal = false"
        ></div>
        <div
          class="relative w-full max-w-md bg-surface-light dark:bg-surface-dark rounded-2xl shadow-2xl border border-stone-200 dark:border-stone-800 overflow-hidden"
        >
          <div class="p-6 text-center">
            <div
              class="w-16 h-16 bg-red-50 dark:bg-red-900/20 text-red-500 rounded-full flex items-center justify-center mx-auto mb-4"
            >
              <span class="material-symbols-outlined text-3xl">person_remove</span>
            </div>
            <h3 class="text-xl font-bold text-slate-900 dark:text-white mb-2">Remove Student?</h3>
            <p class="text-slate-500 dark:text-slate-400 text-sm">
              Are you sure you want to remove
              <span class="font-bold text-slate-900 dark:text-white">{{
                studentToUnenroll?.fullName
              }}</span>
              from this class? This action cannot be undone.
            </p>
          </div>
          <div
            class="p-6 bg-stone-50 dark:bg-stone-900/50 border-t border-stone-100 dark:border-stone-800 flex items-center justify-center gap-3"
          >
            <button
              @click="showUnenrollModal = false"
              class="px-6 py-2 text-sm font-bold text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors"
            >
              Cancel
            </button>
            <button
              @click="handleUnenroll"
              :disabled="unenrollLoading"
              class="px-8 py-2 bg-red-500 hover:bg-red-600 disabled:opacity-50 text-white rounded-lg font-bold shadow-lg shadow-red-500/20 transition-all active:scale-95 flex items-center gap-2"
            >
              <span
                v-if="unenrollLoading"
                class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"
              ></span>
              Remove Student
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.toast-enter-from {
  transform: translateX(100%) scale(0.9);
  opacity: 0;
}
.toast-leave-to {
  transform: translateX(100%) scale(0.9);
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
