<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { getMySchedule, getClassMembers, getClassAttendances, type StudentSchedule, type StudentClassMemberResponse, type AttendanceRecordResponse } from '@/services/scheduleService'
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

const viewMode = ref<'weekly' | 'list' | 'activity_detail' | 'members_list'>('weekly')
const selectedActivity = ref<ScheduleOccurrence | null>(null)
const classMembers = ref<StudentClassMemberResponse[]>([])
const classAttendances = ref<AttendanceRecordResponse[]>([])
const isLoadingMembers = ref(false)
const isLoadingAttendance = ref(false)

async function openActivityDetail(cls: ScheduleOccurrence) {
  selectedActivity.value = cls
  viewMode.value = 'activity_detail'
  isLoadingAttendance.value = true
  try {
    const records = await getClassAttendances(cls.classId)
    classAttendances.value = records || []
  } catch (err) {
    console.error(err)
  } finally {
    isLoadingAttendance.value = false
  }
}

const currentActivityAttendance = computed(() => {
  if (!selectedActivity.value || !classAttendances.value.length) return null
  return classAttendances.value.find(a => a.date === selectedActivity.value!.date)
})

async function viewStudentList() {
  if (!selectedActivity.value) return
  viewMode.value = 'members_list'
  isLoadingMembers.value = true
  try {
    const list = await getClassMembers(selectedActivity.value.classId)
    classMembers.value = list || []
  } catch (err) {
    console.error(err)
  } finally {
    isLoadingMembers.value = false
  }
}

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
  const targetDateObj = currentWeekDates.value[dayIndex - 1]
  if (!targetDateObj) return []
  
  const targetDateStr = `${targetDateObj.getFullYear()}-${String(targetDateObj.getMonth() + 1).padStart(2, '0')}-${String(targetDateObj.getDate()).padStart(2, '0')}`
  
  return expandedListItems.value.filter((item) => {
    if (item.date !== targetDateStr) return false
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

interface ScheduleOccurrence {
  classId: number
  className: string
  date: string // YYYY-MM-DD
  dayName: string // 'Mon', 'Tue'
  dateObj: Date
  courseName: string
  courseCode: string
  roomNumber: string
  teacherName: string
  startTime: string
  endTime: string
  classStatus: string
  colorClass: string
}

const expandedListItems = computed(() => {
  const items: ScheduleOccurrence[] = []
  scheduleData.value.forEach((item, idx) => {
    if (!item.startDate || !item.endDate) return
    const start = new Date(item.startDate)
    const end = new Date(item.endDate)
    const d = new Date(start)
    
    // JS Date.getDay(): 0=Sun, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat
    // Backend dayOfWeek: 1=Mon, 2=Tue, ..., 6=Sat, 7=Sun
    const backendToJsDay = item.dayOfWeek === 7 ? 0 : item.dayOfWeek
    
    const maxIterations = 365 // limit 1 year safe guard
    let iters = 0
    while (d <= end && iters < maxIterations) {
      if (d.getDay() === backendToJsDay) {
        const y = d.getFullYear()
        const mo = String(d.getMonth() + 1).padStart(2, '0')
        const dd = String(d.getDate()).padStart(2, '0')
        const dayMatch = Object.values(days).find(db => db.index === item.dayOfWeek)
        items.push({
          classId: item.classId,
          className: item.className || 'Group ' + item.classId,
          date: `${y}-${mo}-${dd}`,
          dayName: dayMatch ? dayMatch.name : '',
          dateObj: new Date(d),
          courseName: item.courseName,
          courseCode: item.courseCode,
          roomNumber: item.roomNumber,
          teacherName: item.teacherName,
          startTime: item.startTime,
          endTime: item.endTime,
          classStatus: item.classStatus,
          colorClass: getClassStyle(idx) || 'bg-stone-100 border-l-4 border-stone-500'
        })
      }
      d.setDate(d.getDate() + 1)
      iters++
    }
  })
  
  // Sort by date, then time
  items.sort((a, b) => {
    if (a.date !== b.date) return a.date.localeCompare(b.date)
    return a.startTime.localeCompare(b.startTime)
  })
  
  return items
})

const groupedListItems = computed(() => {
  const groups: Record<string, ScheduleOccurrence[]> = {}
  expandedListItems.value.forEach(item => {
    if (!groups[item.date]) {
      groups[item.date] = []
    }
    groups[item.date]!.push(item)
  })
  return Object.keys(groups).sort().map(date => ({
    date,
    dayName: (groups[date] && groups[date][0]) ? groups[date][0].dayName : '',
    items: groups[date] || []
  }))
})

function formatPrettyDate(dateStr: string) {
  const d = new Date(dateStr)
  return d.toLocaleDateString('en-US', {
    month: 'long',
    day: 'numeric',
    year: 'numeric'
  })
}

const weeksInSemester = computed(() => {
  const sem = semesters.value.find((s) => s.semesterId === selectedSemesterId.value)
  if (!sem || !sem.startDate || !sem.endDate) return []
  
  const start = new Date(sem.startDate)
  const end = new Date(sem.endDate)
  
  // Adjust start to the previous Monday if it's not Monday
  const startDay = start.getDay() || 7 // 1 is Monday, 7 is Sunday
  const firstMonday = new Date(start)
  firstMonday.setDate(start.getDate() - (startDay - 1))
  
  const weeks = []
  let current = new Date(firstMonday)
  
  while (current <= end) {
    const weekStart = new Date(current)
    const weekEnd = new Date(current)
    weekEnd.setDate(weekEnd.getDate() + 6)
    
    const startStr = `${String(weekStart.getDate()).padStart(2, '0')}/${String(weekStart.getMonth() + 1).padStart(2, '0')}`
    const endStr = `${String(weekEnd.getDate()).padStart(2, '0')}/${String(weekEnd.getMonth() + 1).padStart(2, '0')}`
    
    weeks.push({
      id: weekStart.toISOString(),
      label: `${startStr} To ${endStr}`,
      start: weekStart,
      end: weekEnd
    })
    
    current.setDate(current.getDate() + 7)
  }
  
  return weeks
})

const selectedWeekId = ref<string>('')

watch(weeksInSemester, (weeks) => {
  if (weeks.length > 0) {
    const now = new Date()
    const currentWeek = weeks.find(w => now >= w.start && now <= w.end)
    if (currentWeek) {
      selectedWeekId.value = currentWeek.id
    } else {
      selectedWeekId.value = weeks[0]?.id || ''
    }
  } else {
    selectedWeekId.value = ''
  }
}, { immediate: true })

const currentWeekDates = computed(() => {
  const week = weeksInSemester.value.find(w => w.id === selectedWeekId.value)
  if (!week) return []
  
  const dates = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(week.start)
    d.setDate(d.getDate() + i)
    dates.push(d)
  }
  return dates
})
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
          
          <!-- Week Selector -->
          <div v-if="viewMode === 'weekly' && weeksInSemester.length > 0" class="relative w-full md:w-56 group border-l pl-4 border-border-light dark:border-border-dark">
            <select
              v-model="selectedWeekId"
              class="w-full h-12 pl-4 pr-12 rounded-xl bg-surface-light dark:bg-surface-dark border border-border-light dark:border-border-dark text-text-main-light dark:text-text-main-dark font-bold appearance-none bg-none cursor-pointer focus:ring-2 focus:ring-primary/20 transition-all outline-none"
            >
              <option v-for="week in weeksInSemester" :key="week.id" :value="week.id">
                Week: {{ week.label }}
              </option>
            </select>
            <div
              class="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none flex items-center justify-center text-text-muted-light dark:text-text-muted-dark group-focus-within:text-primary transition-colors"
            >
              <span class="material-symbols-outlined !text-2xl !leading-none">calendar_view_week</span>
            </div>
          </div>

          <div
            class="hidden sm:flex items-center gap-3 bg-surface-light dark:bg-surface-dark p-1 rounded-xl border border-border-light dark:border-border-dark shadow-sm"
          >
            <button
              @click="viewMode = 'weekly'"
              :class="[
                'px-4 py-2 rounded-lg text-sm font-bold shadow-sm transition-colors',
                viewMode === 'weekly'
                  ? 'bg-primary text-white'
                  : 'text-text-muted-light dark:text-text-muted-dark hover:bg-input-bg-light dark:hover:bg-input-bg-dark font-medium !shadow-none'
              ]"
            >
              Weekly
            </button>
            <button
              @click="viewMode = 'list'"
              :class="[
                'px-4 py-2 rounded-lg text-sm font-bold shadow-sm transition-colors',
                viewMode === 'list'
                  ? 'bg-primary text-white'
                  : 'text-text-muted-light dark:text-text-muted-dark hover:bg-input-bg-light dark:hover:bg-input-bg-dark font-medium !shadow-none'
              ]"
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
            v-if="viewMode === 'weekly'"
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
                <span v-if="currentWeekDates[day.index - 1]" class="block text-sm font-black mt-1 text-text-main-light dark:text-text-main-dark">
                  {{ String(currentWeekDates[day.index - 1]!.getDate()).padStart(2, '0') }}/{{ String(currentWeekDates[day.index - 1]!.getMonth() + 1).padStart(2, '0') }}
                </span>
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
                      @click.stop="openActivityDetail(cls)"
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

          <!-- List View Content -->
          <div
            v-else-if="viewMode === 'list'"
            class="flex flex-col gap-6"
          >
            <div v-if="groupedListItems.length === 0" class="bg-surface-light dark:bg-surface-dark p-8 rounded-xl border border-border-light dark:border-border-dark shadow-sm text-center">
              <span class="material-symbols-outlined text-4xl text-text-muted-light dark:text-text-muted-dark mb-4">calendar_month</span>
              <h3 class="text-xl font-bold text-text-main-light dark:text-text-main-dark mb-2">No Classes Scheduled</h3>
              <p class="text-text-muted-light dark:text-text-muted-dark max-w-md mx-auto">There are no classes scheduled for you in the selected semester timeframe.</p>
            </div>
            
            <div v-for="group in groupedListItems" :key="group.date" class="flex flex-col gap-3 relative">
               <div class="flex items-center gap-4 sticky top-0 bg-background-light dark:bg-background-dark py-2 z-20">
                 <div class="w-16 h-16 shrink-0 rounded-2xl bg-primary/10 border border-primary/20 flex flex-col items-center justify-center text-primary shadow-sm">
                   <span class="text-xs uppercase font-bold opacity-80 leading-none mb-1">{{ group.dayName }}</span>
                   <span class="text-2xl font-black leading-none">{{ group.date.split('-')[2] }}</span>
                 </div>
                 <h3 class="text-xl font-bold text-text-main-light dark:text-text-main-dark hidden sm:block">
                   {{ formatPrettyDate(group.date) }}
                 </h3>
                 <div class="h-px bg-border-light dark:bg-border-dark flex-1"></div>
               </div>
               
               <div class="flex flex-col gap-3 pl-4 sm:pl-20">
                 <div v-for="(cls, idx) in group.items" :key="idx"
                      @click.stop="openActivityDetail(cls)"
                      class="bg-surface-light dark:bg-surface-dark p-4 sm:p-5 rounded-xl border shadow-sm hover:shadow-md transition-shadow relative overflow-hidden flex flex-col sm:flex-row gap-4 sm:items-center cursor-pointer"
                      :class="cls.colorClass.split(' ').slice(0, 2).join(' ')"
                 >
                   <!-- Left Color Accent Bar -->
                   <div class="absolute left-0 top-0 bottom-0 w-1.5" :class="cls.colorClass.match(/border-(\w+-\d+|(primary|secondary)(-\w+)?)/)?.[0]?.replace('border-', 'bg-') || 'bg-primary'"></div>
                   
                   <div class="flex-1 flex gap-4 min-w-0">
                     <div class="flex flex-col gap-0.5 justify-center shrink-0 w-24 border-r border-border-light dark:border-border-dark/30 pr-4">
                       <span class="font-black text-text-main-light dark:text-text-main-dark text-lg whitespace-nowrap">{{ formatTime(cls.startTime) }}</span>
                       <span class="text-xs font-bold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider">{{ formatTime(cls.endTime) }}</span>
                     </div>
                     <div class="flex flex-col gap-1.5 min-w-0 flex-1 pl-1">
                       <h4 class="font-bold text-text-main-light dark:text-text-main-dark text-lg truncate">{{ cls.courseName }} <span class="text-text-muted-light dark:text-text-muted-dark font-normal opacity-70">({{ cls.courseCode }})</span></h4>
                       <div class="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-text-muted-light dark:text-text-muted-dark">
                         <span class="flex items-center gap-1"><span class="material-symbols-outlined !text-[16px] text-primary">person</span> {{ cls.teacherName }}</span>
                         <span class="flex items-center gap-1"><span class="material-symbols-outlined !text-[16px] text-primary">location_on</span> {{ cls.roomNumber }}</span>
                       </div>
                     </div>
                   </div>
                   
                   <div class="shrink-0 sm:self-center ml-2 sm:ml-0 self-start">
                     <span class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-bold uppercase tracking-wider bg-primary/10 text-primary">
                       {{ cls.classStatus }}
                     </span>
                   </div>
                 </div>
               </div>
            </div>
          </div>

          <!-- Activity Detail View -->
          <div v-else-if="viewMode === 'activity_detail' && selectedActivity" class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark shadow-sm p-6 overflow-hidden flex flex-col gap-6">
            <div class="flex items-center justify-between border-b border-border-light dark:border-border-dark pb-4">
              <h2 class="text-2xl font-bold">Activity detail</h2>
              <button @click="viewMode = 'weekly'" class="flex items-center gap-1 px-4 py-2 border border-border-light dark:border-border-dark rounded hover:bg-black/5 dark:hover:bg-white/5 transition-colors font-semibold text-sm">
                <span class="material-symbols-outlined text-[18px]">arrow_back</span>
                Back to Schedule
              </button>
            </div>
            <div class="overflow-x-auto">
              <table class="w-full text-sm text-left">
                <tbody>
                  <tr class="border-b last:border-0 border-border-light dark:border-border-dark">
                    <td class="font-bold py-4 w-1/3">Date:</td>
                    <td class="py-4 font-medium">{{ selectedActivity.dayName }} {{ formatPrettyDate(selectedActivity.date) }}</td>
                  </tr>
                  <tr class="border-b last:border-0 border-border-light dark:border-border-dark">
                    <td class="font-bold py-4">Student group:</td>
                    <td class="py-4">
                      <span class="text-primary font-bold cursor-pointer hover:underline inline-flex items-center gap-1 group" @click="viewStudentList">
                        {{ selectedActivity.className }}
                        <span class="material-symbols-outlined !text-[14px] opacity-70">open_in_new</span>
                      </span>
                    </td>
                  </tr>
                  <tr class="border-b last:border-0 border-border-light dark:border-border-dark">
                    <td class="font-bold py-4">Instructor:</td>
                    <td class="py-4 flex items-center gap-2">
                       <span class="font-bold">{{ selectedActivity.teacherName }}</span>
                       <span class="bg-orange-500 text-white px-2.5 py-0.5 rounded text-xs font-bold cursor-pointer hover:bg-orange-600 transition-colors">Meet URL</span>
                    </td>
                  </tr>
                  <tr class="border-b last:border-0 border-border-light dark:border-border-dark">
                    <td class="font-bold py-4">Course:</td>
                    <td class="py-4"><span class="font-bold">{{ selectedActivity.courseName }}</span> ({{ selectedActivity.courseCode }})</td>
                  </tr>
                  <tr class="border-b last:border-0 border-border-light dark:border-border-dark">
                    <td class="font-bold py-4">Course session number:</td>
                    <td class="py-4">-</td>
                  </tr>
                  <tr class="border-b last:border-0 border-border-light dark:border-border-dark">
                    <td class="font-bold py-4">Course session type:</td>
                    <td class="py-4">-</td>
                  </tr>
                  <tr class="border-b last:border-0 border-border-light dark:border-border-dark">
                    <td class="font-bold py-4">Course session description:</td>
                    <td class="py-4">-</td>
                  </tr>
                  <tr class="border-b last:border-0 border-border-light dark:border-border-dark">
                    <td class="font-bold py-4">Campus/Programme:</td>
                    <td class="py-4">-</td>
                  </tr>
                  <tr class="border-b last:border-0 border-border-light dark:border-border-dark">
                    <td class="font-bold py-4">Attendance:</td>
                    <td class="py-4">
                       <span v-if="isLoadingAttendance">Loading...</span>
                       <span v-else-if="new Date(selectedActivity.date) > new Date()" class="text-text-muted-light dark:text-text-muted-dark italic font-medium">Future</span>
                       <span v-else-if="!currentActivityAttendance" class="text-text-muted-light dark:text-text-muted-dark font-medium">Not yet taken</span>
                       <span v-else :class="{
                          'text-green-600 font-bold': currentActivityAttendance.status === 'ATTENDED',
                          'text-red-600 font-bold': currentActivityAttendance.status === 'ABSENT'
                       }">
                          {{ currentActivityAttendance.status }}
                       </span>
                    </td>
                  </tr>
                  <tr class="border-b last:border-0 border-border-light dark:border-border-dark">
                    <td class="font-bold py-4">Record time:</td>
                    <td class="py-4">
                       <span v-if="isLoadingAttendance">...</span>
                       <span v-else-if="currentActivityAttendance">{{ new Date(currentActivityAttendance.recordTime).toLocaleString() }}</span>
                       <span v-else>-</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Class Members View -->
          <div v-else-if="viewMode === 'members_list'" class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark shadow-sm p-6 overflow-hidden flex flex-col gap-6">
            <div class="flex items-center justify-between border-b border-border-light dark:border-border-dark pb-4">
              <h2 class="text-2xl font-bold">Class Members of <span class="text-primary">{{ selectedActivity?.className }}</span></h2>
              <button @click="viewMode = 'activity_detail'" class="flex items-center gap-1 px-4 py-2 border border-border-light dark:border-border-dark rounded hover:bg-black/5 dark:hover:bg-white/5 transition-colors font-semibold text-sm">
                <span class="material-symbols-outlined text-[18px]">arrow_back</span>
                Back
              </button>
            </div>
            <div v-if="isLoadingMembers" class="py-16 flex flex-col justify-center items-center gap-3 text-primary font-bold">
               <span class="material-symbols-outlined animate-spin text-4xl">progress_activity</span>
               Processing...
            </div>
            <div v-else class="overflow-x-auto rounded-lg border border-border-light dark:border-border-dark shadow-sm">
              <table class="w-full text-sm text-left">
                <thead class="bg-stone-50/50 dark:bg-stone-900/50 border-b border-border-light dark:border-border-dark">
                  <tr>
                    <th class="p-4 font-bold tracking-wider text-center w-16 text-xs uppercase text-text-muted-light dark:text-text-muted-dark">Index</th>
                    <th class="p-4 font-bold tracking-wider text-center w-24 text-xs uppercase text-text-muted-light dark:text-text-muted-dark">Image</th>
                    <th class="p-4 font-bold tracking-wider text-xs uppercase text-text-muted-light dark:text-text-muted-dark">Member Code</th>
                    <th class="p-4 font-bold tracking-wider text-xs uppercase text-text-muted-light dark:text-text-muted-dark">Surname</th>
                    <th class="p-4 font-bold tracking-wider text-xs uppercase text-text-muted-light dark:text-text-muted-dark">Given Name</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-border-light dark:divide-border-dark">
                  <tr v-for="(member, idx) in classMembers" :key="member.studentCode" class="hover:bg-black/5 dark:hover:bg-white/5 transition-colors">
                    <td class="p-4 text-center font-bold text-text-muted-light dark:text-text-muted-dark border-r border-border-light dark:border-border-dark">{{ idx + 1 }}</td>
                    <td class="p-4 border-r border-border-light dark:border-border-dark">
                      <div class="w-12 h-16 bg-black/10 dark:bg-white/10 mx-auto rounded overflow-hidden flex items-center justify-center relative shadow-inner">
                        <img :src="`https://ui-avatars.com/api/?name=${member.firstName}+${member.lastName}&background=random&color=fff&size=64`" class="absolute inset-0 w-full h-full object-cover" />
                      </div>
                    </td>
                    <td class="p-4 font-mono font-bold text-primary">{{ member.studentCode }}</td>
                    <td class="p-4 font-bold">{{ member.lastName }}</td>
                    <td class="p-4">{{ member.firstName }}</td>
                  </tr>
                  <tr v-if="classMembers.length === 0">
                    <td colspan="5" class="p-8 text-center text-text-muted-light dark:text-text-muted-dark font-medium">No members found in this class.</td>
                  </tr>
                </tbody>
              </table>
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
