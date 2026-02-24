<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { gradeService, type StudentGrade, type StudentTranscript } from '@/services/gradeService'
import { semesterService, type SemesterResponse } from '@/services/semesterService'

const isLoading = ref(true)
const semesters = ref<SemesterResponse[]>([])
const selectedSemesterId = ref<number | null>(null)
const semesterGrades = ref<StudentGrade[]>([])
const transcript = ref<StudentTranscript | null>(null)

const currentGpa = computed(() => transcript.value?.cumulativeGpa || 0)
const totalCredits = computed(() => transcript.value?.totalCredits || 0)

async function fetchInitialData() {
  try {
    isLoading.value = true
    const [semRes, transRes] = await Promise.all([
      semesterService.getAllSemesters(),
      gradeService.getTranscript(),
    ])

    semesters.value = semRes.result || []
    transcript.value = transRes.result || null

    if (semesters.value.length > 0) {
      // Mặc định chọn học kỳ hiện tại hoặc học kỳ gần nhất
      const current = semesters.value.find((s) => s.isCurrent)
      selectedSemesterId.value = current ? current.semesterId : semesters.value[0].semesterId
      await fetchGrades()
    }
  } catch (error) {
    console.error('Failed to fetch initial grade data', error)
  } finally {
    isLoading.value = false
  }
}

async function fetchGrades() {
  if (selectedSemesterId.value === null) return

  try {
    const res = await gradeService.getGradesBySemester(selectedSemesterId.value)
    semesterGrades.value = res.result || []
  } catch (error) {
    console.error('Failed to fetch grades for semester', error)
  }
}

function handleSemesterChange(id: number) {
  selectedSemesterId.value = id
  fetchGrades()
}

onMounted(() => {
  fetchInitialData()
})

const selectedSemesterName = computed(() => {
  return (
    semesters.value.find((s) => s.semesterId === selectedSemesterId.value)?.displayName ||
    'Select Semester'
  )
})
</script>

<template>
  <div class="flex-1 flex justify-center w-full px-4 sm:px-6 py-8">
    <div v-if="isLoading" class="w-full max-w-6xl flex flex-col items-center justify-center py-20">
      <div
        class="animate-spin size-12 border-4 border-primary border-t-transparent rounded-full mb-4"
      ></div>
      <p class="text-text-muted-light dark:text-text-muted-dark animate-pulse">
        Loading academic records...
      </p>
    </div>

    <div v-else class="w-full max-w-6xl flex flex-col gap-8">
      <!-- Page Heading & Actions -->
      <div class="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">
        <div class="flex flex-col gap-2">
          <h1
            class="text-text-main-light dark:text-text-main-dark text-3xl md:text-4xl font-black tracking-tight"
          >
            Academic Performance
          </h1>
          <p class="text-text-muted-light dark:text-text-muted-dark text-base max-w-2xl">
            Overview of your final grades and transcript data across all semesters.
          </p>
        </div>

        <!-- Semester Selector -->
        <div class="relative w-full md:w-72 group">
          <select
            v-model="selectedSemesterId"
            @change="fetchGrades"
            class="w-full h-12 pl-4 pr-12 rounded-xl bg-surface-light dark:bg-surface-dark border border-border-light dark:border-border-dark text-text-main-light dark:text-text-main-dark font-bold appearance-none cursor-pointer focus:ring-2 focus:ring-primary/20 transition-all outline-none"
          >
            <option v-for="sem in semesters" :key="sem.semesterId" :value="sem.semesterId">
              {{ sem.displayName }}
            </option>
          </select>
          <div
            class="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none flex items-center justify-center text-text-muted-light dark:text-text-muted-dark group-focus-within:text-primary transition-colors"
          >
            <span class="material-symbols-outlined !text-2xl">unfold_more</span>
          </div>
        </div>
      </div>

      <!-- Stats Grid -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div
          class="bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-border-light dark:border-border-dark shadow-sm"
        >
          <div class="flex items-center gap-4">
            <div
              class="size-12 rounded-lg bg-primary/10 flex items-center justify-center text-primary"
            >
              <span class="material-symbols-outlined !text-3xl">analytics</span>
            </div>
            <div>
              <p class="text-text-muted-light dark:text-text-muted-dark text-sm font-medium">
                Cumulative GPA (4.0)
              </p>
              <h3 class="text-text-main-light dark:text-text-main-dark text-2xl font-bold">
                {{ currentGpa.toFixed(2) }}
              </h3>
            </div>
          </div>
        </div>
        <div
          class="bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-border-light dark:border-border-dark shadow-sm"
        >
          <div class="flex items-center gap-4">
            <div
              class="size-12 rounded-lg bg-primary/10 flex items-center justify-center text-primary"
            >
              <span class="material-symbols-outlined !text-3xl">school</span>
            </div>
            <div>
              <p class="text-text-muted-light dark:text-text-muted-dark text-sm font-medium">
                Total Credits Earned
              </p>
              <h3 class="text-text-main-light dark:text-text-main-dark text-2xl font-bold">
                {{ totalCredits.toFixed(1) }}
              </h3>
            </div>
          </div>
        </div>
        <div
          class="bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-border-light dark:border-border-dark shadow-sm"
        >
          <div class="flex items-center gap-4">
            <div
              class="size-12 rounded-lg bg-green-100 dark:bg-green-900/30 flex items-center justify-center text-green-600 dark:text-green-400"
            >
              <span class="material-symbols-outlined !text-3xl">verified</span>
            </div>
            <div>
              <p class="text-text-muted-light dark:text-text-muted-dark text-sm font-medium">
                Academic Standing
              </p>
              <h3 class="text-text-main-light dark:text-text-main-dark text-2xl font-bold">
                {{ currentGpa >= 2.0 ? 'Good Standing' : 'Academic Probation' }}
              </h3>
            </div>
          </div>
        </div>
      </div>

      <!-- Semester Grades Table -->
      <div class="flex flex-col gap-6">
        <div
          class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark overflow-hidden shadow-sm"
        >
          <div
            class="p-5 border-b border-border-light dark:border-border-dark flex items-center justify-between bg-input-bg-light/10 dark:bg-input-bg-dark/10"
          >
            <h3 class="text-text-main-light dark:text-text-main-dark font-bold text-lg">
              Grades for {{ selectedSemesterName }}
            </h3>
            <button
              @click="fetchInitialData"
              class="flex items-center gap-2 text-sm text-primary font-bold hover:underline"
            >
              <span class="material-symbols-outlined text-sm">refresh</span>
              Refresh Records
            </button>
          </div>

          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-input-bg-light/50 dark:bg-input-bg-dark/50">
                  <th
                    class="px-6 py-3 text-xs font-bold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider"
                  >
                    Course Code
                  </th>
                  <th
                    class="px-6 py-3 text-xs font-bold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider"
                  >
                    Course Name
                  </th>
                  <th
                    class="px-6 py-3 text-center text-xs font-bold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider"
                  >
                    Credits
                  </th>
                  <th
                    class="px-6 py-3 text-xs font-bold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider text-center"
                  >
                    Scale 10.0
                  </th>
                  <th
                    class="px-6 py-3 text-xs font-bold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider text-center"
                  >
                    Scale 4.0
                  </th>
                  <th
                    class="px-6 py-3 text-xs font-bold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider"
                  >
                    Feedback
                  </th>
                </tr>
              </thead>
              <tbody class="divide-y divide-border-light dark:divide-border-dark">
                <tr v-if="semesterGrades.length === 0">
                  <td
                    colspan="6"
                    class="px-6 py-12 text-center text-text-muted-light dark:text-text-muted-dark"
                  >
                    <span class="material-symbols-outlined text-4xl mb-2 opacity-20"
                      >inventory_2</span
                    >
                    <p>No grade records found for this semester.</p>
                  </td>
                </tr>
                <tr
                  v-for="item in semesterGrades"
                  :key="item.courseCode"
                  class="hover:bg-input-bg-light/20 dark:hover:bg-input-bg-dark/20 transition-colors"
                >
                  <td class="px-6 py-4 text-sm font-medium text-primary">{{ item.courseCode }}</td>
                  <td class="px-6 py-4 text-sm text-text-main-light dark:text-text-main-dark">
                    {{ item.courseName }}
                  </td>
                  <td
                    class="px-6 py-4 text-center text-sm text-text-main-light dark:text-text-main-dark"
                  >
                    {{ item.credits }}
                  </td>
                  <td class="px-6 py-4 text-center">
                    <span
                      v-if="item.grade !== null"
                      class="px-2 py-1 rounded-md font-bold text-sm bg-input-bg-light dark:bg-input-bg-dark"
                    >
                      {{ item.grade.toFixed(1) }}
                    </span>
                    <span
                      v-else
                      class="text-text-muted-light dark:text-text-muted-dark italic text-xs"
                    >
                      -
                    </span>
                  </td>
                  <td class="px-6 py-4 text-center">
                    <span
                      v-if="item.grade4 !== null"
                      class="px-2 py-1 rounded-md font-bold"
                      :class="[
                        item.grade4 >= 3.5
                          ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400'
                          : item.grade4 >= 2.0
                            ? 'bg-primary/10 text-primary'
                            : 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
                      ]"
                    >
                      {{ item.grade4.toFixed(1) }}
                    </span>
                    <span
                      v-else
                      class="text-text-muted-light dark:text-text-muted-dark italic text-xs"
                    >
                      -
                    </span>
                  </td>
                  <td
                    class="px-6 py-4 text-sm text-text-muted-light dark:text-text-muted-dark italic"
                  >
                    {{ item.feedback || '-' }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Transcript Summary Button -->
      <div class="flex justify-center mt-4">
        <button
          class="flex items-center gap-2 h-12 px-8 rounded-xl bg-surface-light dark:bg-surface-dark border border-primary text-primary hover:bg-primary/10 font-bold transition-all shadow-lg"
        >
          <span class="material-symbols-outlined">description</span>
          <span>View Detailed Transcript</span>
        </button>
      </div>

      <!-- Grade Interpretation Guide -->
      <div class="bg-primary/5 dark:bg-primary/10 p-6 rounded-xl border border-primary/20">
        <h4
          class="text-text-main-light dark:text-text-main-dark font-bold mb-4 flex items-center gap-2"
        >
          <span class="material-symbols-outlined text-primary">info</span>
          Grade Interpretation Guide (Scale 4.0 & 10.0)
        </h4>
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-4">
          <div
            class="flex flex-col bg-surface-light dark:bg-surface-dark p-3 rounded-lg border border-border-light dark:border-border-dark"
          >
            <span class="text-xs font-bold text-primary mb-1">Scale 4.0: 3.5 - 4.0</span>
            <span class="text-xs font-semibold text-text-main-light dark:text-text-main-dark"
              >Excellent (A/A+)</span
            >
            <span class="text-[10px] text-text-muted-light dark:text-text-muted-dark mt-1"
              >Scale 10: 8.0 - 10.0</span
            >
          </div>
          <div
            class="flex flex-col bg-surface-light dark:bg-surface-dark p-3 rounded-lg border border-border-light dark:border-border-dark"
          >
            <span class="text-xs font-bold text-primary mb-1">Scale 4.0: 3.0 - 3.4</span>
            <span class="text-xs font-semibold text-text-main-light dark:text-text-main-dark"
              >Good (B/B+)</span
            >
            <span class="text-[10px] text-text-muted-light dark:text-text-muted-dark mt-1"
              >Scale 10: 7.0 - 7.9</span
            >
          </div>
          <div
            class="flex flex-col bg-surface-light dark:bg-surface-dark p-3 rounded-lg border border-border-light dark:border-border-dark"
          >
            <span class="text-xs font-bold text-primary mb-1">Scale 4.0: 2.0 - 2.9</span>
            <span class="text-xs font-semibold text-text-main-light dark:text-text-main-dark"
              >Fair (C/C+)</span
            >
            <span class="text-[10px] text-text-muted-light dark:text-text-muted-dark mt-1"
              >Scale 10: 5.5 - 6.9</span
            >
          </div>
          <div
            class="flex flex-col bg-surface-light dark:bg-surface-dark p-3 rounded-lg border border-border-light dark:border-border-dark"
          >
            <span class="text-xs font-bold text-primary mb-1">Scale 4.0: 1.0 - 1.9</span>
            <span class="text-xs font-semibold text-text-main-light dark:text-text-main-dark"
              >Poor (D/D+)</span
            >
            <span class="text-[10px] text-text-muted-light dark:text-text-muted-dark mt-1"
              >Scale 10: 4.0 - 5.4</span
            >
          </div>
          <div
            class="flex flex-col bg-surface-light dark:bg-surface-dark p-3 rounded-lg border border-border-light dark:border-border-dark"
          >
            <span class="text-xs font-bold text-red-500 mb-1">Scale 4.0: 0.0</span>
            <span class="text-xs font-semibold text-text-main-light dark:text-text-main-dark"
              >Fail (F)</span
            >
            <span class="text-[10px] text-text-muted-light dark:text-text-muted-dark mt-1"
              >Scale 10: &lt; 4.0</span
            >
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Xóa bỏ icon dropdown mặc định của browser và tailwind/forms */
select {
  appearance: none !important;
  -webkit-appearance: none !important;
  -moz-appearance: none !important;
  background-image: none !important;
}

/* Ẩn icon mặc định trong IE/Edge */
select::-ms-expand {
  display: none !important;
}
</style>
