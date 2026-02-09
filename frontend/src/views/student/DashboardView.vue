<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const user = computed(() => authStore.user)

const displayName = computed(() => {
  if (user.value?.email) {
    const name = user.value.email.split('@')[0]
    return name.charAt(0).toUpperCase() + name.slice(1)
  }
  return 'Student'
})

// Quick stats — will be fetched from API later
const stats = [
  {
    label: 'Current GPA',
    value: '3.8',
    suffix: '',
    icon: 'school',
    trend: '+0.2%',
    trendPositive: true,
    iconBg: 'bg-blue-50 dark:bg-blue-900/20',
    iconColor: 'text-blue-600 dark:text-blue-400',
    highlight: false,
  },
  {
    label: 'Total Credits',
    value: '84',
    suffix: '/120',
    icon: 'auto_stories',
    trend: '+12',
    trendPositive: true,
    iconBg: 'bg-purple-50 dark:bg-purple-900/20',
    iconColor: 'text-purple-600 dark:text-purple-400',
    highlight: false,
  },
  {
    label: 'Attendance Rate',
    value: '92%',
    suffix: '',
    icon: 'calendar_today',
    trend: '+2%',
    trendPositive: true,
    iconBg: 'bg-green-50 dark:bg-green-900/20',
    iconColor: 'text-green-600 dark:text-green-400',
    highlight: false,
  },
  {
    label: 'Pending Assignments',
    value: '4',
    suffix: '',
    icon: 'assignment_late',
    trend: 'Urgent',
    trendPositive: false,
    iconBg: 'bg-orange-50 dark:bg-orange-900/20',
    iconColor: 'text-primary',
    highlight: true,
  },
]

// Schedule data — will be fetched from API later
const selectedDay = 'Today'
const weekDays = ['Today', 'Tue, 24', 'Wed, 25', 'Thu, 26', 'Fri, 27']

const scheduleItems = [
  {
    time: '09:00',
    period: 'AM',
    title: 'Introduction to Computer Science',
    location: 'Room 302',
    instructor: 'Prof. Smith',
    type: 'Lecture',
    typeBg: 'bg-blue-100 dark:bg-blue-900/30',
    typeColor: 'text-blue-700 dark:text-blue-300',
    borderColor: 'border-primary',
  },
  {
    time: '11:00',
    period: 'AM',
    title: 'Calculus II',
    location: 'Room 101',
    instructor: 'Dr. Johnson',
    type: 'Lab',
    typeBg: 'bg-purple-100 dark:bg-purple-900/30',
    typeColor: 'text-purple-700 dark:text-purple-300',
    borderColor: 'border-purple-500',
  },
  {
    time: '02:00',
    period: 'PM',
    title: 'History of Art',
    location: 'Online',
    instructor: '',
    type: 'Seminar',
    typeBg: 'bg-green-100 dark:bg-green-900/30',
    typeColor: 'text-green-700 dark:text-green-300',
    borderColor: 'border-green-500',
    isOnline: true,
  },
]

// Deadlines — will be fetched from API later
const deadlines = [
  {
    title: 'Calculus Homework 4',
    course: 'Calculus II',
    due: 'Due Tomorrow',
    icon: 'warning',
    iconBg: 'bg-red-50 dark:bg-red-900/20',
    iconColor: 'text-red-600 dark:text-red-400',
  },
  {
    title: 'History Essay Draft',
    course: 'History of Art',
    due: 'Due Friday',
    icon: 'article',
    iconBg: 'bg-orange-50 dark:bg-orange-900/20',
    iconColor: 'text-primary',
  },
  {
    title: 'Final Project Proposal',
    course: 'Intro to CS',
    due: 'Due Next Monday',
    icon: 'code',
    iconBg: 'bg-blue-50 dark:bg-blue-900/20',
    iconColor: 'text-blue-600 dark:text-blue-400',
  },
]

// Latest grades — will be fetched from API later
const grades = [
  { course: 'Data Structures', grade: 'A', gradeColor: 'text-green-600 dark:text-green-400' },
  { course: 'Physics I', grade: 'A-', gradeColor: 'text-green-600 dark:text-green-400' },
  { course: 'English Lit', grade: 'B+', gradeColor: 'text-primary' },
]

// Degree completion
const degreeCompletion = 70
const totalCreditsRequired = 120
</script>

<template>
  <div class="px-6 md:px-12 lg:px-20 py-8 flex flex-col flex-1 max-w-[1440px] mx-auto w-full">
    <!-- Welcome Banner -->
    <div class="flex flex-col md:flex-row gap-6 items-start md:items-center justify-between mb-8">
      <div class="flex gap-5 items-center">
        <div
          v-if="user?.profilePictureUrl"
          class="bg-center bg-no-repeat aspect-square bg-cover rounded-full size-20 md:size-24 border-4 border-surface-light dark:border-background-dark shadow-lg"
          :style="{ backgroundImage: `url(${user.profilePictureUrl})` }"
        ></div>
        <div
          v-else
          class="rounded-full size-20 md:size-24 border-4 border-surface-light dark:border-background-dark shadow-lg bg-primary/10 flex items-center justify-center text-primary font-bold text-2xl"
        >
          {{ displayName.substring(0, 2).toUpperCase() }}
        </div>
        <div class="flex flex-col justify-center">
          <h1 class="text-2xl md:text-3xl font-bold leading-tight">Welcome back, {{ displayName }}!</h1>
          <p class="text-text-muted-light dark:text-text-muted-dark text-sm md:text-base font-medium mt-1">Computer Science Major &bull; Senior Year</p>
          <p class="text-text-muted-light dark:text-stone-500 text-sm mt-1">Fall Semester 2023</p>
        </div>
      </div>
      <div class="flex gap-3">
        <button class="bg-primary hover:bg-orange-600 text-white px-5 py-2.5 rounded-lg text-sm font-semibold shadow-sm transition-colors flex items-center gap-2">
          <span class="material-symbols-outlined text-[18px]">add</span>
          New Task
        </button>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
      <div
        v-for="stat in stats"
        :key="stat.label"
        :class="[
          'flex flex-col gap-2 rounded-xl p-5 bg-surface-light dark:bg-surface-dark shadow-sm hover:shadow-md transition-shadow relative overflow-hidden',
          stat.highlight
            ? 'border border-primary/30'
            : 'border border-border-light dark:border-border-dark',
        ]"
      >
        <!-- Decorative blob for highlighted card -->
        <div v-if="stat.highlight" class="absolute right-0 top-0 w-16 h-16 bg-primary/10 rounded-bl-full -mr-4 -mt-4"></div>

        <div class="flex justify-between items-start z-10">
          <div :class="['p-2 rounded-lg', stat.iconBg, stat.iconColor]">
            <span class="material-symbols-outlined">{{ stat.icon }}</span>
          </div>
          <span
            :class="[
              'text-xs font-bold px-2 py-1 rounded',
              stat.trendPositive ? 'text-green-700 dark:text-green-400 bg-green-50 dark:bg-green-900/20' : 'text-primary bg-orange-50 dark:bg-orange-900/20',
            ]"
          >
            {{ stat.trend }}
          </span>
        </div>
        <p class="text-text-muted-light dark:text-stone-400 text-sm font-medium mt-2 z-10">{{ stat.label }}</p>
        <p class="text-3xl font-bold tracking-tight z-10">
          {{ stat.value }}<span v-if="stat.suffix" class="text-lg text-text-muted-light dark:text-text-muted-dark font-normal">{{ stat.suffix }}</span>
        </p>
      </div>
    </div>

    <!-- Main Content Grid -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
      <!-- Left Column (Weekly Schedule + Degree) - Spans 7 cols -->
      <div class="lg:col-span-7 flex flex-col gap-6">
        <!-- Schedule Section -->
        <section class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark p-6 shadow-sm">
          <div class="flex justify-between items-center mb-6">
            <h3 class="text-xl font-bold">My Weekly Schedule</h3>
            <router-link :to="{ name: 'student-schedule' }" class="text-primary text-sm font-bold hover:underline">
              View Full Calendar
            </router-link>
          </div>

          <!-- Days Tabs -->
          <div class="flex gap-2 mb-6 overflow-x-auto pb-2">
            <button
              v-for="day in weekDays"
              :key="day"
              :class="[
                'px-4 py-2 rounded-lg text-sm font-medium whitespace-nowrap transition-colors',
                day === selectedDay
                  ? 'bg-primary text-white shadow-sm'
                  : 'bg-background-light dark:bg-stone-800 text-text-muted-light dark:text-text-muted-dark hover:bg-stone-200 dark:hover:bg-stone-700',
              ]"
            >
              {{ day }}
            </button>
          </div>

          <!-- Schedule List -->
          <div class="flex flex-col gap-4">
            <div
              v-for="item in scheduleItems"
              :key="item.title"
              :class="['flex items-center gap-4 p-4 rounded-lg bg-background-light dark:bg-stone-800 border-l-4', item.borderColor]"
            >
              <div class="flex flex-col min-w-[80px]">
                <span class="font-bold text-lg">{{ item.time }}</span>
                <span class="text-text-muted-light dark:text-text-muted-dark text-xs uppercase font-medium">{{ item.period }}</span>
              </div>
              <div class="w-px h-10 bg-border-light dark:bg-border-dark"></div>
              <div class="flex-1">
                <h4 class="font-bold text-base">{{ item.title }}</h4>
                <div class="flex items-center gap-3 mt-1">
                  <span class="text-text-muted-light dark:text-text-muted-dark text-sm flex items-center gap-1">
                    <span class="material-symbols-outlined text-[16px]">{{ item.isOnline ? 'laptop_chromebook' : 'location_on' }}</span>
                    {{ item.location }}
                  </span>
                  <span v-if="item.instructor" class="text-text-muted-light dark:text-text-muted-dark text-sm flex items-center gap-1">
                    <span class="material-symbols-outlined text-[16px]">person</span>
                    {{ item.instructor }}
                  </span>
                </div>
              </div>
              <span :class="['text-xs font-bold px-2 py-1 rounded', item.typeBg, item.typeColor]">{{ item.type }}</span>
            </div>
          </div>
        </section>

        <!-- Degree Completion Section -->
        <section class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark p-6 shadow-sm">
          <div class="flex flex-col md:flex-row gap-8 items-center">
            <!-- Progress Ring -->
            <div class="relative size-32 md:size-40 shrink-0">
              <svg class="size-full -rotate-90" viewBox="0 0 36 36" xmlns="http://www.w3.org/2000/svg">
                <path
                  class="text-stone-100 dark:text-stone-800"
                  d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="3.5"
                />
                <path
                  class="text-primary"
                  d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  fill="none"
                  stroke="currentColor"
                  :stroke-dasharray="`${degreeCompletion}, 100`"
                  stroke-linecap="round"
                  stroke-width="3.5"
                />
              </svg>
              <div class="absolute inset-0 flex items-center justify-center flex-col">
                <span class="text-2xl font-bold">{{ degreeCompletion }}%</span>
              </div>
            </div>
            <div class="flex flex-col gap-2 text-center md:text-left">
              <h3 class="text-xl font-bold">Degree Completion</h3>
              <p class="text-text-muted-light dark:text-stone-400 text-sm">
                You are on track to graduate in Spring 2024. Keep up the great work!
              </p>
              <div class="mt-2 flex flex-wrap gap-2 justify-center md:justify-start">
                <span class="bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300 text-xs px-3 py-1 rounded-full font-medium">On Track</span>
                <span class="bg-stone-100 dark:bg-stone-800 text-xs px-3 py-1 rounded-full font-medium">{{ totalCreditsRequired }} Credits Required</span>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- Right Column (Deadlines & Grades) - Spans 5 cols -->
      <div class="lg:col-span-5 flex flex-col gap-6">
        <!-- Deadlines Section -->
        <section class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark p-6 shadow-sm flex flex-col h-full">
          <div class="flex justify-between items-center mb-6">
            <h3 class="text-xl font-bold">Upcoming Deadlines</h3>
            <button class="text-primary text-sm font-bold hover:underline">View All</button>
          </div>
          <div class="flex flex-col gap-4">
            <div
              v-for="(deadline, index) in deadlines"
              :key="deadline.title"
              :class="['flex items-start gap-4', index < deadlines.length - 1 ? 'pb-4 border-b border-stone-100 dark:border-stone-800' : '']"
            >
              <div :class="['p-2.5 rounded-lg', deadline.iconBg, deadline.iconColor]">
                <span class="material-symbols-outlined text-[20px]">{{ deadline.icon }}</span>
              </div>
              <div class="flex-1">
                <h4 class="font-bold text-sm">{{ deadline.title }}</h4>
                <p class="text-text-muted-light dark:text-text-muted-dark text-xs mt-0.5">{{ deadline.course }} &bull; {{ deadline.due }}</p>
              </div>
              <button class="size-8 flex items-center justify-center rounded-full border border-border-light dark:border-border-dark text-text-muted-light dark:text-text-muted-dark hover:bg-background-light dark:hover:bg-stone-800 transition-colors">
                <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
              </button>
            </div>
          </div>
        </section>

        <!-- Latest Grades Section -->
        <section class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark p-6 shadow-sm">
          <h3 class="text-xl font-bold mb-4">Latest Grades</h3>
          <div class="overflow-hidden rounded-lg border border-stone-100 dark:border-stone-800">
            <table class="w-full text-left border-collapse">
              <thead class="bg-background-light dark:bg-stone-800">
                <tr>
                  <th class="p-3 text-xs font-semibold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider">Course</th>
                  <th class="p-3 text-xs font-semibold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider text-right">Grade</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-stone-100 dark:divide-stone-800">
                <tr v-for="item in grades" :key="item.course">
                  <td class="p-3 text-sm font-medium">{{ item.course }}</td>
                  <td :class="['p-3 text-sm font-bold text-right', item.gradeColor]">{{ item.grade }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <router-link
            :to="{ name: 'student-grades' }"
            class="block w-full mt-4 py-2 border border-border-light dark:border-border-dark rounded-lg text-sm font-bold text-center hover:bg-background-light dark:hover:bg-stone-800 transition-colors"
          >
            View Transcript
          </router-link>
        </section>
      </div>
    </div>
  </div>
</template>
