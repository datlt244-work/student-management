<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { gradeService, type StudentGrade } from '@/services/gradeService'
import { semesterService, type SemesterResponse } from '@/services/semesterService'
import { getMyProfile, type CombinedProfile } from '@/services/profileService'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const isLoading = ref(true)
const semesters = ref<SemesterResponse[]>([])
const allSemesterGrades = ref<Record<number, StudentGrade[]>>({})
const selectedGrade = ref<StudentGrade | null>(null)
const expandedSemesters = ref<Set<number>>(new Set())
const profile = ref<CombinedProfile | null>(null)

const userFullName = computed(() => {
  const stu = profile.value?.studentProfile
  if (stu) return `${stu.firstName} ${stu.lastName}`
  return 'Student'
})

const userCode = computed(() => {
  return profile.value?.studentProfile?.studentCode || authStore.user?.email || ''
})

async function fetchInitialData() {
  try {
    isLoading.value = true
    
    // Fetch profile and semesters in parallel
    const [semRes, profRes] = await Promise.all([
      semesterService.getAllSemesters(),
      getMyProfile()
    ])
    
    semesters.value = semRes.result || []
    profile.value = profRes || null

    if (semesters.value.length > 0) {
      // Mặc định expand kỳ hiện tại
      const current = semesters.value.find((s) => s.isCurrent) || semesters.value[0]
      if (current) {
        toggleSemester(current.semesterId)
      }
    }
  } catch (error) {
    console.error('Failed to fetch semesters', error)
  } finally {
    isLoading.value = false
  }
}

async function fetchGradesForSemester(semesterId: number) {
  if (allSemesterGrades.value[semesterId]) return
  try {
    const res = await gradeService.getGradesBySemester(semesterId)
    allSemesterGrades.value[semesterId] = res.result || []
  } catch (error) {
    console.error(`Failed to fetch grades for semester ${semesterId}`, error)
  }
}

async function toggleSemester(semesterId: number) {
  if (expandedSemesters.value.has(semesterId)) {
    expandedSemesters.value.delete(semesterId)
  } else {
    expandedSemesters.value.add(semesterId)
    await fetchGradesForSemester(semesterId)
  }
}

function selectCourse(grade: StudentGrade) {
  selectedGrade.value = grade
}

function formatDate(dateStr: string) {
  if (!dateStr) return 'N/A'
  const date = new Date(dateStr)
  return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

onMounted(() => {
  fetchInitialData()
})
</script>

<template>
  <div class="flex-1 flex flex-col w-full px-4 sm:px-6 py-8 bg-gray-50/50 dark:bg-gray-950/20">
    <!-- Header: Grade Report for User -->
    <div class="max-w-7xl mx-auto w-full mb-8">
      <h1 class="text-3xl font-normal text-text-main-light dark:text-text-main-dark">
        Grade report for {{ userFullName }} ({{ userCode }})
      </h1>
    </div>

    <div v-if="isLoading" class="flex-1 flex flex-col items-center justify-center py-20">
      <div class="animate-spin size-12 border-4 border-primary border-t-transparent rounded-full mb-4"></div>
      <p class="text-text-muted-light dark:text-text-muted-dark animate-pulse">
        Loading academic records...
      </p>
    </div>

    <div v-else class="max-w-7xl mx-auto w-full grid grid-cols-1 lg:grid-cols-12 gap-8">
      <!-- Left Column: Term & Course Selection -->
      <div class="lg:col-span-5 flex flex-col gap-4">
        <div class="overflow-hidden border border-gray-200 dark:border-gray-800 rounded-lg shadow-sm bg-white dark:bg-surface-dark">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="bg-[#6b8cd9] text-white">
                <th class="px-4 py-2 text-sm font-bold uppercase w-1/4">Term</th>
                <th class="px-4 py-2 text-sm font-bold uppercase">Course</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100 dark:divide-gray-800">
              <template v-for="sem in semesters" :key="sem.semesterId">
                <tr 
                  @click="toggleSemester(sem.semesterId)"
                  class="cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-900 transition-colors"
                  :class="{ 'bg-gray-50 dark:bg-gray-900': expandedSemesters.has(sem.semesterId) }"
                >
                  <td class="px-4 py-3 text-sm font-medium text-primary align-top">
                    <div class="flex items-center gap-1">
                      <span class="material-symbols-outlined text-xs transition-transform" :class="{ 'rotate-90': expandedSemesters.has(sem.semesterId) }">
                        chevron_right
                      </span>
                      {{ sem.displayName }}
                    </div>
                  </td>
                  <td class="px-4 py-3 text-sm text-gray-400 italic">
                    {{ expandedSemesters.has(sem.semesterId) ? '' : 'Click to view courses' }}
                  </td>
                </tr>
                
                <!-- Courses under the semester -->
                <tr v-if="expandedSemesters.has(sem.semesterId) && allSemesterGrades[sem.semesterId]?.length === 0">
                  <td colspan="2" class="px-8 py-2 text-xs text-text-muted-light dark:text-text-muted-dark">
                    No courses enrolled in this term.
                  </td>
                </tr>
                <tr 
                  v-for="grade in allSemesterGrades[sem.semesterId]" 
                  :key="grade.enrollmentId"
                  v-show="expandedSemesters.has(sem.semesterId)"
                  @click="selectCourse(grade)"
                  class="group cursor-pointer hover:bg-primary/5 transition-colors"
                  :class="{ 'bg-primary/10 border-l-4 border-l-primary': selectedGrade?.enrollmentId === grade.enrollmentId }"
                >
                  <td class="px-4"></td>
                  <td class="px-4 py-3">
                    <div class="text-sm font-medium" :class="selectedGrade?.enrollmentId === grade.enrollmentId ? 'text-primary' : 'text-blue-600 dark:text-blue-400 group-hover:underline'">
                      {{ grade.courseName }} ({{ grade.courseCode }})
                    </div>
                    <div class="text-[11px] text-text-muted-light dark:text-text-muted-dark mt-1">
                      ({{ grade.classCode }}, from {{ formatDate(grade.fromDate) }} - {{ formatDate(grade.toDate) }})
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Right Column: Detailed Grade Report -->
      <div v-if="!selectedGrade" class="lg:col-span-7 flex flex-col items-center justify-center border-2 border-dashed border-gray-200 dark:border-gray-800 rounded-xl p-12 text-center bg-white/50 dark:bg-surface-dark/50 min-h-[400px]">
        <div class="size-20 rounded-full bg-primary/5 flex items-center justify-center text-primary mb-6">
          <span class="material-symbols-outlined !text-5xl">ads_click</span>
        </div>
        <h2 class="text-2xl font-bold text-text-main-light dark:text-text-main-dark mb-2">Select a course to view details</h2>
        <p class="text-text-muted-light dark:text-text-muted-dark max-w-sm">
          Choose a semester and a course from the left panel to see your detailed grade report and instructor feedback.
        </p>
      </div>

      <section v-else class="lg:col-span-7 flex flex-col gap-6 animate-in fade-in slide-in-from-right-4 duration-500">
        <div class="bg-white dark:bg-background-dark/40 rounded-xl border border-primary/10 shadow-sm overflow-hidden">
          <!-- Table Header Section -->
          <div class="p-6 border-b border-primary/10 flex flex-wrap justify-between items-center gap-4">
          <h2 class="text-2xl font-bold text-slate-900 dark:text-slate-100">
            {{ selectedGrade?.courseName }} - {{ selectedGrade?.courseCode }}
          </h2>
          <div class="flex gap-2">
          <span 
            class="px-3 py-1 text-xs font-bold rounded-full uppercase tracking-wider"
            :class="selectedGrade?.status === 'PASSED' ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400' : 
                    selectedGrade?.status === 'NOT PASSED' ? 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400' : 
                    'bg-slate-100 dark:bg-slate-900/30 text-slate-700 dark:text-slate-400'"
          >
            {{ selectedGrade?.status || 'IN PROGRESS' }}
          </span>
          <span class="px-3 py-1 bg-primary/10 text-primary text-xs font-bold rounded-full uppercase tracking-wider">
            {{ selectedGrade?.credits }} Credits
          </span>
          </div>
          </div>
          <!-- Grade Table -->
          <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse">
          <thead>
          <tr class="bg-slate-50 dark:bg-background-dark/60 text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider">
          <th class="px-6 py-4">Grade Category</th>
          <th class="px-6 py-4">Grade Item</th>
          <th class="px-6 py-4 text-center">Weight</th>
          <th class="px-6 py-4 text-right">Value</th>
          </tr>
          </thead>
          <tbody class="divide-y divide-primary/5">
          <!-- Nếu có dữ liệu thật -->
          <template v-if="selectedGrade?.assessmentScores && selectedGrade.assessmentScores.length > 0">
            <tr 
              v-for="(score, index) in selectedGrade.assessmentScores" 
              :key="index"
              class="hover:bg-primary/5 dark:hover:bg-primary/5 transition-colors"
            >
              <td class="px-6 py-4 text-sm font-medium text-slate-700 dark:text-slate-300">
                {{ score.isTotal ? '' : score.category }}
              </td>
              <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-400" :class="{ 'font-bold underline text-primary': score.isTotal }">
                {{ score.itemName }}
              </td>
              <td class="px-6 py-4 text-sm text-center font-medium">{{ score.weight }}%</td>
              <td class="px-6 py-4 text-right">
                <span 
                  v-if="score.value !== null"
                  class="inline-flex items-center justify-center min-w-[3rem] h-8 px-3 rounded-lg bg-primary/10 text-primary font-bold"
                >
                  {{ score.value }}
                </span>
                <span v-else class="text-slate-400">-</span>
              </td>
            </tr>
          </template>
          <!-- Mock Data fallback if empty (exactly matching template) -->
          <!-- Default structure if no detailed assessment data yet -->
          <template v-else>
            <tr v-for="(item, idx) in [
              { cat: 'Participation', name: 'Class Participation', w: 10 },
              { cat: 'Progress Test', name: 'Test 1', w: 20 },
              { cat: 'Assignment', name: 'Project Report', w: 30 },
              { cat: 'Final Exam', name: 'Final Examination', w: 40 }
            ]" :key="idx" class="hover:bg-primary/5 dark:hover:bg-primary/5 transition-colors">
              <td class="px-6 py-4 text-sm font-medium text-slate-700 dark:text-slate-300">{{ item.cat }}</td>
              <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-400">{{ item.name }}</td>
              <td class="px-6 py-4 text-sm text-center font-medium">{{ item.w }}%</td>
              <td class="px-6 py-4 text-right">
                <span class="text-slate-400 font-medium">-</span>
              </td>
            </tr>
          </template>
          </tbody>
          <tfoot class="bg-slate-50 dark:bg-background-dark/60 border-t-2 border-primary/10">
          <tr>
          <td class="px-6 py-5" colspan="2">
          <div class="flex items-center gap-2">
          <span class="text-sm font-bold uppercase tracking-widest text-slate-500 dark:text-slate-400">Course Total</span>
          <span class="material-symbols-outlined text-primary text-xl">verified</span>
          </div>
          </td>
          <td class="px-6 py-5 text-center">
          <div class="flex flex-col items-center">
            <span class="text-xs text-slate-500 uppercase font-semibold">Average</span>
            <span class="text-lg font-extrabold text-primary">{{ selectedGrade?.grade ?? '-' }}</span>
          </div>
          </td>
          <td class="px-6 py-5 text-right">
          <div class="flex flex-col items-end">
          <span class="text-xs text-slate-500 uppercase font-semibold">Status</span>
          <span 
            class="text-sm font-black uppercase tracking-widest"
            :class="selectedGrade?.status === 'NOT PASSED' ? 'text-red-600 dark:text-red-400' : 'text-green-600 dark:text-green-400'"
          >
            {{ selectedGrade?.status || 'IN PROGRESS' }}
          </span>
          </div>
          </td>
          </tr>
          </tfoot>
          </table>
          </div>
        </div>
        <!-- Summary/Feedback Section -->
        <div class="mt-6 grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="p-5 bg-white dark:bg-background-dark/40 rounded-xl border border-primary/10">
          <div class="flex items-center gap-3 mb-3">
          <span class="material-symbols-outlined text-primary">comment</span>
          <h4 class="font-bold text-slate-900 dark:text-slate-100">Instructor Feedback</h4>
          </div>
          <p class="text-sm text-slate-600 dark:text-slate-400 leading-relaxed italic">
            <template v-if="selectedGrade?.feedback">
              "{{ selectedGrade.feedback }}"
            </template>
            <template v-else>
              No feedback available for this course yet.
            </template>
          </p>
          </div>
          <div class="p-5 bg-white dark:bg-background-dark/40 rounded-xl border border-primary/10">
          <div class="flex items-center gap-3 mb-3">
          <span class="material-symbols-outlined text-primary">analytics</span>
          <h4 class="font-bold text-slate-900 dark:text-slate-100">Performance Summary</h4>
          </div>
          <div class="space-y-3">
          <div>
          <div class="flex justify-between text-xs mb-1">
            <span class="text-slate-500">Progress to Target (10.0)</span>
            <span class="text-primary font-bold">{{ selectedGrade?.grade ? Math.round((Number(selectedGrade.grade) / 10) * 100) : 0 }}%</span>
          </div>
          <div class="w-full h-2 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
            <div class="h-full bg-primary transition-all duration-500" :style="{ width: (selectedGrade?.grade ? (Number(selectedGrade.grade) / 10) * 100 : 0) + '%' }"></div>
          </div>
          </div>
          </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* Custom table styles to match image */
table th {
  letter-spacing: 0.05em;
}

.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 20;
}
</style>
