<script setup lang="ts">
import { ref } from 'vue'

const semesters = ref([
  {
    name: 'Fall 2024 (Current)',
    gpa: '3.90',
    icon: 'calendar_today',
    isCurrent: true,
    isOpen: true,
    courses: [
      {
        code: 'CS-101',
        name: 'Introduction to Algorithms',
        credits: '4.0',
        grade: 'A',
        points: '16.0',
      },
      {
        code: 'MATH-202',
        name: 'Advanced Calculus II',
        credits: '3.0',
        grade: 'A-',
        points: '11.1',
      },
      { code: 'LIT-204', name: 'Creative Writing', credits: '3.0', grade: 'B+', points: '9.9' },
    ],
  },
  {
    name: 'Spring 2024',
    gpa: '3.75',
    icon: 'history',
    isCurrent: false,
    isOpen: false,
    courses: [
      { code: 'CS-102', name: 'Data Structures', credits: '4.0', grade: 'A', points: '16.0' },
      { code: 'PHYS-101', name: 'General Physics I', credits: '4.0', grade: 'B', points: '12.0' },
      { code: 'ENG-102', name: 'Composition II', credits: '3.0', grade: 'A', points: '12.0' },
    ],
  },
  {
    name: 'Fall 2023',
    gpa: '3.85',
    icon: 'history',
    isCurrent: false,
    isOpen: false,
    courses: [
      {
        code: 'CS-100',
        name: 'Computing Fundamentals',
        credits: '3.0',
        grade: 'A',
        points: '12.0',
      },
      { code: 'MATH-201', name: 'Advanced Calculus I', credits: '3.0', grade: 'A', points: '12.0' },
      {
        code: 'PSYCH-101',
        name: 'Intro to Psychology',
        credits: '3.0',
        grade: 'B+',
        points: '9.9',
      },
    ],
  },
])

function toggleSemester(index: number) {
  const semester = semesters.value[index]
  if (semester) {
    semester.isOpen = !semester.isOpen
  }
}
</script>

<template>
  <div class="flex-1 flex justify-center w-full px-4 sm:px-6 py-8">
    <div class="w-full max-w-6xl flex flex-col gap-8">
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
        <button
          class="flex items-center gap-2 h-12 px-6 rounded-xl bg-primary hover:bg-primary/90 text-text-main-light font-bold transition-all shadow-lg shadow-primary/20"
        >
          <span class="material-symbols-outlined">download</span>
          <span>Download Transcript</span>
        </button>
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
                Current GPA
              </p>
              <h3 class="text-text-main-light dark:text-text-main-dark text-2xl font-bold">3.82</h3>
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
              <h3 class="text-text-main-light dark:text-text-main-dark text-2xl font-bold">94.0</h3>
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
                Good Standing
              </h3>
            </div>
          </div>
        </div>
      </div>

      <!-- Semesters List -->
      <div class="flex flex-col gap-6">
        <div
          v-for="(semester, index) in semesters"
          :key="semester.name"
          class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark overflow-hidden shadow-sm"
        >
          <div
            @click="toggleSemester(index)"
            class="flex items-center justify-between p-5 cursor-pointer hover:bg-input-bg-light/30 dark:hover:bg-input-bg-dark/30 transition-colors"
          >
            <div class="flex items-center gap-4">
              <div
                v-if="semester.isCurrent"
                class="size-8 rounded-full bg-primary flex items-center justify-center text-white"
              >
                <span class="material-symbols-outlined !text-lg">{{ semester.icon }}</span>
              </div>
              <div
                v-else
                class="size-8 rounded-full bg-input-bg-light dark:bg-input-bg-dark flex items-center justify-center text-text-muted-light dark:text-text-muted-dark"
              >
                <span class="material-symbols-outlined !text-lg">{{ semester.icon }}</span>
              </div>
              <h3 class="text-text-main-light dark:text-text-main-dark font-bold text-lg">
                {{ semester.name }}
              </h3>
            </div>
            <div class="flex items-center gap-4">
              <span
                class="text-sm font-medium px-3 py-1 rounded-full bg-input-bg-light dark:bg-input-bg-dark text-text-muted-light dark:text-text-muted-dark"
              >
                GPA: {{ semester.gpa }}
              </span>
              <span
                class="material-symbols-outlined text-text-muted-light dark:text-text-muted-dark transition-transform"
                :class="{ 'rotate-180': !semester.isOpen }"
              >
                expand_less
              </span>
            </div>
          </div>

          <div v-show="semester.isOpen" class="overflow-x-auto">
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
                    class="px-6 py-3 text-xs font-bold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider"
                  >
                    Credits
                  </th>
                  <th
                    class="px-6 py-3 text-xs font-bold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider text-center"
                  >
                    Grade
                  </th>
                  <th
                    class="px-6 py-3 text-xs font-bold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider text-right"
                  >
                    Points
                  </th>
                </tr>
              </thead>
              <tbody class="divide-y divide-border-light dark:divide-border-dark">
                <tr
                  v-for="course in semester.courses"
                  :key="course.code"
                  class="hover:bg-input-bg-light/20 dark:hover:bg-input-bg-dark/20 transition-colors"
                >
                  <td class="px-6 py-4 text-sm font-medium text-primary">{{ course.code }}</td>
                  <td class="px-6 py-4 text-sm text-text-main-light dark:text-text-main-dark">
                    {{ course.name }}
                  </td>
                  <td class="px-6 py-4 text-sm text-text-main-light dark:text-text-main-dark">
                    {{ course.credits }}
                  </td>
                  <td class="px-6 py-4 text-center">
                    <span
                      class="px-2 py-1 rounded-md font-bold"
                      :class="[
                        course.grade.startsWith('A')
                          ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400'
                          : 'bg-primary/10 text-primary',
                      ]"
                    >
                      {{ course.grade }}
                    </span>
                  </td>
                  <td
                    class="px-6 py-4 text-sm text-right text-text-main-light dark:text-text-main-dark"
                  >
                    {{ course.points }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Guide -->
      <div class="bg-primary/5 dark:bg-primary/10 p-6 rounded-xl border border-primary/20">
        <h4
          class="text-text-main-light dark:text-text-main-dark font-bold mb-4 flex items-center gap-2"
        >
          <span class="material-symbols-outlined text-primary">info</span>
          Grade Interpretation Guide
        </h4>
        <div class="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-4">
          <div class="flex flex-col">
            <span class="text-xs font-bold text-text-muted-light dark:text-text-muted-dark"
              >A (4.0)</span
            >
            <span class="text-xs text-text-muted-light dark:text-text-muted-dark">Excellent</span>
          </div>
          <div class="flex flex-col">
            <span class="text-xs font-bold text-text-muted-light dark:text-text-muted-dark"
              >B (3.0)</span
            >
            <span class="text-xs text-text-muted-light dark:text-text-muted-dark">Good</span>
          </div>
          <div class="flex flex-col">
            <span class="text-xs font-bold text-text-muted-light dark:text-text-muted-dark"
              >C (2.0)</span
            >
            <span class="text-xs text-text-muted-light dark:text-text-muted-dark"
              >Satisfactory</span
            >
          </div>
          <div class="flex flex-col">
            <span class="text-xs font-bold text-text-muted-light dark:text-text-muted-dark"
              >D (1.0)</span
            >
            <span class="text-xs text-text-muted-light dark:text-text-muted-dark">Poor</span>
          </div>
          <div class="flex flex-col">
            <span class="text-xs font-bold text-text-muted-light dark:text-text-muted-dark"
              >F (0.0)</span
            >
            <span class="text-xs text-text-muted-light dark:text-text-muted-dark">Failure</span>
          </div>
          <div class="flex flex-col">
            <span class="text-xs font-bold text-text-muted-light dark:text-text-muted-dark"
              >P / NP</span
            >
            <span class="text-xs text-text-muted-light dark:text-text-muted-dark"
              >Pass / No Pass</span
            >
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
