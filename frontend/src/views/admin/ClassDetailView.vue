<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getAdminClassDetail, type AdminClassDetail } from '@/services/adminClassService'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const { showToast } = useToast()
const classId = Number(route.params.classId)

const isLoading = ref(false)
const classInfo = ref<AdminClassDetail | null>(null)
const searchQuery = ref('')
const showAddStudentModal = ref(false)

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
            <span class="font-medium text-slate-900 dark:text-white">{{ classInfo.schedule }}</span>
          </div>
        </div>
        <div class="flex flex-col gap-1 border-stone-100 dark:border-stone-800 sm:border-l sm:pl-4">
          <span
            class="text-xs uppercase tracking-wider font-bold text-slate-500 dark:text-slate-400"
            >Room</span
          >
          <div class="flex items-center gap-2 mt-1">
            <span class="material-symbols-outlined text-primary text-[20px]">meeting_room</span>
            <span class="font-medium text-slate-900 dark:text-white">{{ classInfo.roomNumber }}</span>
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
            :style="{ width: Math.min((classInfo.studentCount / classInfo.maxStudents) * 100, 100) + '%' }"
          ></div>
        </div>
        <p class="text-xs text-slate-500 dark:text-slate-400 mt-2">
          Only {{ Math.max(classInfo.maxStudents - classInfo.studentCount, 0) }} seats remaining for this semester.
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
        <h3 class="font-bold text-lg text-slate-900 dark:text-white">Enrolled Students ({{ classInfo.studentCount }})</h3>
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
      <div class="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
      <p class="text-slate-500 font-medium">Loading class details...</p>
    </div>
  </div>
</template>
