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
      <div class="lg:col-span-7 flex flex-col gap-6">
        <div v-if="!selectedGrade" class="h-full min-h-[400px] flex flex-col items-center justify-center border-2 border-dashed border-gray-200 dark:border-gray-800 rounded-xl p-12 text-center bg-white/50 dark:bg-surface-dark/50">
          <div class="size-20 rounded-full bg-primary/5 flex items-center justify-center text-primary mb-6">
            <span class="material-symbols-outlined !text-5xl">ads_click</span>
          </div>
          <h2 class="text-2xl font-bold text-text-main-light dark:text-text-main-dark mb-2">Select a term, course ...</h2>
          <p class="text-text-muted-light dark:text-text-muted-dark max-w-sm">
            Choose a semester and a course from the left panel to view detailed grade assessment.
          </p>
        </div>

        <div v-else class="flex flex-col gap-6 animate-in fade-in slide-in-from-right-4 duration-500">

          <div class="overflow-hidden border border-gray-200 dark:border-gray-800 rounded-lg shadow-md bg-white dark:bg-surface-dark">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-[#6b8cd9] text-white">
                  <th class="px-4 py-2 text-xs font-bold uppercase">Grade Category</th>
                  <th class="px-4 py-2 text-xs font-bold uppercase">Grade Item</th>
                  <th class="px-4 py-2 text-xs font-bold uppercase text-right">Weight</th>
                  <th class="px-4 py-2 text-xs font-bold uppercase text-right">Value</th>
                  <th class="px-4 py-2 text-xs font-bold uppercase">Comment</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100 dark:divide-gray-800">
                <tr 
                  v-for="(score, index) in selectedGrade.assessmentScores" 
                  :key="index"
                  :class="{ 'bg-gray-50/80 dark:bg-gray-900/40 font-bold': score.isTotal }"
                >
                  <td class="px-4 py-3 text-sm text-primary">
                    {{ score.category }}
                  </td>
                  <td class="px-4 py-3 text-sm text-text-main-light dark:text-text-main-dark">
                    {{ score.itemName }}
                  </td>
                  <td class="px-4 py-3 text-sm text-right text-text-muted-light dark:text-text-muted-dark">
                    {{ score.weight.toFixed(1) }} %
                  </td>
                  <td class="px-4 py-3 text-sm text-right font-medium" :class="{ 'text-primary': score.isTotal }">
                    {{ score.value !== null ? score.value.toFixed(1) : '-' }}
                  </td>
                  <td class="px-4 py-3 text-sm text-text-muted-light dark:text-text-muted-dark italic">
                    {{ score.comment || '' }}
                  </td>
                </tr>

                <!-- Course Total Section -->
                <tr class="bg-gray-50 dark:bg-gray-900 border-t-2 border-gray-200 dark:border-gray-800">
                  <td colspan="2" class="px-4 py-4 text-lg font-black text-gray-800 dark:text-gray-200 uppercase">
                    Course Total
                  </td>
                  <td class="px-4 py-4 text-right">
                    <span class="text-sm font-bold text-gray-400 uppercase mr-2">Average</span>
                    <span class="text-xl font-black text-gray-800 dark:text-gray-200">{{ selectedGrade.grade?.toFixed(1) || '-' }}</span>
                  </td>
                  <td colspan="2" class="px-4 py-4 text-right">
                    <span class="text-sm font-bold text-gray-400 uppercase mr-2">Status</span>
                    <span 
                      class="text-xl font-black"
                      :class="{
                        'text-green-600': selectedGrade.status === 'PASSED',
                        'text-red-600': selectedGrade.status === 'FAILED',
                        'text-blue-600': selectedGrade.status === 'IN_PROGRESS'
                      }"
                    >
                      {{ selectedGrade.status === 'IN_PROGRESS' ? 'IN PROGRESS' : selectedGrade.status }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <!-- Feedback Message -->
          <div v-if="selectedGrade.feedback" class="bg-primary/5 dark:bg-primary/10 border-l-4 border-primary p-4 rounded-r-lg">
            <h4 class="text-xs font-bold text-primary uppercase tracking-wider mb-1">Instructor Feedback</h4>
            <p class="text-sm text-text-main-light dark:text-text-main-dark leading-relaxed">
              {{ selectedGrade.feedback }}
            </p>
          </div>
        </div>
      </div>
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
