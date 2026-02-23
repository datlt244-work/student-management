<script setup lang="ts">
import { ref } from 'vue'

const days = [
  { name: 'Mon', date: '23', isToday: false, isWeekend: false },
  { name: 'Tue', date: '24', isToday: true, isWeekend: false },
  { name: 'Wed', date: '25', isToday: false, isWeekend: false },
  { name: 'Thu', date: '26', isToday: false, isWeekend: false },
  { name: 'Fri', date: '27', isToday: false, isWeekend: false },
  { name: 'Sat', date: '28', isToday: false, isWeekend: true },
  { name: 'Sun', date: '29', isToday: false, isWeekend: true },
]

const timeSlots = [
  '08:00 AM',
  '09:00 AM',
  '10:00 AM',
  '11:00 AM',
  '12:00 PM',
  '01:00 PM',
  '02:00 PM',
]

const upcomingExams = [
  {
    month: 'Oct',
    day: '30',
    title: 'Calculus II Midterm',
    time: '10:00 AM - Room 102',
    color: 'red',
  },
  { month: 'Nov', day: '05', title: 'Physics Quiz 3', time: 'Online Portal', color: 'red' },
]

const deadlines = [
  { title: 'CS-101 Project Alpha', due: 'Due Tomorrow, 11:59 PM', isUrgent: true },
  { title: 'LIT-204 Essay Draft', due: 'Due Friday, Oct 27', isUrgent: false },
  { title: 'Lab Report 3', due: 'Due Monday, Oct 30', isUrgent: false },
]

const classes = ref([
  {
    day: 'Mon',
    start: '08:00 AM',
    end: '09:00 AM',
    title: 'CS-101 Algo',
    location: 'Rm 304',
    color: 'primary',
  },
  {
    day: 'Wed',
    start: '08:00 AM',
    end: '09:00 AM',
    title: 'CS-101 Algo',
    location: 'Rm 304',
    color: 'primary',
  },
  {
    day: 'Fri',
    start: '08:00 AM',
    end: '09:00 AM',
    title: 'CS-101 Algo',
    location: 'Rm 304',
    color: 'primary',
  },
  {
    day: 'Tue',
    start: '09:00 AM',
    end: '10:00 AM',
    title: 'LIT-204 Writing',
    location: 'Hall B',
    color: 'amber',
  },
  {
    day: 'Thu',
    start: '09:00 AM',
    end: '10:00 AM',
    title: 'LIT-204 Writing',
    location: 'Hall B',
    color: 'amber',
  },
  {
    day: 'Mon',
    start: '10:00 AM',
    duration: 1.5,
    title: 'MATH-202 Calc II',
    location: 'Rm 102',
    color: 'orange',
    timeStr: '10:00 - 11:30',
  },
  {
    day: 'Wed',
    start: '10:00 AM',
    duration: 1.5,
    title: 'MATH-202 Calc II',
    location: 'Rm 102',
    color: 'orange',
    timeStr: '10:00 - 11:30',
  },
  {
    day: 'Fri',
    start: '10:00 AM',
    duration: 1.5,
    title: 'MATH-202 Calc II',
    location: 'Rm 102',
    color: 'orange',
    timeStr: '10:00 - 11:30',
  },
  {
    day: 'Tue',
    start: '01:00 PM',
    duration: 1.5,
    title: 'PHYS-101 Lab',
    location: 'Lab 4',
    color: 'amber',
    timeStr: '01:00 - 02:30',
    isLab: true,
  },
  {
    day: 'Thu',
    start: '01:00 PM',
    duration: 1.5,
    title: 'PHYS-101 Lab',
    location: 'Lab 4',
    color: 'amber',
    timeStr: '01:00 - 02:30',
    isLab: true,
  },
])

function getClassAt(day: string, time: string) {
  return classes.value.find((c) => c.day === day && c.start === time)
}
</script>

<template>
  <div class="flex-1 flex justify-center w-full px-4 sm:px-6 py-8">
    <div class="w-full max-w-[1400px] flex flex-col gap-8">
      <!-- Page Heading & Actions -->
      <div class="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">
        <div class="flex flex-col gap-2">
          <h1
            class="text-text-main-light dark:text-text-main-dark text-3xl md:text-4xl font-black tracking-tight"
          >
            Class Schedule
          </h1>
          <p class="text-text-muted-light dark:text-text-muted-dark text-base max-w-2xl">
            Manage your weekly classes and keep track of upcoming academic events.
          </p>
        </div>
        <div
          class="flex items-center gap-3 bg-surface-light dark:bg-surface-dark p-1 rounded-xl border border-border-light dark:border-border-dark shadow-sm"
        >
          <button class="px-4 py-2 rounded-lg bg-primary text-white text-sm font-bold shadow-sm">
            Weekly
          </button>
          <button
            class="px-4 py-2 rounded-lg text-text-muted-light dark:text-text-muted-dark hover:bg-input-bg-light dark:hover:bg-input-bg-dark text-sm font-medium transition-colors"
          >
            Monthly
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <!-- Main Schedule Column -->
        <div class="lg:col-span-9 flex flex-col gap-6">
          <div
            class="bg-surface-light dark:bg-surface-dark p-4 rounded-xl border border-border-light dark:border-border-dark shadow-sm flex flex-wrap items-center justify-between gap-4"
          >
            <div class="flex items-center gap-4">
              <button
                class="p-2 hover:bg-input-bg-light dark:hover:bg-input-bg-dark rounded-lg text-text-main-light dark:text-text-main-dark transition-colors"
              >
                <span class="material-symbols-outlined">chevron_left</span>
              </button>
              <h2 class="text-xl font-bold text-text-main-light dark:text-text-main-dark">
                October 23 - 29, 2024
              </h2>
              <button
                class="p-2 hover:bg-input-bg-light dark:hover:bg-input-bg-dark rounded-lg text-text-main-light dark:text-text-main-dark transition-colors"
              >
                <span class="material-symbols-outlined">chevron_right</span>
              </button>
            </div>
            <button
              class="px-4 py-2 rounded-lg border border-primary text-primary hover:bg-primary/5 text-sm font-bold transition-colors"
            >
              Today
            </button>
          </div>

          <div
            class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark shadow-sm overflow-hidden flex flex-col"
          >
            <!-- Header Row -->
            <div class="grid grid-cols-8 border-b border-border-light dark:border-border-dark">
              <div class="p-4 border-r border-border-light dark:border-border-dark"></div>
              <div
                v-for="day in days"
                :key="day.name"
                class="p-4 text-center border-r last:border-r-0 border-border-light dark:border-border-dark"
                :class="{
                  'bg-primary/5': day.isToday,
                  'bg-input-bg-light/30 dark:bg-input-bg-dark/30': day.isWeekend,
                }"
              >
                <span
                  class="block text-xs font-medium uppercase"
                  :class="
                    day.isToday ? 'text-primary' : 'text-text-muted-light dark:text-text-muted-dark'
                  "
                  >{{ day.name }}</span
                >
                <span
                  class="block text-xl font-bold"
                  :class="
                    day.isToday ? 'text-primary' : 'text-text-main-light dark:text-text-main-dark'
                  "
                  >{{ day.date }}</span
                >
              </div>
            </div>

            <!-- Grid Content -->
            <div class="overflow-y-auto max-h-[600px] relative custom-scrollbar">
              <div
                v-for="time in timeSlots"
                :key="time"
                class="grid grid-cols-8 min-h-[100px] border-b border-border-light dark:border-border-dark last:border-0"
              >
                <div
                  class="p-2 text-xs font-medium text-text-muted-light dark:text-text-muted-dark text-right border-r border-border-light dark:border-border-dark -mt-2.5"
                >
                  {{ time }}
                </div>

                <template v-if="time === '12:00 PM'">
                  <div
                    class="border-r border-border-light dark:border-border-dark col-span-5 bg-input-bg-light/50 dark:bg-input-bg-dark/50 flex items-center justify-center"
                  >
                    <span
                      class="text-sm font-bold text-text-muted-light dark:text-text-muted-dark uppercase tracking-widest opacity-50"
                      >Lunch Break</span
                    >
                  </div>
                  <div
                    class="border-r border-border-light dark:border-border-dark bg-input-bg-light/30 dark:bg-input-bg-dark/30"
                  ></div>
                  <div class="bg-input-bg-light/30 dark:bg-input-bg-dark/30"></div>
                </template>
                <template v-else>
                  <div
                    v-for="day in days"
                    :key="`${day.name}-${time}`"
                    class="border-r last:border-r-0 border-border-light dark:border-border-dark relative p-1"
                    :class="{
                      'bg-primary/5': day.isToday,
                      'bg-input-bg-light/30 dark:bg-input-bg-dark/30': day.isWeekend,
                    }"
                  >
                    <div
                      v-if="getClassAt(day.name, time)"
                      class="absolute top-2 inset-x-1 rounded p-2 shadow-sm hover:shadow-md transition-shadow cursor-pointer flex flex-col"
                      :class="[
                        getClassAt(day.name, time)?.duration
                          ? 'bottom-[-45px] z-10 justify-center'
                          : 'bottom-2',
                        getClassAt(day.name, time)?.color === 'primary'
                          ? 'bg-orange-100 dark:bg-orange-900/40 border-l-4 border-primary'
                          : '',
                        getClassAt(day.name, time)?.color === 'amber'
                          ? 'bg-amber-100 dark:bg-amber-900/40 border-l-4 border-amber-500'
                          : '',
                        getClassAt(day.name, time)?.color === 'orange'
                          ? 'bg-orange-200 dark:bg-orange-800/40 border-l-4 border-orange-600'
                          : '',
                      ]"
                    >
                      <h4
                        class="text-xs font-bold text-text-main-light dark:text-text-main-dark truncate"
                      >
                        {{ getClassAt(day.name, time)?.title }}
                      </h4>
                      <p
                        class="text-[10px] text-text-muted-light dark:text-text-muted-dark mt-1 flex items-center gap-1"
                      >
                        <span class="material-symbols-outlined !text-[10px]">{{
                          getClassAt(day.name, time)?.isLab ? 'science' : 'location_on'
                        }}</span>
                        {{ getClassAt(day.name, time)?.location }}
                      </p>
                      <span
                        v-if="getClassAt(day.name, time)?.timeStr"
                        class="text-[10px] font-bold mt-1"
                        :class="
                          getClassAt(day.name, time)?.color === 'orange'
                            ? 'text-orange-700 dark:text-orange-300'
                            : 'text-amber-700 dark:text-amber-300'
                        "
                      >
                        {{ getClassAt(day.name, time)?.timeStr }}
                      </span>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>

        <!-- Sidebar Column -->
        <div class="lg:col-span-3 flex flex-col gap-6">
          <!-- Upcoming Exams -->
          <div
            class="bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-border-light dark:border-border-dark shadow-sm"
          >
            <h3
              class="text-lg font-bold text-text-main-light dark:text-text-main-dark mb-4 flex items-center gap-2"
            >
              <span class="material-symbols-outlined text-primary">assignment_late</span>
              Upcoming Exams
            </h3>
            <div class="flex flex-col gap-4">
              <div
                v-for="exam in upcomingExams"
                :key="exam.title"
                class="flex gap-3 items-start pb-4 border-b border-border-light dark:border-border-dark last:border-0 last:pb-0"
              >
                <div
                  class="bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 font-bold rounded-lg p-2 text-center min-w-[3.5rem]"
                >
                  <span class="text-xs block uppercase">{{ exam.month }}</span>
                  <span class="text-lg block leading-none">{{ exam.day }}</span>
                </div>
                <div>
                  <h4 class="font-bold text-text-main-light dark:text-text-main-dark text-sm">
                    {{ exam.title }}
                  </h4>
                  <p class="text-xs text-text-muted-light dark:text-text-muted-dark mt-1">
                    {{ exam.time }}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <!-- Deadlines -->
          <div
            class="bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-border-light dark:border-border-dark shadow-sm"
          >
            <h3
              class="text-lg font-bold text-text-main-light dark:text-text-main-dark mb-4 flex items-center gap-2"
            >
              <span class="material-symbols-outlined text-primary">timer</span>
              Deadlines
            </h3>
            <div class="flex flex-col gap-4">
              <div
                v-for="deadline in deadlines"
                :key="deadline.title"
                class="relative pl-4 border-l-2"
                :class="
                  deadline.isUrgent
                    ? 'border-primary'
                    : 'border-border-light dark:border-border-dark'
                "
              >
                <div
                  class="absolute left-[-5px] top-1 size-2 rounded-full"
                  :class="
                    deadline.isUrgent ? 'bg-primary' : 'bg-text-muted-light dark:bg-text-muted-dark'
                  "
                ></div>
                <h4 class="font-bold text-text-main-light dark:text-text-main-dark text-sm">
                  {{ deadline.title }}
                </h4>
                <p class="text-xs text-text-muted-light dark:text-text-muted-dark mt-0.5">
                  {{ deadline.due }}
                </p>
              </div>
            </div>
          </div>

          <button
            class="w-full py-4 rounded-xl bg-primary hover:bg-primary/90 text-text-main-light font-bold shadow-lg shadow-primary/20 transition-all flex items-center justify-center gap-2"
          >
            <span class="material-symbols-outlined">add</span>
            <span>Add Personal Event</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #e8ddce;
  border-radius: 10px;
}
.dark .custom-scrollbar::-webkit-scrollbar-thumb {
  background: #3e3223;
}
</style>
