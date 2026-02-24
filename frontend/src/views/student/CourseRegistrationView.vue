<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getAvailableClasses, type StudentAvailableClass } from '@/services/studentClassService'

const searchQuery = ref('')
const loading = ref(true)
const availableClasses = ref<StudentAvailableClass[]>([])
const selectedCredits = ref<number | null>(null)
const selectedDay = ref<string | null>(null)

// Default placeholder for courses
const DEFAULT_COURSE_IMAGE =
  'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?q=80&w=2073&auto=format&fit=crop'

onMounted(async () => {
  try {
    availableClasses.value = await getAvailableClasses()
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
})

const filteredClasses = computed(() => {
  return availableClasses.value.filter((c) => {
    // Search filter
    const matchesSearch =
      !searchQuery.value ||
      c.courseName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      c.courseCode.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      c.teacherName.toLowerCase().includes(searchQuery.value.toLowerCase())

    // Credits filter
    const matchesCredits = selectedCredits.value === null || c.credits === selectedCredits.value

    // Day filter
    const matchesDay = !selectedDay.value || c.schedule.includes(selectedDay.value)

    return matchesSearch && matchesCredits && matchesDay
  })
})

const uniqueCredits = computed(() => {
  const credits = availableClasses.value.map((c) => c.credits)
  return [...new Set(credits)].sort((a, b) => a - b)
})

const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

function clearFilters() {
  searchQuery.value = ''
  selectedCredits.value = null
  selectedDay.value = null
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
        <!-- Chips/Dropdowns -->
        <div class="flex flex-wrap items-center gap-3">
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
          Loading available courses...
        </p>
      </div>

      <!-- Empty State -->
      <div
        v-else-if="filteredClasses.length === 0"
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
        <!-- Cards -->
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
                <span>{{ cls.schedule }}</span>
              </div>
              <div
                class="flex items-center gap-2 text-text-muted-light dark:text-text-muted-dark text-xs"
              >
                <span class="material-symbols-outlined !text-[16px]">location_on</span>
                <span>{{ cls.roomNumber || 'TBD' }}</span>
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
                v-if="cls.currentStudents < cls.maxStudents"
                class="bg-primary hover:bg-primary/90 text-text-main-light font-bold text-sm py-2 px-4 rounded-lg transition-colors shadow-sm shadow-primary/20"
              >
                Register
              </button>
              <button
                v-else
                class="bg-input-bg-light dark:bg-input-bg-dark text-text-muted-light dark:text-text-muted-dark font-bold text-sm py-2 px-4 rounded-lg transition-colors cursor-not-allowed"
                disabled
              >
                Waitlist
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
