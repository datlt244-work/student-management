<script setup lang="ts">
import { computed } from 'vue'

const currentDate = computed(() => {
  return new Date().toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
})

// Placeholder stats — sẽ fetch từ API sau
const stats = [
  { label: 'Total Students', value: '2,450', icon: 'school', trend: '+12%', iconBg: 'bg-orange-100 dark:bg-orange-900/20', iconColor: 'text-primary', trendColor: 'text-green-600 dark:text-green-400', trendBg: 'bg-green-50 dark:bg-green-900/20' },
  { label: 'Total Teachers', value: '120', icon: 'cast_for_education', trend: '+5%', iconBg: 'bg-blue-100 dark:bg-blue-900/20', iconColor: 'text-blue-600 dark:text-blue-400', trendColor: 'text-green-600 dark:text-green-400', trendBg: 'bg-green-50 dark:bg-green-900/20' },
  { label: 'Active Courses', value: '45', icon: 'book_2', trend: '+2%', iconBg: 'bg-purple-100 dark:bg-purple-900/20', iconColor: 'text-purple-600 dark:text-purple-400', trendColor: 'text-green-600 dark:text-green-400', trendBg: 'bg-green-50 dark:bg-green-900/20' },
  { label: 'System Logs', value: '892', icon: 'description', trend: '+8%', iconBg: 'bg-stone-100 dark:bg-stone-800', iconColor: 'text-stone-600 dark:text-stone-400', trendColor: 'text-orange-600 dark:text-orange-400', trendBg: 'bg-orange-50 dark:bg-orange-900/20' },
]

// Placeholder activity data
const activities = [
  { time: '10:23 AM', user: 'John Doe', initials: 'JD', initialsBg: 'bg-blue-100 dark:bg-blue-900', initialsColor: 'text-blue-700 dark:text-blue-300', action: 'Login', actionBg: 'bg-blue-50 dark:bg-blue-900/30', actionColor: 'text-blue-700 dark:text-blue-300', actionRing: 'ring-blue-700/10 dark:ring-blue-400/20', details: 'User logged in from IP 192.168.1.5', status: 'Success', statusType: 'success' },
  { time: '10:15 AM', user: 'Jane Smith', initials: 'JS', initialsBg: 'bg-pink-100 dark:bg-pink-900', initialsColor: 'text-pink-700 dark:text-pink-300', action: 'Update', actionBg: 'bg-purple-50 dark:bg-purple-900/30', actionColor: 'text-purple-700 dark:text-purple-300', actionRing: 'ring-purple-700/10 dark:ring-purple-400/20', details: 'Updated Grade 5 Physics Syllabus', status: 'Pending', statusType: 'pending' },
  { time: '09:45 AM', user: 'Admin User', initials: 'AD', initialsBg: 'bg-orange-100 dark:bg-orange-900', initialsColor: 'text-primary-dark dark:text-primary', action: 'Delete', actionBg: 'bg-red-50 dark:bg-red-900/30', actionColor: 'text-red-700 dark:text-red-300', actionRing: 'ring-red-600/10 dark:ring-red-400/20', details: 'Removed inactive student account', status: 'Completed', statusType: 'completed' },
  { time: '09:30 AM', user: 'System', initials: 'dns', initialsBg: 'bg-stone-100 dark:bg-stone-800', initialsColor: 'text-stone-600 dark:text-stone-300', action: 'Backup', actionBg: 'bg-stone-100 dark:bg-stone-800', actionColor: 'text-stone-600 dark:text-stone-300', actionRing: 'ring-stone-500/10 dark:ring-stone-400/20', details: 'Automated database backup', status: 'Success', statusType: 'success', isIcon: true },
  { time: '09:00 AM', user: 'Sarah Lee', initials: 'SL', initialsBg: 'bg-indigo-100 dark:bg-indigo-900', initialsColor: 'text-indigo-700 dark:text-indigo-300', action: 'Register', actionBg: 'bg-indigo-50 dark:bg-indigo-900/30', actionColor: 'text-indigo-700 dark:text-indigo-300', actionRing: 'ring-indigo-700/10 dark:ring-indigo-400/20', details: 'New student registration request', status: 'Pending', statusType: 'pending' },
]

function getStatusClasses(type: string) {
  switch (type) {
    case 'success':
      return {
        wrapper: 'bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-400 ring-green-600/20 dark:ring-green-500/20',
        dot: 'bg-green-600 dark:bg-green-400',
      }
    case 'pending':
      return {
        wrapper: 'bg-yellow-50 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-400 ring-yellow-600/20 dark:ring-yellow-500/20',
        dot: 'bg-yellow-600 dark:bg-yellow-400 animate-pulse',
      }
    case 'completed':
      return {
        wrapper: 'bg-gray-50 dark:bg-gray-700/30 text-gray-600 dark:text-gray-300 ring-gray-500/10 dark:ring-gray-400/20',
        dot: 'bg-gray-600 dark:bg-gray-400',
      }
    default:
      return {
        wrapper: 'bg-gray-50 dark:bg-gray-700/30 text-gray-600 dark:text-gray-300 ring-gray-500/10 dark:ring-gray-400/20',
        dot: 'bg-gray-600 dark:bg-gray-400',
      }
  }
}
</script>

<template>
  <div class="max-w-[1200px] w-full mx-auto p-6 md:p-10 flex flex-col gap-8">
    <!-- Dashboard Header -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4 pb-4 border-b border-stone-200 dark:border-stone-800">
      <div>
        <h1 class="text-slate-900 dark:text-white text-3xl font-bold leading-tight tracking-tight">
          Dashboard Overview
        </h1>
        <p class="text-slate-500 dark:text-slate-400 mt-2 text-sm">
          Welcome back. Here's what's happening in your school system today.
        </p>
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs font-medium px-3 py-1 rounded-full bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400 border border-green-200 dark:border-green-800 flex items-center gap-1">
          <span class="w-2 h-2 rounded-full bg-green-500 animate-pulse"></span>
          System Online
        </span>
        <div class="text-sm text-slate-500 dark:text-slate-400 font-medium font-body bg-white dark:bg-surface-dark px-3 py-1 rounded-md border border-stone-200 dark:border-stone-700 shadow-sm">
          {{ currentDate }}
        </div>
      </div>
    </div>

    <!-- Stats Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="flex flex-col justify-between gap-4 rounded-xl p-6 bg-surface-light dark:bg-surface-dark border border-stone-200 dark:border-stone-800 shadow-sm hover:shadow-md transition-shadow"
      >
        <div class="flex justify-between items-start">
          <div :class="['p-2 rounded-lg', stat.iconBg, stat.iconColor]">
            <span class="material-symbols-outlined">{{ stat.icon }}</span>
          </div>
          <span :class="['flex items-center gap-1 text-xs font-bold px-2 py-1 rounded-full', stat.trendColor, stat.trendBg]">
            <span class="material-symbols-outlined text-[14px]">trending_up</span>
            {{ stat.trend }}
          </span>
        </div>
        <div>
          <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">{{ stat.label }}</p>
          <p class="text-slate-900 dark:text-white text-3xl font-bold mt-1">{{ stat.value }}</p>
        </div>
      </div>
    </div>

    <!-- Recent Activity Section -->
    <div class="flex flex-col gap-4">
      <div class="flex items-center justify-between">
        <h2 class="text-slate-900 dark:text-white text-xl font-bold">Recent System Activity</h2>
        <button class="text-sm font-medium text-primary hover:text-primary-dark transition-colors flex items-center gap-1">
          View All Logs
          <span class="material-symbols-outlined text-[16px]">arrow_forward</span>
        </button>
      </div>

      <!-- Table Container -->
      <div class="w-full overflow-hidden rounded-xl border border-stone-200 dark:border-stone-800 bg-surface-light dark:bg-surface-dark shadow-sm">
        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="bg-stone-50 dark:bg-stone-900/50 border-b border-stone-200 dark:border-stone-800">
                <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap">Time</th>
                <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap">User</th>
                <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap">Action</th>
                <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 min-w-[300px]">Details</th>
                <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap">Status</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-stone-200 dark:divide-stone-800">
              <tr
                v-for="(activity, index) in activities"
                :key="index"
                class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors"
              >
                <td class="p-4 text-sm font-medium text-slate-600 dark:text-slate-300 whitespace-nowrap">
                  {{ activity.time }}
                </td>
                <td class="p-4 text-sm font-medium text-slate-900 dark:text-white whitespace-nowrap">
                  <div class="flex items-center gap-2">
                    <div :class="['w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold', activity.initialsBg, activity.initialsColor]">
                      <span v-if="activity.isIcon" class="material-symbols-outlined text-[14px]">{{ activity.initials }}</span>
                      <template v-else>{{ activity.initials }}</template>
                    </div>
                    {{ activity.user }}
                  </div>
                </td>
                <td class="p-4">
                  <span :class="['inline-flex items-center rounded-md px-2 py-1 text-xs font-medium ring-1 ring-inset', activity.actionBg, activity.actionColor, activity.actionRing]">
                    {{ activity.action }}
                  </span>
                </td>
                <td class="p-4 text-sm text-slate-600 dark:text-slate-400">
                  {{ activity.details }}
                </td>
                <td class="p-4">
                  <span :class="['inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium ring-1 ring-inset', getStatusClasses(activity.statusType).wrapper]">
                    <span :class="['w-1.5 h-1.5 rounded-full', getStatusClasses(activity.statusType).dot]"></span>
                    {{ activity.status }}
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

