<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  getAvailableClasses,
  getEnrolledClasses,
  enrollInClass,
  dropClass,
  type StudentAvailableClass,
  type StudentEnrolledClass,
} from '@/services/studentClassService'
import { useToast } from '@/composables/useToast'

const searchQuery = ref('')
const loading = ref(true)
const registeringId = ref<number | null>(null)
const droppingId = ref<number | null>(null)
const availableClasses = ref<StudentAvailableClass[]>([])
const enrolledClasses = ref<StudentEnrolledClass[]>([])
const selectedCredits = ref<number | null>(null)
const selectedDay = ref<string | null>(null)
const activeTab = ref<'available' | 'enrolled'>('available')
const showDropConfirm = ref(false)
const classToDropId = ref<number | null>(null)
const classToDropName = ref('')

const { toast, showToast } = useToast()

// Default placeholder for courses
const DEFAULT_COURSE_IMAGE =
  'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?q=80&w=2073&auto=format&fit=crop'

onMounted(async () => {
  await fetchAllData()
})

async function fetchAllData() {
  loading.value = true
  try {
    const [available, enrolled] = await Promise.all([getAvailableClasses(), getEnrolledClasses()])
    availableClasses.value = available
    enrolledClasses.value = enrolled
  } catch (error) {
    console.error(error)
    showToast('Failed to load courses', 'error')
  } finally {
    loading.value = false
  }
}

async function handleEnroll(classId: number) {
  registeringId.value = classId
  try {
    await enrollInClass(classId)
    showToast('Enrolled successfully!', 'success')
    await fetchAllData()
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to enroll'
    showToast(message, 'error')
  } finally {
    registeringId.value = null
  }
}

function confirmDrop(classId: number, courseName: string) {
  classToDropId.value = classId
  classToDropName.value = courseName
  showDropConfirm.value = true
}

function cancelDrop() {
  showDropConfirm.value = false
  classToDropId.value = null
  classToDropName.value = ''
}

async function handleDrop() {
  if (!classToDropId.value) return
  const classId = classToDropId.value
  showDropConfirm.value = false
  droppingId.value = classId
  try {
    await dropClass(classId)
    showToast('Dropped class successfully!', 'success')
    await fetchAllData()
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to drop class'
    showToast(message, 'error')
  } finally {
    droppingId.value = null
    classToDropId.value = null
    classToDropName.value = ''
  }
}

const daysMap: Record<string, number> = {
  'Mon': 1, 'Tue': 2, 'Wed': 3, 'Thu': 4, 'Fri': 5, 'Sat': 6, 'Sun': 7
}

const filteredClasses = computed(() => {
  return availableClasses.value.filter((c) => {
    const matchesSearch =
      !searchQuery.value ||
      c.courseName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      c.courseCode.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      c.teacherName.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchesCredits = selectedCredits.value === null || c.credits === selectedCredits.value
    const matchesDay = !selectedDay.value || c.sessions?.some(s => s.dayOfWeek === daysMap[selectedDay.value!])

    return matchesSearch && matchesCredits && matchesDay
  })
})

const filteredEnrolled = computed(() => {
  if (!searchQuery.value) return enrolledClasses.value
  return enrolledClasses.value.filter(
    (c) =>
      c.courseName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      c.courseCode.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      c.teacherName.toLowerCase().includes(searchQuery.value.toLowerCase()),
  )
})

const uniqueCredits = computed(() => {
  const credits = availableClasses.value.map((c) => c.credits)
  return [...new Set(credits)].sort((a, b) => a - b)
})

const totalEnrolledCredits = computed(() => {
  return enrolledClasses.value.reduce((sum, c) => sum + c.credits, 0)
})

const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

function clearFilters() {
  searchQuery.value = ''
  selectedCredits.value = null
  selectedDay.value = null
}

function formatSchedule(cls: StudentAvailableClass | StudentEnrolledClass): string {
  if (!cls.sessions || cls.sessions.length === 0) return 'N/A'
  return cls.sessions.map(s => `T${s.dayOfWeek} ${s.startTime.slice(0, 5)}-${s.endTime.slice(0, 5)}`).join(', ')
}

function formatRoom(cls: StudentAvailableClass | StudentEnrolledClass): string {
  if (!cls.sessions || cls.sessions.length === 0) return 'TBD'
  return Array.from(new Set(cls.sessions.map(s => s.roomName))).join(', ')
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
            Course Registration
          </h1>
          <p class="text-text-muted-light dark:text-text-muted-dark text-base max-w-2xl">
            Browse the catalog and register for classes in your department for the current semester.
          </p>
        </div>
        <div class="flex gap-3">
          <router-link
            :to="{ name: 'student-schedule' }"
            class="flex items-center gap-2 h-10 px-4 rounded-lg border border-border-light dark:border-border-dark bg-surface-light dark:bg-surface-dark text-text-main-light dark:text-text-main-dark font-medium hover:bg-input-bg-light dark:hover:bg-input-bg-dark transition-colors"
          >
            <span class="material-symbols-outlined !text-[20px]">calendar_month</span>
            <span>My Schedule</span>
          </router-link>
        </div>
      </div>

      <!-- Tabs -->
      <div class="flex gap-1 p-1 rounded-xl bg-input-bg-light dark:bg-input-bg-dark w-fit">
        <button
          @click="activeTab = 'available'"
          :class="[
            'flex items-center gap-2 px-5 py-2.5 rounded-lg text-sm font-semibold transition-all duration-200',
            activeTab === 'available'
              ? 'bg-surface-light dark:bg-surface-dark text-primary shadow-sm'
              : 'text-text-muted-light dark:text-text-muted-dark hover:text-text-main-light dark:hover:text-text-main-dark',
          ]"
        >
          <span class="material-symbols-outlined !text-[18px]">library_books</span>
          Available Classes
          <span
            v-if="availableClasses.length > 0"
            class="text-xs bg-primary/10 text-primary px-2 py-0.5 rounded-full"
          >
            {{ availableClasses.length }}
          </span>
        </button>
        <button
          @click="activeTab = 'enrolled'"
          :class="[
            'flex items-center gap-2 px-5 py-2.5 rounded-lg text-sm font-semibold transition-all duration-200',
            activeTab === 'enrolled'
              ? 'bg-surface-light dark:bg-surface-dark text-primary shadow-sm'
              : 'text-text-muted-light dark:text-text-muted-dark hover:text-text-main-light dark:hover:text-text-main-dark',
          ]"
        >
          <span class="material-symbols-outlined !text-[18px]">school</span>
          My Enrolled
          <span
            v-if="enrolledClasses.length > 0"
            class="text-xs bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-300 px-2 py-0.5 rounded-full"
          >
            {{ enrolledClasses.length }}
          </span>
        </button>
      </div>

      <!-- Search & Filters Bar -->
      <div
        class="flex flex-col lg:flex-row gap-4 p-4 rounded-xl bg-surface-light dark:bg-surface-dark shadow-sm border border-border-light dark:border-border-dark"
      >
        <!-- Search Input -->
        <div class="flex-1 min-w-[300px]">
          <div
            class="flex w-full items-center rounded-lg bg-input-bg-light dark:bg-input-bg-dark h-12 px-4 focus-within:ring-2 ring-primary/50 transition-all border border-transparent"
          >
            <span class="material-symbols-outlined text-text-muted-light dark:text-text-muted-dark"
              >search</span
            >
            <input
              v-model="searchQuery"
              class="w-full bg-transparent border-none text-base text-text-main-light dark:text-text-main-dark placeholder:text-text-muted-light dark:placeholder:text-text-muted-dark focus:ring-0 ml-2"
              placeholder="Search by course name, code, or professor..."
            />
          </div>
        </div>
        <!-- Chips/Dropdowns (only for available tab) -->
        <div v-if="activeTab === 'available'" class="flex flex-wrap items-center gap-3">
          <!-- Credits Filter -->
          <div class="relative">
            <select
              v-model="selectedCredits"
              class="appearance-none h-10 pl-4 pr-10 rounded-lg bg-input-bg-light dark:bg-input-bg-dark text-text-main-light dark:text-text-main-dark text-sm font-medium border-none focus:ring-2 ring-primary/50 hover:bg-primary/10 dark:hover:bg-primary/20 transition-all cursor-pointer w-full sm:w-auto !bg-none"
            >
              <option :value="null">All Credits</option>
              <option v-for="c in uniqueCredits" :key="c" :value="c">{{ c }} Credits</option>
            </select>
            <span
              class="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark !text-[18px] pointer-events-none select-none"
              >keyboard_arrow_down</span
            >
          </div>

          <!-- Day Filter -->
          <div class="relative">
            <select
              v-model="selectedDay"
              class="appearance-none h-10 pl-4 pr-10 rounded-lg bg-input-bg-light dark:bg-input-bg-dark text-text-main-light dark:text-text-main-dark text-sm font-medium border-none focus:ring-2 ring-primary/50 hover:bg-primary/10 dark:hover:bg-primary/20 transition-all cursor-pointer w-full sm:w-auto !bg-none"
            >
              <option :value="null">All Days</option>
              <option v-for="day in days" :key="day" :value="day">{{ day }}</option>
            </select>
            <span
              class="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark !text-[18px] pointer-events-none select-none"
              >keyboard_arrow_down</span
            >
          </div>

          <div class="h-8 w-px bg-border-light dark:bg-border-dark mx-1 hidden md:block"></div>

          <button
            @click="clearFilters"
            class="flex items-center justify-center size-10 rounded-lg bg-input-bg-light dark:bg-input-bg-dark text-text-main-light dark:text-text-main-dark hover:text-primary hover:bg-primary/10 transition-colors shadow-sm"
            title="Clear all filters"
          >
            <span class="material-symbols-outlined !text-[20px]">filter_alt_off</span>
          </button>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-20 gap-4">
        <div
          class="size-12 border-4 border-primary/20 border-t-primary rounded-full animate-spin"
        ></div>
        <p class="text-text-muted-light dark:text-text-muted-dark font-medium">
          Loading courses...
        </p>
      </div>

      <!-- ==================== AVAILABLE CLASSES TAB ==================== -->
      <template v-else-if="activeTab === 'available'">
        <!-- Empty State -->
        <div
          v-if="filteredClasses.length === 0"
          class="flex flex-col items-center justify-center py-20 gap-4 bg-surface-light dark:bg-surface-dark rounded-xl border border-dashed border-border-light dark:border-border-dark"
        >
          <span class="material-symbols-outlined text-6xl text-text-muted-light dark:text-stone-700"
            >library_books</span
          >
          <div class="text-center">
            <h3 class="text-xl font-bold">No courses found</h3>
            <p class="text-text-muted-light dark:text-text-muted-dark mt-1">
              Try adjusting your search or filters to find what you're looking for.
            </p>
          </div>
          <button @click="clearFilters" class="text-primary font-bold hover:underline">
            Clear all filters
          </button>
        </div>

        <!-- Course Grid -->
        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div
            v-for="cls in filteredClasses"
            :key="cls.classId"
            class="group flex flex-col rounded-xl bg-surface-light dark:bg-surface-dark overflow-hidden shadow-sm hover:shadow-md border border-border-light dark:border-border-dark transition-all duration-300"
          >
            <div
              class="relative h-48 w-full bg-cover bg-center"
              :style="{ backgroundImage: `url(${DEFAULT_COURSE_IMAGE})` }"
            >
              <div
                class="absolute top-3 right-3 bg-white/90 dark:bg-black/80 backdrop-blur text-xs font-bold px-2 py-1 rounded text-text-main-light dark:text-text-main-dark"
              >
                {{ cls.credits }} Credits
              </div>
            </div>
            <div class="p-5 flex flex-col gap-4 flex-1">
              <div class="flex flex-col gap-1">
                <span class="text-primary text-xs font-bold uppercase tracking-wider">{{
                  cls.courseCode
                }}</span>
                <h3
                  class="text-text-main-light dark:text-text-main-dark text-lg font-bold leading-tight group-hover:text-primary transition-colors"
                >
                  {{ cls.courseName }}
                </h3>
                <p class="text-text-muted-light dark:text-text-muted-dark text-sm">
                  {{ cls.teacherName }}
                </p>
              </div>
              <div class="flex flex-col gap-2 mt-auto">
                <div
                  class="flex items-center gap-2 text-text-muted-light dark:text-text-muted-dark text-xs"
                >
                  <span class="material-symbols-outlined !text-[16px]">schedule</span>
                  <span>{{ formatSchedule(cls) }}</span>
                </div>
                <div
                  class="flex items-center gap-2 text-text-muted-light dark:text-text-muted-dark text-xs"
                >
                  <span class="material-symbols-outlined !text-[16px]">location_on</span>
                  <span>{{ formatRoom(cls) }}</span>
                </div>
              </div>
              <div
                class="pt-2 border-t border-border-light dark:border-border-dark flex justify-between items-center"
              >
                <span
                  v-if="cls.currentStudents < cls.maxStudents"
                  class="text-green-600 dark:text-green-400 text-xs font-medium bg-green-100 dark:bg-green-900/30 px-2 py-1 rounded"
                >
                  {{ cls.maxStudents - cls.currentStudents }} Seats Open
                </span>
                <span
                  v-else
                  class="text-red-600 dark:text-red-400 text-xs font-medium bg-red-100 dark:bg-red-900/30 px-2 py-1 rounded"
                >
                  Class Full
                </span>

                <button
                  @click="handleEnroll(cls.classId)"
                  :disabled="
                    cls.currentStudents >= cls.maxStudents || registeringId === cls.classId
                  "
                  class="bg-primary hover:bg-primary/90 text-text-main-light font-bold text-sm py-2 px-4 rounded-lg transition-all shadow-sm shadow-primary/20 disabled:bg-stone-300 dark:disabled:bg-stone-800 disabled:text-text-muted-light dark:disabled:text-text-muted-dark disabled:cursor-not-allowed flex items-center gap-2"
                >
                  <div
                    v-if="registeringId === cls.classId"
                    class="size-4 border-2 border-text-main-light/20 border-t-text-main-light rounded-full animate-spin"
                  ></div>
                  <span>{{
                    cls.currentStudents >= cls.maxStudents ? 'Waitlist' : 'Register'
                  }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- ==================== ENROLLED CLASSES TAB ==================== -->
      <template v-else>
        <!-- Summary Bar -->
        <div
          v-if="enrolledClasses.length > 0"
          class="flex items-center gap-6 p-4 rounded-xl bg-primary/5 dark:bg-primary/10 border border-primary/20"
        >
          <div class="flex items-center gap-2">
            <span class="material-symbols-outlined text-primary !text-[20px]">school</span>
            <span class="text-sm font-semibold text-text-main-light dark:text-text-main-dark"
              >{{ enrolledClasses.length }} Class{{
                enrolledClasses.length !== 1 ? 'es' : ''
              }}</span
            >
          </div>
          <div class="h-5 w-px bg-border-light dark:bg-border-dark"></div>
          <div class="flex items-center gap-2">
            <span class="material-symbols-outlined text-primary !text-[20px]">token</span>
            <span class="text-sm font-semibold text-text-main-light dark:text-text-main-dark"
              >{{ totalEnrolledCredits }} Credits</span
            >
          </div>
        </div>

        <!-- Empty State -->
        <div
          v-if="filteredEnrolled.length === 0"
          class="flex flex-col items-center justify-center py-20 gap-4 bg-surface-light dark:bg-surface-dark rounded-xl border border-dashed border-border-light dark:border-border-dark"
        >
          <span class="material-symbols-outlined text-6xl text-text-muted-light dark:text-stone-700"
            >how_to_reg</span
          >
          <div class="text-center">
            <h3 class="text-xl font-bold text-text-main-light dark:text-text-main-dark">
              No enrolled classes
            </h3>
            <p class="text-text-muted-light dark:text-text-muted-dark mt-1">
              Head to the "Available Classes" tab to register for courses.
            </p>
          </div>
          <button
            @click="activeTab = 'available'"
            class="text-primary font-bold hover:underline flex items-center gap-1"
          >
            <span class="material-symbols-outlined !text-[18px]">arrow_back</span>
            Browse available classes
          </button>
        </div>

        <!-- Enrolled Classes Table -->
        <div
          v-else
          class="rounded-xl bg-surface-light dark:bg-surface-dark border border-border-light dark:border-border-dark overflow-hidden shadow-sm"
        >
          <div class="overflow-x-auto">
            <table class="w-full">
              <thead>
                <tr
                  class="bg-input-bg-light dark:bg-input-bg-dark border-b border-border-light dark:border-border-dark"
                >
                  <th
                    class="text-left text-xs font-semibold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider px-6 py-3.5"
                  >
                    Course
                  </th>
                  <th
                    class="text-left text-xs font-semibold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider px-6 py-3.5 hidden md:table-cell"
                  >
                    Professor
                  </th>
                  <th
                    class="text-left text-xs font-semibold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider px-6 py-3.5 hidden sm:table-cell"
                  >
                    Schedule
                  </th>
                  <th
                    class="text-center text-xs font-semibold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider px-6 py-3.5"
                  >
                    Credits
                  </th>
                  <th
                    class="text-right text-xs font-semibold text-text-muted-light dark:text-text-muted-dark uppercase tracking-wider px-6 py-3.5"
                  >
                    Action
                  </th>
                </tr>
              </thead>
              <tbody class="divide-y divide-border-light dark:divide-border-dark">
                <tr
                  v-for="cls in filteredEnrolled"
                  :key="cls.enrollmentId"
                  class="hover:bg-input-bg-light/50 dark:hover:bg-input-bg-dark/50 transition-colors"
                >
                  <td class="px-6 py-4">
                    <div class="flex flex-col gap-0.5">
                      <span class="text-primary text-xs font-bold uppercase tracking-wider">{{
                        cls.courseCode
                      }}</span>
                      <span
                        class="text-text-main-light dark:text-text-main-dark text-sm font-semibold"
                        >{{ cls.courseName }}</span
                      >
                      <span
                        class="text-text-muted-light dark:text-text-muted-dark text-xs md:hidden"
                        >{{ cls.teacherName }}</span
                      >
                      <span
                        class="text-text-muted-light dark:text-text-muted-dark text-xs sm:hidden"
                        >{{ formatSchedule(cls) }} · {{ formatRoom(cls) }}</span
                      >
                    </div>
                  </td>
                  <td
                    class="px-6 py-4 text-text-main-light dark:text-text-main-dark text-sm hidden md:table-cell"
                  >
                    {{ cls.teacherName }}
                  </td>
                  <td class="px-6 py-4 hidden sm:table-cell">
                    <div class="flex flex-col gap-0.5">
                      <span class="text-text-main-light dark:text-text-main-dark text-sm">{{
                        formatSchedule(cls)
                      }}</span>
                      <span class="text-text-muted-light dark:text-text-muted-dark text-xs">{{
                        formatRoom(cls)
                      }}</span>
                    </div>
                  </td>
                  <td
                    class="px-6 py-4 text-center text-text-main-light dark:text-text-main-dark text-sm font-medium"
                  >
                    {{ cls.credits }}
                  </td>
                  <td class="px-6 py-4 text-right">
                    <button
                      @click="confirmDrop(cls.classId, cls.courseName)"
                      :disabled="droppingId === cls.classId"
                      class="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-xs font-semibold border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 hover:bg-red-100 dark:hover:bg-red-900/40 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <div
                        v-if="droppingId === cls.classId"
                        class="size-3.5 border-2 border-red-300 border-t-red-600 rounded-full animate-spin"
                      ></div>
                      <span class="material-symbols-outlined !text-[16px]" v-else>close</span>
                      Drop
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>
    </div>

    <!-- Drop Confirmation Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="showDropConfirm"
          class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4"
          @click.self="cancelDrop"
        >
          <div
            class="bg-surface-light dark:bg-surface-dark rounded-2xl shadow-2xl max-w-md w-full p-6 flex flex-col gap-5"
          >
            <div class="flex items-start gap-4">
              <div
                class="shrink-0 size-12 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center"
              >
                <span class="material-symbols-outlined text-red-600 dark:text-red-400 !text-[24px]"
                  >warning</span
                >
              </div>
              <div class="flex flex-col gap-1">
                <h3
                  class="text-lg font-bold text-text-main-light dark:text-text-main-dark leading-tight"
                >
                  Drop Class?
                </h3>
                <p class="text-text-muted-light dark:text-text-muted-dark text-sm">
                  Are you sure you want to drop
                  <strong class="text-text-main-light dark:text-text-main-dark">{{
                    classToDropName
                  }}</strong
                  >? You can re-enroll later if seats are still available.
                </p>
              </div>
            </div>
            <div class="flex justify-end gap-3">
              <button
                @click="cancelDrop"
                class="px-4 py-2.5 rounded-lg text-sm font-semibold border border-border-light dark:border-border-dark text-text-main-light dark:text-text-main-dark hover:bg-input-bg-light dark:hover:bg-input-bg-dark transition-colors"
              >
                Cancel
              </button>
              <button
                @click="handleDrop"
                class="px-4 py-2.5 rounded-lg text-sm font-semibold bg-red-600 hover:bg-red-700 text-white transition-colors shadow-sm"
              >
                Drop Class
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Toast Notification -->
    <Teleport to="body">
      <Transition name="toast">
        <div
          v-if="toast"
          :class="[
            'fixed top-6 right-6 z-[100] flex items-center gap-3 px-5 py-3.5 rounded-xl shadow-2xl text-sm font-medium border backdrop-blur-sm',
            toast.type === 'success'
              ? 'bg-green-50 dark:bg-green-900/40 border-green-200 dark:border-green-700 text-green-800 dark:text-green-300'
              : 'bg-red-50 dark:bg-red-900/40 border-red-200 dark:border-red-700 text-red-800 dark:text-red-300',
          ]"
        >
          <span class="material-symbols-outlined text-[20px]">
            {{ toast.type === 'success' ? 'check_circle' : 'error' }}
          </span>
          {{ toast.message }}
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.toast-enter-active {
  transition: all 0.3s ease;
}
.toast-leave-active {
  transition: all 0.25s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateY(-12px) scale(0.95);
}
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.97);
}

.modal-enter-active {
  transition: all 0.2s ease;
}
.modal-leave-active {
  transition: all 0.15s ease;
}
.modal-enter-from {
  opacity: 0;
}
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from > div {
  transform: scale(0.95);
}
.modal-leave-to > div {
  transform: scale(0.97);
}
</style>
