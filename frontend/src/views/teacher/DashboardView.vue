<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const user = computed(() => authStore.user)

const currentDate = computed(() => {
  return new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
})

const displayName = computed(() => {
  if (user.value?.email) {
    const name = user.value.email.split('@')[0]
    return (name?.charAt(0).toUpperCase() ?? '') + (name?.slice(1) ?? '')
  }
  return 'Teacher'
})

// Quick stats — will be fetched from API later
const stats = [
  {
    label: 'Total Students',
    value: '124',
    icon: 'groups',
    trend: '+2.4%',
    trendPositive: true,
  },
  {
    label: 'Active Classes',
    value: '6',
    icon: 'meeting_room',
    trend: 'No change',
    trendPositive: null,
  },
  {
    label: 'Pending Grades',
    value: '12',
    icon: 'pending_actions',
    trend: '-3 items',
    trendPositive: false,
  },
  {
    label: 'Avg. Attendance',
    value: '94%',
    icon: 'person_check',
    trend: '+1.2%',
    trendPositive: true,
  },
]

// Today's schedule — will be fetched from API later
const scheduleItems = [
  {
    time: '08:00 AM',
    title: 'Advanced Mathematics',
    location: 'Room 204',
    group: 'Grade 11-A',
    isCurrent: false,
    isOnline: false,
  },
  {
    time: '10:30 AM',
    title: 'Algebra Fundamentals',
    location: 'Online Section',
    group: 'Grade 9-C',
    isCurrent: true,
    isOnline: true,
  },
  {
    time: '01:00 PM',
    title: 'Faculty Meeting',
    location: 'Conference Room B',
    group: '',
    isCurrent: false,
    isOnline: false,
  },
  {
    time: '02:30 PM',
    title: 'Calculus II',
    location: 'Room 102',
    group: 'Grade 12-B',
    isCurrent: false,
    isOnline: false,
  },
]

// Performance chart data
const performanceData = [
  { month: 'JAN', value: 40, label: '68%' },
  { month: 'FEB', value: 55, label: '72%' },
  { month: 'MAR', value: 65, label: '78%' },
  { month: 'APR', value: 85, label: '92%' },
  { month: 'MAY', value: 75, label: '84%' },
  { month: 'JUN', value: 80, label: '88%' },
]

// Quick actions
const quickActions = [
  { label: 'Mark Attendance', icon: 'how_to_reg' },
  { label: 'Input Grades', icon: 'edit_square' },
  { label: 'Post Announcement', icon: 'campaign' },
]

// Recent announcements
const announcements = [
  {
    source: 'Faculty Board',
    time: '2h ago',
    title: 'Year-end exams schedule has been updated.',
    description: 'Please review the updated calendar for the upcoming semester finals starting next week...',
    isNew: true,
  },
  {
    source: 'Principal Office',
    time: '5h ago',
    title: 'School picnic next Friday confirmed.',
    description: 'All classes will be suspended for the annual outdoors activity day on Oct 31st.',
    isNew: true,
  },
  {
    source: 'IT Department',
    time: 'Yesterday',
    title: 'System maintenance scheduled.',
    description: 'The portal will be down for 2 hours this Sunday for regular database optimization.',
    isNew: false,
  },
]
</script>

<template>
  <div class="flex-1 flex flex-col p-6 lg:p-10 gap-8 max-w-7xl mx-auto w-full">
    <!-- Welcome Section -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
      <div class="space-y-1">
        <h1 class="text-3xl font-black tracking-tight">Welcome back, {{ displayName }}</h1>
        <p class="text-text-muted-light dark:text-text-muted-dark">
          Here's what's happening with your classes today, {{ currentDate }}.
        </p>
      </div>
      <div class="flex gap-3">
        <button class="flex items-center gap-2 bg-surface-light dark:bg-surface-dark border border-border-light dark:border-border-dark px-4 py-2 rounded-lg text-sm font-bold shadow-sm hover:shadow-md transition-shadow">
          <span class="material-symbols-outlined text-[20px]">download</span> Export Report
        </button>
        <button class="flex items-center gap-2 bg-primary text-white px-4 py-2 rounded-lg text-sm font-bold shadow-lg shadow-primary/20 hover:brightness-110 transition-all">
          <span class="material-symbols-outlined text-[20px]">add</span> New Entry
        </button>
      </div>
    </div>

    <!-- Quick Stats -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="bg-surface-light dark:bg-surface-dark p-6 rounded-xl border-l-4 border-primary shadow-sm space-y-2 hover:shadow-md transition-shadow"
      >
        <div class="flex justify-between items-start">
          <p class="text-sm font-medium text-text-muted-light dark:text-text-muted-dark">{{ stat.label }}</p>
          <span class="material-symbols-outlined text-primary">{{ stat.icon }}</span>
        </div>
        <div class="flex items-baseline gap-2">
          <h3 class="text-2xl font-bold">{{ stat.value }}</h3>
          <span
            :class="[
              'text-xs font-bold',
              stat.trendPositive === true ? 'text-green-600 dark:text-green-400' :
              stat.trendPositive === false ? 'text-red-500 dark:text-red-400' :
              'text-text-muted-light dark:text-text-muted-dark',
            ]"
          >
            {{ stat.trend }}
          </span>
        </div>
      </div>
    </div>

    <!-- Main Grid: Schedule vs Announcements & Quick Actions -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <!-- Left: Today's Schedule + Performance -->
      <div class="lg:col-span-2 space-y-6">
        <div class="flex items-center justify-between">
          <h2 class="text-xl font-bold tracking-tight">Today's Schedule</h2>
          <router-link
            :to="{ name: 'teacher-schedule' }"
            class="text-sm font-bold text-primary hover:underline"
          >
            View Calendar
          </router-link>
        </div>

        <div class="space-y-4">
          <!-- Schedule entries -->
          <div v-for="(item, index) in scheduleItems" :key="index" class="flex gap-4">
            <div class="flex flex-col items-center">
              <span class="text-xs font-bold text-text-muted-light dark:text-text-muted-dark w-14 text-right">{{ item.time }}</span>
              <div v-if="index < scheduleItems.length - 1" class="w-px h-full bg-border-light dark:bg-border-dark my-1"></div>
            </div>

            <!-- Current session (highlighted) -->
            <div
              v-if="item.isCurrent"
              class="flex-1 bg-primary/10 border-2 border-primary/30 p-4 rounded-xl flex justify-between items-center"
            >
              <div class="space-y-1">
                <div class="flex items-center gap-2">
                  <p class="text-sm font-bold text-primary">{{ item.title }}</p>
                  <span class="bg-primary text-white text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider">Now</span>
                </div>
                <p class="text-xs text-text-muted-light dark:text-text-muted-dark flex items-center gap-1">
                  <span class="material-symbols-outlined text-[14px]">location_on</span>
                  {{ item.location }}<template v-if="item.group"> &bull; {{ item.group }}</template>
                </p>
              </div>
              <button class="bg-primary text-white text-xs font-bold px-4 py-2 rounded-lg hover:brightness-110 transition-all">
                Join Session
              </button>
            </div>

            <!-- Regular session -->
            <div
              v-else
              class="flex-1 bg-surface-light dark:bg-surface-dark p-4 rounded-xl border border-border-light dark:border-border-dark flex justify-between items-center group hover:border-primary transition-colors cursor-pointer"
            >
              <div class="space-y-1">
                <p class="text-sm font-bold">{{ item.title }}</p>
                <p class="text-xs text-text-muted-light dark:text-text-muted-dark flex items-center gap-1">
                  <span class="material-symbols-outlined text-[14px]">location_on</span>
                  {{ item.location }}<template v-if="item.group"> &bull; {{ item.group }}</template>
                </p>
              </div>
              <span class="material-symbols-outlined text-border-light group-hover:text-primary transition-colors">chevron_right</span>
            </div>
          </div>
        </div>

        <!-- Student Performance Trends -->
        <div class="bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-border-light dark:border-border-dark space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-bold">Student Performance Trends</h3>
            <select class="text-xs bg-background-light dark:bg-stone-800 border-none rounded-lg focus:ring-primary py-1 text-text-main-light dark:text-text-main-dark">
              <option>Last 6 Months</option>
              <option>Semester 1</option>
            </select>
          </div>
          <!-- Bar Chart -->
          <div class="relative h-48 w-full flex items-end justify-between px-4 pb-4">
            <div class="absolute inset-0 flex flex-col justify-between py-4 pointer-events-none">
              <div class="border-b border-dashed border-border-light dark:border-border-dark w-full"></div>
              <div class="border-b border-dashed border-border-light dark:border-border-dark w-full"></div>
              <div class="border-b border-dashed border-border-light dark:border-border-dark w-full"></div>
              <div class="border-b border-dashed border-border-light dark:border-border-dark w-full"></div>
            </div>
            <div
              v-for="bar in performanceData"
              :key="bar.month"
              class="z-10 w-8 bg-primary rounded-t-lg transition-all hover:brightness-110 cursor-pointer"
              :style="{ height: bar.value + '%', opacity: 0.3 + (bar.value / 100) * 0.7 }"
              :title="`${bar.month}: ${bar.label}`"
            ></div>
          </div>
          <div class="flex justify-between px-4 text-[10px] font-bold text-text-muted-light dark:text-text-muted-dark">
            <span v-for="bar in performanceData" :key="bar.month">{{ bar.month }}</span>
          </div>
        </div>
      </div>

      <!-- Right Column: Quick Actions & Announcements -->
      <div class="space-y-8">
        <!-- Quick Actions -->
        <div class="space-y-4">
          <h2 class="text-xl font-bold tracking-tight">Quick Actions</h2>
          <div class="grid grid-cols-1 gap-3">
            <button
              v-for="action in quickActions"
              :key="action.label"
              class="flex items-center gap-3 w-full p-4 bg-surface-light dark:bg-surface-dark hover:bg-primary hover:text-white transition-all rounded-xl border border-border-light dark:border-border-dark group"
            >
              <div class="size-10 flex items-center justify-center bg-primary/10 group-hover:bg-white/20 rounded-lg text-primary group-hover:text-white transition-colors">
                <span class="material-symbols-outlined">{{ action.icon }}</span>
              </div>
              <span class="font-bold text-sm">{{ action.label }}</span>
            </button>
          </div>
        </div>

        <!-- Recent Announcements -->
        <div class="space-y-4">
          <div class="flex items-center justify-between">
            <h2 class="text-xl font-bold tracking-tight">Recent Announcements</h2>
            <span class="material-symbols-outlined text-text-muted-light dark:text-text-muted-dark cursor-pointer hover:text-primary transition-colors">more_horiz</span>
          </div>
          <div class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark divide-y divide-border-light dark:divide-border-dark">
            <div
              v-for="(announcement, index) in announcements"
              :key="index"
              class="p-4 space-y-2"
            >
              <div class="flex items-center gap-2">
                <span
                  :class="[
                    'size-2 rounded-full',
                    announcement.isNew ? 'bg-primary' : 'bg-border-light dark:bg-border-dark',
                  ]"
                ></span>
                <p class="text-xs font-bold text-text-muted-light dark:text-text-muted-dark">
                  {{ announcement.source }} &bull; {{ announcement.time }}
                </p>
              </div>
              <p class="text-sm font-semibold leading-tight">{{ announcement.title }}</p>
              <p class="text-xs text-text-muted-light dark:text-text-muted-dark line-clamp-2">
                {{ announcement.description }}
              </p>
            </div>
            <button class="w-full py-3 text-xs font-bold text-primary hover:bg-primary/5 transition-colors rounded-b-xl">
              View All Notifications
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
