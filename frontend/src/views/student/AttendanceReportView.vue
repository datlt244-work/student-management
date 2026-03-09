<template>
  <div class="p-6 md:p-8 max-w-[1600px] mx-auto space-y-8 animate-fade-in">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-3xl font-black tracking-tight text-text-main-light dark:text-text-main-dark">
          Attendance Report
        </h1>
        <p class="text-text-muted-light dark:text-text-muted-dark mt-1 text-sm md:text-base">
          Track your presence and view detailed attendance records for each course.
        </p>
      </div>
    </div>

    <!-- Error State -->
    <div
      v-if="error"
      class="p-4 rounded-xl border border-red-200 bg-red-50 text-red-600 dark:border-red-900/50 dark:bg-red-900/20 dark:text-red-400 font-bold shadow-sm"
    >
      {{ error }}
    </div>

    <!-- Main Content Layout -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 relative">
      <!-- Left Sidebar: Terms and Courses -->
      <div class="lg:col-span-3 flex flex-col gap-4 sticky top-6 self-start max-h-[calc(100vh-200px)] overflow-y-auto custom-scrollbar">
        <div class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark shadow-sm overflow-hidden flex flex-col h-full">
          <div class="p-4 border-b border-border-light dark:border-border-dark bg-stone-50/50 dark:bg-stone-900/50 flex justify-between items-center z-10 sticky top-0">
             <h2 class="font-bold text-sm tracking-wider uppercase text-text-muted-light dark:text-text-muted-dark">Term / Course</h2>
          </div>
          <div class="p-2 space-y-2">
            <!-- Semesters List -->
            <div v-for="semester in semesters" :key="semester.semesterId" class="space-y-1">
               <button 
                 @click="selectSemester(semester.semesterId)"
                 class="w-full text-left px-3 py-2 rounded-lg font-bold text-sm transition-colors flex items-center justify-between"
                 :class="selectedSemesterId === semester.semesterId ? 'bg-primary/10 text-primary' : 'hover:bg-black/5 dark:hover:bg-white/5 text-text-main-light dark:text-text-main-dark'"
               >
                  {{ semester.displayName }}
                  <span class="material-symbols-outlined !text-[18px] transition-transform" :class="{'rotate-180': selectedSemesterId === semester.semesterId}">expand_more</span>
               </button>
               
               <!-- Courses for Selected Semester -->
               <div v-if="selectedSemesterId === semester.semesterId" class="pl-4 space-y-1 pr-1 pb-2">
                  <div v-if="isLoadingSchedule" class="px-3 py-2 text-xs text-text-muted-light font-medium italic">Loading courses...</div>
                  <div v-else-if="uniqueCourses.length === 0" class="px-3 py-2 text-xs text-text-muted-light font-medium italic">No courses found</div>
                  <button 
                    v-for="course in uniqueCourses" :key="course.courseCode"
                    @click="selectCourse(course.courseCode)"
                    class="w-full text-left px-3 py-2 rounded-lg text-sm transition-colors block truncate"
                    :class="selectedCourseCode === course.courseCode ? 'bg-black/5 dark:bg-white/5 font-bold text-text-main-light dark:text-text-main-dark border-l-2 border-primary' : 'text-text-muted-light dark:text-text-muted-dark hover:text-text-main-light dark:hover:text-text-main-dark hover:bg-black/5 dark:hover:bg-white/5 border-l-2 border-transparent'"
                    :title="course.courseName"
                  >
                     {{ course.courseName }} <span class="opacity-70 text-xs">({{ course.courseCode }})</span>
                  </button>
               </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Content: Report Table -->
      <div class="lg:col-span-9 flex flex-col gap-6">
        <div v-if="!selectedCourseCode" class="bg-surface-light dark:bg-surface-dark p-8 rounded-xl border border-border-light dark:border-border-dark shadow-sm text-center flex flex-col items-center justify-center min-h-[400px]">
           <img src="https://ui-avatars.com/api/?name=Select+Course&background=f3f4f6&color=9ca3af&size=128" alt="Placeholder" class="rounded-full w-24 h-24 mb-4 opacity-50 grayscale" />
           <h3 class="text-xl font-bold text-text-main-light dark:text-text-main-dark mb-2">Select a Course</h3>
           <p class="text-text-muted-light dark:text-text-muted-dark max-w-sm">Please select a term and a course from the left sidebar to view detailed attendance records.</p>
        </div>
        
        <div v-else class="space-y-6">
           <!-- Attendance Status Warning Banner -->
           <div v-if="absentPercentage !== null && absentPercentage >= 0" 
                :class="absentPercentage >= 20 ? 'bg-red-50 border-red-200 text-red-700 dark:bg-red-900/20 dark:border-red-800 dark:text-red-400' : (absentPercentage >= 15 ? 'bg-amber-50 border-amber-200 text-amber-700 dark:bg-amber-900/20 dark:border-amber-800 dark:text-amber-400' : 'bg-primary/5 border-primary/20 text-text-main-light dark:text-text-main-dark')"
                class="p-5 rounded-2xl border flex items-start gap-4 shadow-sm transition-all duration-300">
                <div class="p-2 rounded-full h-fit w-fit" :class="absentPercentage >= 20 ? 'bg-red-100 dark:bg-red-900/40 text-red-600' : (absentPercentage >= 15 ? 'bg-amber-100 dark:bg-amber-900/40 text-amber-600' : 'bg-primary/10 text-primary')">
                   <span class="material-symbols-outlined block">{{ absentPercentage >= 20 ? 'dangerous' : (absentPercentage >= 15 ? 'warning' : 'fact_check') }}</span>
                </div>
                <div class="flex-1">
                    <h4 class="font-black text-sm uppercase tracking-wider">
                       {{ absentPercentage >= 20 ? 'CẢNH BÁO: ĐÃ VƯỢT QUÁ GIỚI HẠN VẮNG MẶT' : (absentPercentage >= 15 ? 'CẢNH BÁO: SẮP VƯỢT QUÁ GIỚI HẠN VẮNG MẶT' : 'THÔNG TIN CHUNG VỀ ĐIỂM DANH') }}
                    </h4>
                    <p class="text-sm mt-1.5 font-medium leading-relaxed">
                       Tổng số buổi học của môn này là <span class="font-bold underline">{{ totalSessionsCount }} buổi</span>. 
                       Bạn được phép vắng tối đa <span class="font-bold text-lg leading-none">{{ maxAllowedAbsent }} buổi</span> (20%).
                       Hiện tại bạn đã vắng <span class="font-bold text-lg leading-none" :class="absentPercentage >= 20 ? 'text-red-600' : (absentPercentage >= 15 ? 'text-amber-600' : 'text-primary')">{{ totalAbsent }} buổi</span>.
                       <br/>
                       <span v-if="absentPercentage >= 20" class="font-black italic block mt-1">Lưu ý: Bạn đã KHÔNG đủ điều kiện dự thi vì vượt hạn định 20% vắng mặt.</span>
                       <span v-else-if="absentPercentage >= 15" class="italic block mt-1">Cần cẩn thận! Bạn chỉ còn được phép vắng thêm {{ maxAllowedAbsent - totalAbsent }} buổi nữa.</span>
                       <span v-else class="italic opacity-80 block mt-1">Bạn vẫn đang nằm trong giới hạn an toàn để dự thi.</span>
                    </p>
                </div>
           </div>

           <div class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark shadow-sm overflow-hidden flex flex-col">
              <!-- Table Header Info -->
              <div class="p-6 border-b border-border-light dark:border-border-dark bg-stone-50/50 dark:bg-stone-900/50 flex flex-wrap gap-4 items-center justify-between">
                 <div>
                    <h2 class="text-xl font-black text-text-main-light dark:text-text-main-dark">{{ currentCourseInfo?.courseName }}</h2>
                    <p class="text-sm font-bold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider mt-1">{{ currentCourseInfo?.courseCode }}</p>
                 </div>
                 <div v-if="absentPercentage !== null" class="flex items-center gap-2 bg-white dark:bg-stone-800 px-4 py-2 rounded-lg border border-border-light dark:border-border-dark shadow-sm">
                    <span class="text-xs uppercase font-bold text-text-muted-light tracking-wider">Tỷ lệ vắng:</span>
                    <span class="text-lg font-black" :class="absentPercentage >= 20 ? 'text-red-500' : 'text-text-main-light dark:text-text-main-dark'">{{ absentPercentage }}%</span>
                    <span class="text-xs font-medium text-text-muted-light">({{ totalAbsent }} / {{ totalSessionsCount }})</span>
                 </div>
              </div>

              <!-- Table -->
              <div class="overflow-x-auto">
                <table class="w-full text-sm text-left align-middle">
                  <thead class="bg-stone-50/50 dark:bg-stone-900/50 border-b border-border-light dark:border-border-dark">
                    <tr>
                      <th class="p-4 font-bold tracking-wider text-center w-12 text-xs uppercase text-text-muted-light dark:text-text-muted-dark">No.</th>
                      <th class="p-4 font-bold tracking-wider text-xs uppercase text-text-muted-light dark:text-text-muted-dark">Date</th>
                      <th class="p-4 font-bold tracking-wider text-xs uppercase text-text-muted-light dark:text-text-muted-dark">Slot</th>
                      <th class="p-4 font-bold tracking-wider text-xs uppercase text-text-muted-light dark:text-text-muted-dark">Room</th>
                      <th class="p-4 font-bold tracking-wider text-xs uppercase text-text-muted-light dark:text-text-muted-dark">Lecturer</th>
                      <th class="p-4 font-bold tracking-wider text-xs uppercase text-text-muted-light dark:text-text-muted-dark">Group Name</th>
                      <th class="p-4 font-bold tracking-wider text-xs uppercase text-text-muted-light dark:text-text-muted-dark">Attendance Status</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-border-light dark:divide-border-dark relative">
                     <!-- Loading overlay -->
                     <tr v-if="isLoadingAttendances">
                        <td colspan="7" class="p-0">
                           <div class="absolute inset-0 bg-white/50 dark:bg-black/50 backdrop-blur-[2px] z-10 flex items-center justify-center min-h-[200px]">
                              <div class="flex items-center gap-2 text-primary font-bold bg-white dark:bg-stone-800 px-4 py-2 rounded-full shadow-lg">
                                 <span class="material-symbols-outlined animate-spin">progress_activity</span>
                                 Loading records...
                              </div>
                           </div>
                        </td>
                     </tr>

                     <tr v-for="(cls, idx) in occurrencesOfSelectedCourse" :key="idx" class="hover:bg-black/5 dark:hover:bg-white/5 transition-colors">
                        <td class="p-4 text-center font-bold text-text-muted-light dark:text-text-muted-dark border-r border-border-light dark:border-border-dark">{{ idx + 1 }}</td>
                        <td class="p-4 border-r border-border-light dark:border-border-dark font-medium whitespace-nowrap">
                           <div class="flex items-center gap-2">
                              <span class="w-10 text-xs uppercase font-bold text-text-muted-light">{{ cls.dayName }}</span>
                              <span>{{ formatPrettyDate(cls.date) }}</span>
                           </div>
                        </td>
                        <td class="p-4 border-r border-border-light dark:border-border-dark">
                           <span class="bg-black/5 dark:bg-white/5 border border-border-light dark:border-border-dark px-2 py-1 rounded text-xs font-bold text-text-main-light dark:text-text-main-dark whitespace-nowrap">
                              {{ formatTime(cls.startTime) }}-{{ formatTime(cls.endTime) }}
                           </span>
                        </td>
                        <td class="p-4 border-r border-border-light dark:border-border-dark">{{ cls.roomNumber }}</td>
                        <td class="p-4 border-r border-border-light dark:border-border-dark">{{ cls.teacherName }}</td>
                        <td class="p-4 border-r border-border-light dark:border-border-dark font-mono text-xs">{{ cls.className }}</td>
                        <td class="p-4 font-bold">
                           <span v-if="new Date(cls.date) > new Date()" class="text-text-muted-light dark:text-text-muted-dark italic font-normal text-xs">Future</span>
                           <span v-else-if="!getAttendanceStatus(cls.date)" class="text-text-muted-light dark:text-text-muted-dark italic font-normal text-xs">-</span>
                           <span v-else :class="{
                              'text-green-600': getAttendanceStatus(cls.date) === 'ATTENDED',
                              'text-red-600': getAttendanceStatus(cls.date) === 'ABSENT'
                           }">
                              {{ getAttendanceStatus(cls.date) }}
                              <span v-if="getAttendanceStatus(cls.date) === 'ABSENT' && !classAttendances[cls.classId]?.some(r => r.date === cls.date)" class="text-[10px] italic opacity-60 block font-normal text-text-muted-light dark:text-text-muted-dark leading-tight">
                                 (System auto)
                              </span>
                           </span>
                        </td>
                     </tr>
                     <tr v-if="occurrencesOfSelectedCourse.length === 0">
                        <td colspan="7" class="p-8 text-center text-text-muted-light dark:text-text-muted-dark font-medium">No schedule found for this course.</td>
                     </tr>
                  </tbody>
                </table>
              </div>
           </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { semesterService, type SemesterResponse } from '@/services/semesterService'
import { getMySchedule, getClassAttendances, type StudentSchedule, type AttendanceRecordResponse } from '@/services/scheduleService'

const days = [
  { name: 'Mon', index: 1 },
  { name: 'Tue', index: 2 },
  { name: 'Wed', index: 3 },
  { name: 'Thu', index: 4 },
  { name: 'Fri', index: 5 },
  { name: 'Sat', index: 6 },
  { name: 'Sun', index: 7 },
]

const semesters = ref<SemesterResponse[]>([])
const selectedSemesterId = ref<number | null>(null)
const scheduleData = ref<StudentSchedule[]>([])
const isLoadingSchedule = ref(false)
const error = ref('')

const selectedCourseCode = ref<string | null>(null)
const classAttendances = ref<Record<number, AttendanceRecordResponse[]>>({}) // mapping classId -> attendances
const isLoadingAttendances = ref(false)

onMounted(async () => {
  try {
    const response = await semesterService.getAllSemesters()
    semesters.value = response.result
    const current = semesters.value.find((s) => s.isCurrent)
    if (current) {
      selectedSemesterId.value = current.semesterId
    } else if (semesters.value.length > 0) {
      selectedSemesterId.value = semesters.value[0]?.semesterId || null
    }
  } catch (err: unknown) {
    error.value = (err as Error).message || 'Failed to initialize semesters'
  }
})

watch(selectedSemesterId, async (newVal) => {
  if (!newVal) return
  isLoadingSchedule.value = true
  selectedCourseCode.value = null
  scheduleData.value = []
  try {
    scheduleData.value = await getMySchedule(newVal)
  } catch (err: unknown) {
    error.value = (err as Error).message || 'Failed to fetch schedule'
  } finally {
    isLoadingSchedule.value = false
  }
})

const uniqueCourses = computed(() => {
   const map = new Map<string, { courseCode: string, courseName: string }>()
   scheduleData.value.forEach(item => {
      if (!map.has(item.courseCode)) {
         map.set(item.courseCode, { courseCode: item.courseCode, courseName: item.courseName })
      }
   })
   return Array.from(map.values())
})

function selectSemester(id: number) {
   if (selectedSemesterId.value === id) {
      selectedSemesterId.value = null // toggle close
   } else {
      selectedSemesterId.value = id
   }
}

async function selectCourse(code: string) {
   selectedCourseCode.value = code
   
   // Fetch attendances for all classes of this course
   const courseClasses = scheduleData.value.filter(c => c.courseCode === code)
   const uniqueClassIds = Array.from(new Set(courseClasses.map(c => c.classId)))
   
   isLoadingAttendances.value = true
   try {
      for (const classId of uniqueClassIds) {
         if (!classAttendances.value[classId]) {
            const att = await getClassAttendances(classId)
            classAttendances.value[classId] = att || []
         }
      }
   } catch (err) {
      console.error('Failed to load attendances', err)
   } finally {
      isLoadingAttendances.value = false
   }
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
}

const occurrencesOfSelectedCourse = computed(() => {
   if (!selectedCourseCode.value) return []
   const courseData = scheduleData.value.filter(c => c.courseCode === selectedCourseCode.value)
   const items: ScheduleOccurrence[] = []
   
   courseData.forEach((item) => {
    if (!item.startDate || !item.endDate) return
    const start = new Date(item.startDate)
    const end = new Date(item.endDate)
    const d = new Date(start)
    
    // Day logic
    const backendToJsDay = item.dayOfWeek === 7 ? 0 : item.dayOfWeek
    const maxIterations = 365 
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

const currentCourseInfo = computed(() => {
   if (occurrencesOfSelectedCourse.value.length > 0) {
      return occurrencesOfSelectedCourse.value[0]
   }
   return null
})

function getAttendanceStatus(dateStr: string) {
   if (!selectedCourseCode.value || !occurrencesOfSelectedCourse.value.length) return null
   
   // Currently, find the classId from the date
   const occurrence = occurrencesOfSelectedCourse.value.find(o => o.date === dateStr)
   if (!occurrence) return null
   
   const recordsForClass = classAttendances.value[occurrence.classId] || []
   const record = recordsForClass.find(r => r.date === dateStr)
   
   if (record) return record.status

   // Requirement: If session has passed and no data, auto-mark as ABSENT after 00:00 next day
   const sessionDate = new Date(dateStr)
   sessionDate.setHours(0, 0, 0, 0)
   const todayDate = new Date()
   todayDate.setHours(0, 0, 0, 0)
   
   if (sessionDate < todayDate) {
      return 'ABSENT'
   }
   
   return null
}

const totalSessionsCount = computed(() => occurrencesOfSelectedCourse.value.length)

const maxAllowedAbsent = computed(() => {
   return Math.floor(totalSessionsCount.value * 0.2)
})



const totalAbsent = computed(() => {
   let count = 0
   occurrencesOfSelectedCourse.value.forEach(o => {
      const status = getAttendanceStatus(o.date)
      if (status === 'ABSENT') count++
   })
   return count
})

const absentPercentage = computed(() => {
   if (totalSessionsCount.value === 0) return null
   // Calculate percentage based on TOTAL course sessions as per requirement
   return Math.round((totalAbsent.value / totalSessionsCount.value) * 100)
})

function formatPrettyDate(dateStr: string) {
  const parts = dateStr.split('-')
  if (parts.length !== 3) return dateStr
  return `${parts[2]}/${parts[1]}/${parts[0]}`
}

function formatTime(time: string | undefined) {
  if (!time) return ''
  return time.substring(0, 5)
}
</script>
