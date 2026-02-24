<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { getMySchedule, type StudentSchedule } from '@/services/scheduleService'
import { semesterService, type SemesterResponse } from '@/services/semesterService'

const days = [
  { name: 'Mon', index: 1 },
  { name: 'Tue', index: 2 },
  { name: 'Wed', index: 3 },
  { name: 'Thu', index: 4 },
  { name: 'Fri', index: 5 },
  { name: 'Sat', index: 6 },
  { name: 'Sun', index: 7 },
]

const timeSlots = [
  '08:00',
  '09:00',
  '10:00',
  '11:00',
  '12:00',
  '13:00',
  '14:00',
  '15:00',
  '16:00',
  '17:00',
]

interface UpcomingExam {
  title: string
  month: string
  day: string
  time: string
}

interface Deadline {
  title: string
  due: string
  isUrgent: boolean
}

// Sidebar data (could be fetched from backend later)
const upcomingExams = ref<UpcomingExam[]>([])
const deadlines = ref<Deadline[]>([])

const semesters = ref<SemesterResponse[]>([])
const selectedSemesterId = ref<number | null>(null)
const scheduleData = ref<StudentSchedule[]>([])
const isLoading = ref(false)
const error = ref('')

async function fetchSemesters() {
  try {
    const response = await semesterService.getAllSemesters()
    semesters.value = response.result

    // Auto-select current semester
    const current = semesters.value.find((s) => s.isCurrent)
    if (current) {
      selectedSemesterId.value = current.semesterId
    } else if (semesters.value.length > 0) {
      selectedSemesterId.value = semesters.value[0]?.semesterId || null
    }
  } catch (err) {
    console.error('Failed to fetch semesters:', err)
  }
}

async function fetchSchedule() {
  if (!selectedSemesterId.value) return

  isLoading.value = true
  error.value = ''
  try {
    scheduleData.value = await getMySchedule(selectedSemesterId.value)
  } catch (err: unknown) {
    error.value = (err as Error).message || 'Failed to load schedule'
    console.error(err)
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchSemesters()
})

watch(selectedSemesterId, () => {
  fetchSchedule()
})

function getDurationInHours(start: string, end: string) {
  if (!start || !end) return 1
  const sParts = start.split(':').map(Number)
  const eParts = end.split(':').map(Number)
  const h1 = sParts[0] || 0
  const m1 = sParts[1] || 0
  const h2 = eParts[0] || 0
  const m2 = eParts[1] || 0
  return h2 + m2 / 60 - (h1 + m1 / 60)
}

function getStartMinuteOffset(start: string | undefined) {
  if (!start) return 0
  const parts = start.split(':')
  const m = parts.length > 1 ? parseInt(parts[1] as string) : 0
  return (m / 60) * 100
}

function getClassesStartingAt(dayIndex: number, hour: number) {
  return scheduleData.value.filter((item) => {
    if (item.dayOfWeek !== dayIndex) return false
    const parts = item.startTime.split(':')
    const startHour = parts.length > 0 ? parseInt(parts[0] as string) : -1
    return startHour === hour
  })
}

const colors = [
  'bg-orange-100 dark:bg-orange-900/40 border-l-4 border-primary',
  'bg-amber-100 dark:bg-amber-900/40 border-l-4 border-amber-500',
  'bg-blue-100 dark:bg-blue-900/40 border-l-4 border-blue-500',
  'bg-green-100 dark:bg-green-900/40 border-l-4 border-green-500',
]

function getClassStyle(index: number) {
  return colors[index % colors.length]
}

const currentSemesterName = computed(() => {
  const sem = semesters.value.find((s) => s.semesterId === selectedSemesterId.value)
  return sem ? sem.displayName : 'Select Semester'
})

function formatTime(time: string | undefined) {
  if (!time) return ''
  return time.substring(0, 5)
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
        <div class="flex flex-wrap items-center gap-4">
          <!-- Semester Selector -->
          <div class="relative w-full md:w-72 group">
            <select
              v-model="selectedSemesterId"
              class="w-full h-12 pl-4 pr-12 rounded-xl bg-surface-light dark:bg-surface-dark border border-border-light dark:border-border-dark text-text-main-light dark:text-text-main-dark font-bold appearance-none bg-none cursor-pointer focus:ring-2 focus:ring-primary/20 transition-all outline-none"
            >
              <option v-for="sem in semesters" :key="sem.semesterId" :value="sem.semesterId">
                {{ sem.displayName }}
              </option>
            </select>
            <div
              class="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none flex items-center justify-center text-text-muted-light dark:text-text-muted-dark group-focus-within:text-primary transition-colors"
            >
              <span class="material-symbols-outlined !text-2xl !leading-none">unfold_more</span>
            </div>
          </div>

          <div
            class="hidden sm:flex items-center gap-3 bg-surface-light dark:bg-surface-dark p-1 rounded-xl border border-border-light dark:border-border-dark shadow-sm"
          >
            <button class="px-4 py-2 rounded-lg bg-primary text-white text-sm font-bold shadow-sm">
              Weekly
            </button>
            <button
              class="px-4 py-2 rounded-lg text-text-muted-light dark:text-text-muted-dark hover:bg-input-bg-light dark:hover:bg-input-bg-dark text-sm font-medium transition-colors"
            >
              List
            </button>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <!-- Main Schedule Column -->
        <div class="lg:col-span-9 flex flex-col gap-6">
          <div
            class="bg-surface-light dark:bg-surface-dark p-4 rounded-xl border border-border-light dark:border-border-dark shadow-sm flex flex-wrap items-center justify-between gap-4"
          >
            <div class="flex items-center gap-4">
              <h2 class="text-xl font-bold text-text-main-light dark:text-text-main-dark">
                {{ currentSemesterName }}
              </h2>
            </div>
            <div v-if="isLoading" class="flex items-center gap-2 text-primary font-bold text-sm">
              <span class="material-symbols-outlined animate-spin text-lg">progress_activity</span>
              <span>Loading Schedule...</span>
            </div>
            <div
              v-else-if="scheduleData.length > 0"
              class="text-xs font-bold px-3 py-1.5 rounded-full bg-primary/10 text-primary"
            >
              {{ scheduleData.length }} Classes Scheduled
            </div>
          </div>

          <div
            class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark shadow-sm overflow-hidden flex flex-col"
          >
            <!-- Header Row -->
            <div
              class="grid grid-cols-8 border-b border-border-light dark:border-border-dark bg-stone-50/50 dark:bg-stone-900/50"
            >
              <div class="p-4 border-r border-border-light dark:border-border-dark"></div>
              <div
                v-for="day in days"
                :key="day.name"
                class="p-4 text-center border-r last:border-r-0 border-border-light dark:border-border-dark"
              >
                <span
                  class="block text-xs font-bold uppercase text-text-muted-light dark:text-text-muted-dark tracking-wider"
                  >{{ day.name }}</span
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

                <template v-if="time === '12:00'">
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
                    class="border-r last:border-r-0 border-border-light dark:border-border-dark relative"
                  >
                    <!-- Classes starting in this hour slot -->
                    <div
                      v-for="(cls, idx) in getClassesStartingAt(
                        day.index,
                        parseInt(time.split(':')[0] as string),
                      )"
                      :key="idx"
                      class="absolute left-1 right-1 rounded-lg p-2.5 shadow-sm hover:shadow-md transition-all cursor-pointer flex flex-col group border border-transparent hover:border-primary/30 z-10"
                      :class="getClassStyle(idx)"
                      :style="{
                        top: `calc(${getStartMinuteOffset(cls.startTime)}% + 4px)`,
                        height: `calc(${getDurationInHours(cls.startTime, cls.endTime) * 100}% - 8px)`,
                      }"
                    >
                      <div class="flex items-start justify-between">
                        <h4
                          class="text-xs font-bold text-text-main-light dark:text-text-main-dark leading-tight line-clamp-2"
                        >
                          {{ cls.courseName }}
                        </h4>
                      </div>
                      <div class="mt-2 space-y-1 overflow-hidden">
                        <p
                          class="text-[10px] text-text-muted-light dark:text-text-muted-dark flex items-center gap-1"
                        >
                          <span class="material-symbols-outlined !text-[12px] text-primary"
                            >location_on</span
                          >
                          {{ cls.roomNumber }}
                        </p>
                        <p
                          class="text-[10px] text-text-muted-light dark:text-text-muted-dark flex items-center gap-1"
                        >
                          <span class="material-symbols-outlined !text-[12px] text-primary"
                            >person</span
                          >
                          <span class="truncate">{{ cls.teacherName }}</span>
                        </p>
                        <div class="flex items-center justify-between pt-1">
                          <span
                            class="text-[10px] font-bold text-primary bg-primary/10 px-1.5 py-0.5 rounded shrink-0"
                          >
                            {{ formatTime(cls.startTime) }} - {{ formatTime(cls.endTime) }}
                          </span>
                        </div>
                      </div>
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
              <div
                v-if="upcomingExams.length === 0"
                class="text-xs text-text-muted-light py-4 text-center"
              >
                No upcoming exams
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
              <div
                v-if="deadlines.length === 0"
                class="text-xs text-text-muted-light py-4 text-center"
              >
                No upcoming deadlines
              </div>
            </div>
          </div>

          <button
            class="w-full py-4 rounded-xl bg-primary hover:bg-primary/90 text-white font-bold shadow-lg shadow-primary/20 transition-all flex items-center justify-center gap-2"
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
