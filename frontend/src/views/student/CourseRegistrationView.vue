<script setup lang="ts">
import { ref } from 'vue'

const searchQuery = ref('')

// const departments = ['Computer Science', 'Mathematics', 'Arts', 'Chemistry', 'Economics', 'Literature']
const selectedDepartments = ref(['Computer Science'])

const courses = ref([
  {
    code: 'CS-101',
    title: 'Introduction to Algorithms',
    professor: 'Prof. Alan Turing',
    credits: 4,
    schedule: 'Mon, Wed 10:00 AM - 11:30 AM',
    location: 'Building A, Room 304',
    seatsOpen: 24,
    status: 'Open',
    image:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuA6GibUlYwTHKKJNgW4ZaA0ABYlb0SRTwHGKEW42efG_sh6i1Y66Heabkpc0KQ2GsSTk6svSbW0HplCdlwtPdoAUFycjoI6jo5u2IIEasNAER1YxIJT94OIS0RL8_Qe9te7R15fb69_ykFCi9CaxdgrL5Zn4lEAVFddbWF07dkqd2x8b-Hgo7i28XLoqJA9-61ESNliwD-f97V-CTV6vLCZ751hCCTSjDtnZPuTTb2BadSYTEkj2HksPy92ESQR0j8RSWGNKS3c9UU',
  },
  {
    code: 'MATH-202',
    title: 'Advanced Calculus II',
    professor: 'Prof. Katherine Johnson',
    credits: 3,
    schedule: 'Tue, Thu 02:00 PM - 03:30 PM',
    location: 'Science Hall, Room 101',
    seatsOpen: 5,
    status: 'Limited',
    image:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuAyGTtF7PHQEMijpgDbVB83G8a_yuzpWnyf0UJZtHanYN63XFD_fAr940lD-uorkKWdGsRRiibEgYDJvwqy7TuWZrPQLHpPAsbJD78Yt-k2ZOgiuXWuVsovkHEp6ioaktzpufoX_PB4ODjP-bT5tAX-87yMViSn1nLgmuDNIaNL1EGeqTvVwA6Uwrliy04Jrb3QwGhX1gpO83XMEEIpdWMUvjaB8OpjVeo18vYLHse8NUF9Z_FJL9wpDqsH724hmQtpnRu2Uo-j7Yw',
  },
  {
    code: 'ARTS-105',
    title: 'Modern Art History',
    professor: 'Prof. Leonardo Da Vinci',
    credits: 3,
    schedule: 'Fri 09:00 AM - 12:00 PM',
    location: 'Arts Center, Studio B',
    seatsOpen: 12,
    status: 'Open',
    image:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuBB7_W0zTridFxqR3C3ednT0mfqfcSXX5AX7Bf2ir5_uoYiHaLOPAL1bYTTzL3DXdXQnMONU4Lgwvcpe2wQFS8EUOahOmnI1XUEvGGgViXMQmdGjTy1jrytU0HDb-CY6GKF_7hg0ObFFKj_hWK9701vP48-8ZOL5GcZA-dCSAjDv-daXUjhJOXOHM-uZEzTcYWQ8mTx-F3VuyWOYszvTogN0w3-9YM_ErcOWiDJj2SVnZKqNISnGvYWp7VJzeDwSDTPWwcVeD45_h0',
  },
  {
    code: 'CHEM-301',
    title: 'Organic Chemistry',
    professor: 'Prof. Marie Curie',
    credits: 4,
    schedule: 'Mon, Wed, Fri 11:00 AM - 12:00 PM',
    location: 'Science Hall, Lab 4',
    seatsOpen: 0,
    status: 'Waitlist',
    image:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuCeY8X2jdx4e9l19o_fRutPKjtUsbAy-87Xq4TIDlUJv0MSwCODU4T5rWz4iGvpxeBHETKw1XTngJOmrxFaYaPb9wMvi4P6rujdxqk-ztEZ9JZmav16kKeRTbZwNWfhUPJhADSNcwnc1UP4JdtFs9mc_ZPXCS-dr0pnkBvC_VxzkmFrZpjwSchh6Zw4_glgYseIvdHjK5v9oRZjQe74XKb81A8hdosf9F5skVTetxFTPLqwTPUciSNmJHpJIP1HQBW2aJBAO-vI8og',
  },
  {
    code: 'ECON-101',
    title: 'Macroeconomics',
    professor: 'Prof. Adam Smith',
    credits: 3,
    schedule: 'Tue, Thu 09:30 AM - 11:00 AM',
    location: 'Business School, Hall C',
    seatsOpen: 50,
    status: 'Open',
    image:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuAFUzW-PWX_tvaZE86XJUB2kvKpCprXbZmXs5qWn8QaVsBe4V2fPffz5iCsc-wcfSyQVWQWpBuLF1NizF1-hUhTHTdPAwBDhlf5XhYqYZtIZDMtYlXNoWEPPVRa_Peetta984hC0NURLw6ORavvfVTQNGRQGCsqvqCthDvAMIh5OofcvWildhTsIAP3AqZ6VjvBt-yBvkOxorHGvhZgkNGG2i5Ep-GiCTfwkkyx71bQE7MLQylnr8CJC6v87EwWD9PbOPRoFR1BZ5o',
  },
  {
    code: 'LIT-204',
    title: 'Creative Writing',
    professor: 'Prof. William Shakespeare',
    credits: 3,
    schedule: 'Wed 06:00 PM - 09:00 PM',
    location: 'Humanities Bldg, Room 202',
    seatsOpen: 8,
    status: 'Open',
    image:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuAgzOeiKLKYeid_D_6l81dpyGmSMf3tWNiJaQC_2_5oBx81u8i55EIxel17NRxBZG6JWzIKhKXadpvzDYENKgQroRhW3ISI1t7V_W3BWCujx9j7FVzWLPQ6Q6GUIr_eGxG969w1d-HBs8xBRnbGh5WsyiSwza72YsSoxFsVDfnL_k0DeBMOBSWFxUkF1araE8WPrKoE6WeLu5rVjgj7AvAuKAYrenl3SWdDMCnNaUAFHRx37mCQ0lxAwYdRsSF73YU9rdowM14VtWA',
  },
])

function removeFilter(filter: string) {
  selectedDepartments.value = selectedDepartments.value.filter((d) => d !== filter)
}

function clearAllFilters() {
  selectedDepartments.value = []
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
            Browse the catalog for Fall 2024. Select courses to add them to your tentative schedule.
          </p>
        </div>
        <div class="flex gap-3">
          <button
            class="flex items-center gap-2 h-10 px-4 rounded-lg border border-border-light dark:border-border-dark bg-surface-light dark:bg-surface-dark text-text-main-light dark:text-text-main-dark font-medium hover:bg-input-bg-light dark:hover:bg-input-bg-dark transition-colors"
          >
            <span class="material-symbols-outlined !text-[20px]">calendar_month</span>
            <span>My Schedule</span>
          </button>
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
          <button
            class="group flex h-10 items-center justify-between gap-x-3 rounded-lg bg-input-bg-light dark:bg-input-bg-dark px-4 hover:bg-primary/10 dark:hover:bg-primary/20 transition-colors"
          >
            <span class="text-text-main-light dark:text-text-main-dark text-sm font-medium"
              >Department</span
            >
            <span
              class="material-symbols-outlined text-text-muted-light dark:text-text-muted-dark !text-[20px] group-hover:text-primary transition-colors"
              >expand_more</span
            >
          </button>
          <button
            class="group flex h-10 items-center justify-between gap-x-3 rounded-lg bg-input-bg-light dark:bg-input-bg-dark px-4 hover:bg-primary/10 dark:hover:bg-primary/20 transition-colors"
          >
            <span class="text-text-main-light dark:text-text-main-dark text-sm font-medium"
              >Credits</span
            >
            <span
              class="material-symbols-outlined text-text-muted-light dark:text-text-muted-dark !text-[20px] group-hover:text-primary transition-colors"
              >expand_more</span
            >
          </button>
          <button
            class="group flex h-10 items-center justify-between gap-x-3 rounded-lg bg-input-bg-light dark:bg-input-bg-dark px-4 hover:bg-primary/10 dark:hover:bg-primary/20 transition-colors"
          >
            <span class="text-text-main-light dark:text-text-main-dark text-sm font-medium"
              >Schedule</span
            >
            <span
              class="material-symbols-outlined text-text-muted-light dark:text-text-muted-dark !text-[20px] group-hover:text-primary transition-colors"
              >expand_more</span
            >
          </button>
          <div class="h-8 w-px bg-border-light dark:bg-border-dark mx-1 hidden md:block"></div>
          <button
            class="flex items-center justify-center size-10 rounded-lg bg-input-bg-light dark:bg-input-bg-dark text-text-main-light dark:text-text-main-dark hover:text-primary hover:bg-primary/10 transition-colors"
          >
            <span class="material-symbols-outlined">filter_list</span>
          </button>
        </div>
      </div>

      <!-- Active Filters (Chips) -->
      <div v-if="selectedDepartments.length > 0" class="flex gap-2 flex-wrap">
        <span
          v-for="dept in selectedDepartments"
          :key="dept"
          class="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-primary/10 text-primary text-sm font-medium border border-primary/20"
        >
          {{ dept }}
          <button @click="removeFilter(dept)" class="hover:text-red-500 ml-1">
            <span class="material-symbols-outlined !text-[16px] align-middle">close</span>
          </button>
        </span>
        <span
          class="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-primary/10 text-primary text-sm font-medium border border-primary/20"
        >
          Fall 2024
          <button class="hover:text-red-500 ml-1">
            <span class="material-symbols-outlined !text-[16px] align-middle">close</span>
          </button>
        </span>
        <button
          @click="clearAllFilters"
          class="text-sm text-text-muted-light dark:text-text-muted-dark underline hover:text-primary ml-2"
        >
          Clear all
        </button>
      </div>

      <!-- Course Grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <!-- Cards -->
        <div
          v-for="course in courses"
          :key="course.code"
          class="group flex flex-col rounded-xl bg-surface-light dark:bg-surface-dark overflow-hidden shadow-sm hover:shadow-md border border-border-light dark:border-border-dark transition-all duration-300"
        >
          <div
            class="relative h-48 w-full bg-cover bg-center"
            :style="{ backgroundImage: `url(${course.image})` }"
          >
            <div
              class="absolute top-3 right-3 bg-white/90 dark:bg-black/80 backdrop-blur text-xs font-bold px-2 py-1 rounded text-text-main-light dark:text-text-main-dark"
            >
              {{ course.credits }} Credits
            </div>
          </div>
          <div class="p-5 flex flex-col gap-4 flex-1">
            <div class="flex flex-col gap-1">
              <span class="text-primary text-xs font-bold uppercase tracking-wider">{{
                course.code
              }}</span>
              <h3
                class="text-text-main-light dark:text-text-main-dark text-lg font-bold leading-tight group-hover:text-primary transition-colors"
              >
                {{ course.title }}
              </h3>
              <p class="text-text-muted-light dark:text-text-muted-dark text-sm">
                {{ course.professor }}
              </p>
            </div>
            <div class="flex flex-col gap-2 mt-auto">
              <div
                class="flex items-center gap-2 text-text-muted-light dark:text-text-muted-dark text-xs"
              >
                <span class="material-symbols-outlined !text-[16px]">schedule</span>
                <span>{{ course.schedule }}</span>
              </div>
              <div
                class="flex items-center gap-2 text-text-muted-light dark:text-text-muted-dark text-xs"
              >
                <span class="material-symbols-outlined !text-[16px]">location_on</span>
                <span>{{ course.location }}</span>
              </div>
            </div>
            <div
              class="pt-2 border-t border-border-light dark:border-border-dark flex justify-between items-center"
            >
              <span
                v-if="course.status === 'Open'"
                class="text-green-600 dark:text-green-400 text-xs font-medium bg-green-100 dark:bg-green-900/30 px-2 py-1 rounded"
              >
                {{ course.seatsOpen }} Seats Open
              </span>
              <span
                v-else-if="course.status === 'Limited'"
                class="text-orange-600 dark:text-orange-400 text-xs font-medium bg-orange-100 dark:bg-orange-900/30 px-2 py-1 rounded"
              >
                {{ course.seatsOpen }} Seats Left
              </span>
              <span
                v-else
                class="text-red-600 dark:text-red-400 text-xs font-medium bg-red-100 dark:bg-red-900/30 px-2 py-1 rounded"
              >
                Waitlist Only
              </span>

              <button
                v-if="course.status !== 'Waitlist'"
                class="bg-primary hover:bg-primary/90 text-text-main-light font-bold text-sm py-2 px-4 rounded-lg transition-colors shadow-sm shadow-primary/20"
              >
                Register
              </button>
              <button
                v-else
                class="bg-input-bg-light dark:bg-input-bg-dark text-text-muted-light dark:text-text-muted-dark font-bold text-sm py-2 px-4 rounded-lg transition-colors cursor-not-allowed"
              >
                Join Waitlist
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div class="flex justify-center py-6">
        <nav class="flex items-center gap-2">
          <button
            class="p-2 rounded-lg text-text-muted-light dark:text-text-muted-dark hover:bg-input-bg-light dark:hover:bg-input-bg-dark disabled:opacity-50"
          >
            <span class="material-symbols-outlined">chevron_left</span>
          </button>
          <button
            class="size-9 rounded-lg bg-primary text-text-main-light font-bold flex items-center justify-center"
          >
            1
          </button>
          <button
            class="size-9 rounded-lg text-text-main-light dark:text-text-main-dark hover:bg-input-bg-light dark:hover:bg-input-bg-dark font-medium flex items-center justify-center"
          >
            2
          </button>
          <button
            class="size-9 rounded-lg text-text-main-light dark:text-text-main-dark hover:bg-input-bg-light dark:hover:bg-input-bg-dark font-medium flex items-center justify-center"
          >
            3
          </button>
          <button
            class="p-2 rounded-lg text-text-main-light dark:text-text-main-dark hover:bg-input-bg-light dark:hover:bg-input-bg-dark"
          >
            <span class="material-symbols-outlined">chevron_right</span>
          </button>
        </nav>
      </div>
    </div>
  </div>
</template>
