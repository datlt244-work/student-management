<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { getAdminCourse, type AdminCourseDetail } from '@/services/adminUserService'

const route = useRoute()
const router = useRouter()
const courseId = route.params.courseId as string

const course = ref<AdminCourseDetail | null>(null)
const isLoading = ref(false)
const error = ref<string | null>(null)

function formatDate(dateString?: string) {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

async function fetchCourseDetail() {
  try {
    isLoading.value = true
    error.value = null
    course.value = await getAdminCourse(courseId)
  } catch (e: unknown) {
    if (e && typeof e === 'object' && 'message' in e) {
      error.value = String((e as { message?: unknown }).message)
    } else {
      error.value = 'Failed to load course details'
    }
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchCourseDetail()
})
</script>

<template>
  <div class="flex h-full w-full bg-background-light dark:bg-background-dark">
    <main class="flex-1 flex flex-col h-full overflow-y-auto scroll-smooth">
      <div v-if="isLoading" class="flex items-center justify-center h-full">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
      </div>

      <div v-else-if="error" class="flex flex-col items-center justify-center h-full gap-4">
        <div class="text-red-500 text-xl font-bold">Error</div>
        <p class="text-slate-600 dark:text-slate-400">{{ error }}</p>
        <button
          @click="router.back()"
          class="px-4 py-2 bg-stone-100 dark:bg-stone-800 rounded-lg hover:bg-stone-200 dark:hover:bg-stone-700 transition-colors"
        >
          Go Back
        </button>
      </div>

      <div v-else-if="course" class="max-w-[1100px] w-full mx-auto p-8 md:p-12 flex flex-col gap-8">
        <div class="flex items-center justify-between">
          <nav class="flex items-center gap-2 text-sm font-medium">
            <RouterLink :to="{ name: 'admin-courses' }" class="text-slate-500 hover:text-primary transition-colors">
              Course Management
            </RouterLink>
            <span class="material-symbols-outlined text-slate-400 text-[18px]">chevron_right</span>
            <span class="text-slate-900 dark:text-white">Course Details</span>
          </nav>
          <div class="flex gap-4">
            <button
              class="flex items-center gap-2 rounded-xl h-11 px-6 bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-700 text-slate-600 dark:text-slate-300 font-bold text-sm shadow-sm hover:shadow-md transition-all"
            >
              <span class="material-symbols-outlined text-[20px]">share</span>
              <span>Share</span>
            </button>
            <button
              class="flex items-center gap-2 rounded-xl h-11 px-6 bg-primary hover:bg-primary-dark text-white font-bold text-sm shadow-lg shadow-orange-500/25 transition-all active:scale-95"
            >
              <span class="material-symbols-outlined text-[20px]">edit_note</span>
              <span>Edit Course</span>
            </button>
          </div>
        </div>

        <div
          class="relative overflow-hidden rounded-[2rem] soft-gradient p-10 md:p-14 text-white shadow-2xl shadow-orange-500/20"
        >
          <div class="relative z-10">
            <div class="flex items-center gap-3 mb-4">
              <span
                class="px-3 py-1 bg-white/20 backdrop-blur-md rounded-lg text-xs font-bold uppercase tracking-widest border border-white/20"
              >
                Undergraduate
              </span>
              <span
                class="px-3 py-1 bg-white/20 backdrop-blur-md rounded-lg text-xs font-bold uppercase tracking-widest border border-white/20"
              >
                {{ course.currentSemester || 'Semester N/A' }}
              </span>
            </div>
            <h2 class="text-4xl md:text-5xl font-bold leading-tight tracking-tight max-w-2xl">
              {{ course.name }}
            </h2>
            <div class="flex items-center gap-2 mt-4 text-white/90">
              <span class="material-symbols-outlined text-[20px]">fingerprint</span>
              <span class="text-lg font-medium opacity-90">Course Code: {{ course.code }}</span>
            </div>
          </div>
          <div class="absolute -top-24 -right-24 w-64 h-64 bg-white/10 rounded-full blur-3xl"></div>
          <div
            class="absolute -bottom-12 right-12 w-32 h-32 bg-primary-dark/20 rounded-full blur-2xl"
          ></div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div
            class="bg-white dark:bg-surface-dark p-6 rounded-2xl border border-stone-100 dark:border-stone-800 shadow-sm flex items-center gap-5"
          >
            <div
              class="w-14 h-14 rounded-2xl bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center text-blue-600 dark:text-blue-400"
            >
              <span class="material-symbols-outlined text-3xl">domain</span>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">
                Department
              </p>
              <p class="text-lg font-bold text-slate-800 dark:text-white leading-none">
                {{ course.departmentName || 'N/A' }}
              </p>
            </div>
          </div>
          <div
            class="bg-white dark:bg-surface-dark p-6 rounded-2xl border border-stone-100 dark:border-stone-800 shadow-sm flex items-center gap-5"
          >
            <div
              class="w-14 h-14 rounded-2xl bg-amber-50 dark:bg-amber-900/20 flex items-center justify-center text-amber-600 dark:text-amber-400"
            >
              <span class="material-symbols-outlined text-3xl">stars</span>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">
                Total Credits
              </p>
              <p class="text-lg font-bold text-slate-800 dark:text-white leading-none">
                {{ Number(course.credits).toFixed(1) }} Units
              </p>
            </div>
          </div>
          <div
            class="bg-white dark:bg-surface-dark p-6 rounded-2xl border border-stone-100 dark:border-stone-800 shadow-sm flex items-center gap-5"
          >
            <div
              :class="[
                'w-14 h-14 rounded-2xl flex items-center justify-center',
                course.status === 'ACTIVE'
                  ? 'bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 dark:text-emerald-400'
                  : 'bg-stone-100 dark:bg-stone-800 text-stone-600 dark:text-stone-400',
              ]"
            >
              <span class="material-symbols-outlined text-3xl">verified</span>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Status</p>
              <p class="text-lg font-bold text-slate-800 dark:text-white leading-none">
                {{ course.status }}
              </p>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div class="lg:col-span-2 flex flex-col gap-6">
            <div
              class="bg-white dark:bg-surface-dark rounded-[1.5rem] p-10 border border-stone-100 dark:border-stone-800 shadow-sm"
            >
              <div class="flex items-center gap-3 mb-6">
                <span class="material-symbols-outlined text-primary">description</span>
                <h3 class="text-xl font-bold text-slate-900 dark:text-white">Course Overview</h3>
              </div>
              <p class="text-slate-600 dark:text-slate-300 text-lg leading-relaxed font-body">
                {{ course.description || 'No description provided.' }}
              </p>
            </div>
          </div>
          <div class="lg:col-span-1">
            <div
              class="bg-stone-50 dark:bg-surface-dark/50 rounded-[1.5rem] p-8 border border-stone-100 dark:border-stone-800 h-full"
            >
              <h3 class="text-sm font-bold text-slate-400 uppercase tracking-widest mb-6">
                Administrative Metadata
              </h3>
              <div class="space-y-6">
                <div class="flex flex-col gap-1">
                  <span class="text-xs font-medium text-slate-500 uppercase">Created On</span>
                  <div class="flex items-center gap-2 text-slate-700 dark:text-slate-200">
                    <span class="material-symbols-outlined text-[18px]">calendar_month</span>
                    <span class="font-bold">{{ formatDate(course.createdAt) }}</span>
                  </div>
                </div>
                <div class="flex flex-col gap-1">
                  <span class="text-xs font-medium text-slate-500 uppercase">Last Updated</span>
                  <div class="flex items-center gap-2 text-slate-700 dark:text-slate-200">
                    <span class="material-symbols-outlined text-[18px]">update</span>
                    <span class="font-bold">
                      {{
                        // @ts-ignore
                        course.updatedAt ? formatDate(course.updatedAt) : 'N/A'
                      }}
                    </span>
                  </div>
                </div>
                <!-- Mock data as not in response yet -->
                <div class="flex flex-col gap-1">
                  <span class="text-xs font-medium text-slate-500 uppercase">Created By</span>
                  <div class="flex items-center gap-2 text-slate-700 dark:text-slate-200">
                    <span class="material-symbols-outlined text-[18px]">person</span>
                    <span class="font-bold">{{ course.createdBy || 'Unknown' }}</span>
                  </div>
                </div>
                <div class="pt-6 border-t border-stone-200 dark:border-stone-800">
                  <div class="flex flex-col gap-1">
                    <span class="text-xs font-medium text-slate-500 uppercase">Access Level</span>
                    <div class="flex items-center gap-2">
                      <span class="w-2 h-2 rounded-full bg-primary"></span>
                      <span class="text-sm font-bold text-slate-700 dark:text-slate-200">
                        System-wide Admin Only
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.soft-gradient {
  background: linear-gradient(135deg, #ff8c00 0%, #ffa534 100%);
}
</style>
