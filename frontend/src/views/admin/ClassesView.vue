<script setup lang="ts">
import { ref } from 'vue'

const stats = [
  { label: 'Total Classes', value: '124', icon: 'class', color: 'blue' },
  { label: 'Ongoing Classes', value: '8', icon: 'timelapse', color: 'orange' },
  { label: 'Active Students', value: '3,852', icon: 'groups', color: 'green' },
]

const semesters = ['Fall 2023', 'Spring 2024', 'Summer 2024']
const departments = ['Computer Science', 'English', 'Mathematics', 'Physics']
const statuses = ['Active', 'Cancelled', 'Completed']

const classes = ref([
  {
    id: '#CL-1024',
    name: 'CS101-A',
    course: 'Intro to Comp Sci',
    teacher: 'Prof. Wright',
    teacherImg:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuA7s5uF2OqfXKzZUy2Gk0vFGgSzpTCYs3ujiRptcYm7AUCnxkkNnUWp5_r8X14NSPYtTrfm8LyDiitYO-e2DjIaFObvZO7rCQFn6MH0WAZecomHBLT6u9LdooK4RgWaVZUD-GhDVkk8_1XVugztLc91sGA3Kfyk76ELxew5ho_oUG4cIiJ50bwf-XT5M_ZErt8uRg80ximHfCpFtyIRjDj3XyMP4H-2ApQ1HeWVlZxumSH6XJawODA1chAYUq7e5wkEXTyUYctm1xk',
    schedule: 'Mon, Wed 09:00 AM',
    room: 'Rm 304',
    students: 25,
    capacity: 30,
    progress: 83,
    progressColor: 'bg-primary',
  },
  {
    id: '#CL-1025',
    name: 'ENG202-B',
    course: 'Advanced Literature',
    teacher: 'Dr. Chen',
    teacherImg:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuBMBwAJx3-XV4d-gjhyxJozgYvifSOBHqdertmb8497BlDrr7CXMVmseYW3niej5h9e-XzAyQpQ3et72D1qgWzpz3NQ95Oq4hcjoI3q4FVwaQsKWLavzff2aTLg4W6M3_QcUcNTlPHgsUbteaUOKSEMdiwMmcRZteSC743RYii8m5JRwY1-YLZ7X0HWECKxkaQsw74wSRxJfNX0MTgdMzCHxlchYD46BmG49kvUChK_LqnyfXLGy-mMZY14WR_L9LjtLjnMNiK46-o',
    schedule: 'Tue, Thu 14:00 PM',
    room: 'Hall B',
    students: 18,
    capacity: 40,
    progress: 45,
    progressColor: 'bg-green-500',
  },
  {
    id: '#CL-1026',
    name: 'PHY301-A',
    course: 'Quantum Physics',
    teacher: 'Prof. Ross',
    teacherImg:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuBDVVxA7UrKPxSoyjVtItFa9p4ajqomzKE45Y8WR2hW7RvgY99LgNgP9-IEJr-0-Q0irBy9733fFqYg-rlkSxhdPPaODQi5Ux82-Z3BPpsOHzBusf8KN9zYlpDSEjFYe-oypQeY7QeZFkDsSCdLFmj3KIG03iWBnjyS7nJENj9zZkqgk_xhkAdHeYJe-UpZ3tdgIROkw_kMGppdJLuKgexNYF7-33xxUTmbYpVlGYy-CbfKW5ALn18YhsBGzzCbucEM_X4zae42TZ8',
    schedule: 'Mon, Fri 10:00 AM',
    room: 'Lab 101',
    students: 19,
    capacity: 20,
    progress: 95,
    progressColor: 'bg-red-500',
  },
])

const searchQuery = ref('')

// Add New Class modal state
const showAddClassModal = ref(false)
const createLoading = ref(false)
const createError = ref<string | null>(null)

const newClass = ref({
  courseId: '',
  teacherId: '',
  semesterId: '',
  room: '',
  schedule: '',
  maxStudents: 40,
})

// Mock data for selects (in a real app, these would be fetched from API)
const mockCourses = [
  { id: '1', name: 'Intro to Comp Sci' },
  { id: '2', name: 'Advanced Literature' },
  { id: '3', name: 'Quantum Physics' },
]
const mockTeachers = [
  { id: '1', name: 'Prof. Wright' },
  { id: '2', name: 'Dr. Chen' },
  { id: '3', name: 'Prof. Ross' },
]

function handleAddClass() {
  showAddClassModal.value = true
  createError.value = null
  newClass.value = {
    courseId: '',
    teacherId: '',
    semesterId: '',
    room: '',
    schedule: '',
    maxStudents: 40,
  }
}

function closeAddClassModal() {
  showAddClassModal.value = false
  createError.value = null
}

async function submitNewClass() {
  if (
    !newClass.value.courseId ||
    !newClass.value.semesterId ||
    !newClass.value.room ||
    !newClass.value.schedule
  ) {
    createError.value = 'Please fill in all required fields.'
    return
  }

  try {
    createLoading.value = true
    // Mock API call
    await new Promise((resolve) => setTimeout(resolve, 1000))
    console.log('Creating class:', newClass.value)

    // Add to list for demo purposes
    const selectedCourse = mockCourses.find((c) => c.id === newClass.value.courseId)
    const selectedTeacher = mockTeachers.find((t) => t.id === newClass.value.teacherId)

    classes.value.unshift({
      id: `#CL-${Math.floor(Math.random() * 10000)}`,
      name: `${selectedCourse?.name || 'New Class'}-X`,
      course: selectedCourse?.name || 'Unknown Course',
      teacher: selectedTeacher?.name || 'Unassigned',
      teacherImg:
        'https://lh3.googleusercontent.com/aida-public/AB6AXuBMBwAJx3-XV4d-gjhyxJozgYvifSOBHqdertmb8497BlDrr7CXMVmseYW3niej5h9e-XzAyQpQ3et72D1qgWzpz3NQ95Oq4hcjoI3q4FVwaQsKWLavzff2aTLg4W6M3_QcUcNTlPHgsUbteaUOKSEMdiwMmcRZteSC743RYii8m5JRwY1-YLZ7X0HWECKxkaQsw74wSRxJfNX0MTgdMzCHxlchYD46BmG49kvUChK_LqnyfXLGy-mMZY14WR_L9LjtLjnMNiK46-o',
      schedule: newClass.value.schedule,
      room: newClass.value.room,
      students: 0,
      capacity: newClass.value.maxStudents,
      progress: 0,
      progressColor: 'bg-primary',
    })

    closeAddClassModal()
  } catch {
    createError.value = 'Failed to create class. Please try again.'
  } finally {
    createLoading.value = false
  }
}
</script>

<template>
  <div class="max-w-[1400px] w-full mx-auto p-6 md:p-10 flex flex-col gap-8">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-slate-900 dark:text-white text-3xl font-bold leading-tight tracking-tight">
          Class Management
        </h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">
          Organize and manage physical and virtual class schedules.
        </p>
      </div>
      <div class="flex items-center gap-3">
        <button
          @click="handleAddClass"
          class="flex items-center gap-2 px-6 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95"
        >
          <span class="material-symbols-outlined text-[20px]">add_circle</span>
          Add New Class
        </button>
      </div>
    </div>

    <!-- Stats Grid -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="bg-surface-light dark:bg-surface-dark p-5 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex items-center gap-4"
      >
        <div
          class="p-3 rounded-lg"
          :class="{
            'bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400':
              stat.color === 'blue',
            'bg-orange-50 dark:bg-orange-900/20 text-orange-600 dark:text-orange-400':
              stat.color === 'orange',
            'bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400':
              stat.color === 'green',
          }"
        >
          <span class="material-symbols-outlined text-2xl">{{ stat.icon }}</span>
        </div>
        <div>
          <p class="text-sm font-medium text-slate-500 dark:text-slate-400">{{ stat.label }}</p>
          <h3 class="text-2xl font-bold text-slate-900 dark:text-white">{{ stat.value }}</h3>
        </div>
      </div>
    </div>

    <!-- Filters Bar -->
    <div
      class="bg-surface-light dark:bg-surface-dark p-4 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex flex-col lg:flex-row gap-4"
    >
      <div class="relative flex-1">
        <span
          class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
          >search</span
        >
        <input
          v-model="searchQuery"
          class="w-full pl-10 pr-4 h-11 bg-white dark:bg-stone-900 border-stone-200 dark:border-stone-700 text-sm rounded-lg focus:ring-primary focus:border-primary transition-all text-slate-900 dark:text-white"
          placeholder="Search by class name or ID..."
          type="text"
        />
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <select
          class="h-11 bg-white dark:bg-stone-900 border-stone-200 dark:border-stone-700 text-sm rounded-lg focus:ring-primary focus:border-primary transition-all text-slate-900 dark:text-white"
        >
          <option value="">Semester</option>
          <option v-for="s in semesters" :key="s" :value="s">{{ s }}</option>
        </select>
        <select
          class="h-11 bg-white dark:bg-stone-900 border-stone-200 dark:border-stone-700 text-sm rounded-lg focus:ring-primary focus:border-primary transition-all text-slate-900 dark:text-white"
        >
          <option value="">Department</option>
          <option v-for="d in departments" :key="d" :value="d">{{ d }}</option>
        </select>
        <select
          class="h-11 bg-white dark:bg-stone-900 border-stone-200 dark:border-stone-700 text-sm rounded-lg focus:ring-primary focus:border-primary transition-all text-slate-900 dark:text-white"
        >
          <option value="">Status</option>
          <option v-for="st in statuses" :key="st" :value="st">{{ st }}</option>
        </select>
        <button
          class="flex items-center justify-center gap-2 h-11 px-4 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-slate-700 dark:text-slate-300 rounded-lg font-semibold transition-all"
        >
          <span class="material-symbols-outlined text-[20px]">filter_list</span>
          Clear
        </button>
      </div>
    </div>

    <!-- Table -->
    <div
      class="w-full overflow-hidden rounded-xl border border-stone-200 dark:border-stone-800 bg-surface-light dark:bg-surface-dark shadow-sm"
    >
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr
              class="bg-stone-50 dark:bg-stone-900/50 border-b border-stone-200 dark:border-stone-800"
            >
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Class ID
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Class Name
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Course
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Teacher
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Schedule
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Room
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Capacity
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap text-right"
              >
                Actions
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-200 dark:divide-stone-800">
            <tr
              v-for="cls in classes"
              :key="cls.id"
              class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors"
            >
              <td class="p-4 text-sm font-medium text-slate-500 whitespace-nowrap">{{ cls.id }}</td>
              <td class="p-4 whitespace-nowrap">
                <span class="text-sm font-bold text-slate-900 dark:text-white leading-none">{{
                  cls.name
                }}</span>
              </td>
              <td class="p-4 whitespace-nowrap">
                <span class="text-sm text-slate-600 dark:text-slate-400">{{ cls.course }}</span>
              </td>
              <td class="p-4 whitespace-nowrap">
                <div class="flex items-center gap-2">
                  <div
                    class="w-6 h-6 rounded-full bg-orange-100 dark:bg-orange-900/20 flex items-center justify-center overflow-hidden"
                  >
                    <img :src="cls.teacherImg" alt="Teacher" class="w-full h-full object-cover" />
                  </div>
                  <span class="text-sm text-slate-700 dark:text-slate-300">{{ cls.teacher }}</span>
                </div>
              </td>
              <td class="p-4 text-sm text-slate-600 dark:text-slate-400 whitespace-nowrap">
                {{ cls.schedule }}
              </td>
              <td class="p-4 text-sm text-slate-600 dark:text-slate-400 whitespace-nowrap">
                {{ cls.room }}
              </td>
              <td class="p-4">
                <div class="flex items-center gap-2 text-slate-900 dark:text-white">
                  <div
                    class="w-20 bg-stone-200 dark:bg-stone-700 rounded-full h-1.5 overflow-hidden"
                  >
                    <div
                      class="h-1.5 rounded-full"
                      :class="cls.progressColor"
                      :style="{ width: cls.progress + '%' }"
                    ></div>
                  </div>
                  <span class="text-xs font-medium text-slate-500 dark:text-slate-400"
                    >{{ cls.students }}/{{ cls.capacity }}</span
                  >
                </div>
              </td>
              <td class="p-4 text-right whitespace-nowrap">
                <div class="flex items-center justify-end gap-1">
                  <router-link
                    :to="{
                      name: 'admin-class-detail',
                      params: { classId: cls.id.replace('#', '') },
                    }"
                    class="p-2 text-slate-400 hover:text-blue-500 transition-colors"
                    title="View Students"
                  >
                    <span class="material-symbols-outlined text-[20px]">group</span>
                  </router-link>
                  <button
                    class="p-2 text-slate-400 hover:text-primary transition-colors"
                    title="Edit"
                  >
                    <span class="material-symbols-outlined text-[20px]">edit</span>
                  </button>
                  <button
                    class="p-2 text-slate-400 hover:text-red-500 transition-colors"
                    title="Delete"
                  >
                    <span class="material-symbols-outlined text-[20px]">delete</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div
        class="p-4 border-t border-stone-200 dark:border-stone-800 bg-stone-50/50 dark:bg-stone-900/30 flex items-center justify-between"
      >
        <div class="flex items-center gap-4">
          <span class="text-sm text-slate-500 dark:text-slate-400">Records per page:</span>
          <select
            class="h-9 py-0 border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-sm rounded text-slate-900 dark:text-white"
          >
            <option>20</option>
            <option>50</option>
            <option>100</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-slate-700 dark:text-slate-300 mr-2"
            >Page 1 of 6</span
          >
          <div class="flex gap-1">
            <button
              class="w-9 h-9 flex items-center justify-center rounded border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-400 hover:text-primary transition-colors disabled:opacity-50"
              disabled
            >
              <span class="material-symbols-outlined text-[18px]">chevron_left</span>
            </button>
            <button
              class="w-9 h-9 flex items-center justify-center rounded bg-primary text-white font-bold text-sm shadow-sm shadow-primary/20"
            >
              1
            </button>
            <button
              class="w-9 h-9 flex items-center justify-center rounded border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-600 dark:text-slate-400 hover:bg-stone-50 dark:hover:bg-stone-800 font-medium text-sm transition-colors"
            >
              2
            </button>
            <button
              class="w-9 h-9 flex items-center justify-center rounded border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-600 dark:text-slate-400 hover:bg-stone-50 dark:hover:bg-stone-800 font-medium text-sm transition-colors"
            >
              3
            </button>
            <button
              class="w-9 h-9 flex items-center justify-center rounded border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-400 hover:text-primary transition-colors"
            >
              <span class="material-symbols-outlined text-[18px]">chevron_right</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Add New Class Modal -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="showAddClassModal"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4"
      >
        <div
          class="absolute inset-0 bg-black/50 backdrop-blur-md"
          @click="closeAddClassModal"
        ></div>
        <div
          class="relative bg-white dark:bg-surface-dark w-full max-w-xl rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]"
        >
          <!-- Header -->
          <div
            class="flex items-center justify-between px-6 py-5 border-b border-stone-100 dark:border-stone-800"
          >
            <div class="flex items-center gap-3">
              <div class="p-2 rounded-xl bg-primary/10 text-primary">
                <span class="material-symbols-outlined">add_circle</span>
              </div>
              <h2 class="text-xl font-bold text-slate-900 dark:text-white">Add New Class</h2>
            </div>
            <button
              @click="closeAddClassModal"
              class="p-1.5 rounded-lg hover:bg-stone-100 dark:hover:bg-stone-800 text-slate-400 transition-colors"
            >
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <!-- Form Content -->
          <form @submit.prevent="submitNewClass" class="p-6 overflow-y-auto space-y-5">
            <div
              v-if="createError"
              class="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 text-sm"
            >
              {{ createError }}
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div class="space-y-1.5">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider"
                  >Course <span class="text-red-500">*</span></label
                >
                <select
                  v-model="newClass.courseId"
                  required
                  class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
                >
                  <option value="">Select Course</option>
                  <option v-for="c in mockCourses" :key="c.id" :value="c.id">{{ c.name }}</option>
                </select>
              </div>
              <div class="space-y-1.5">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider"
                  >Semester <span class="text-red-500">*</span></label
                >
                <select
                  v-model="newClass.semesterId"
                  required
                  class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
                >
                  <option value="">Select Semester</option>
                  <option v-for="s in semesters" :key="s" :value="s">{{ s }}</option>
                </select>
              </div>
            </div>

            <div class="space-y-1.5">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider"
                >Teacher</label
              >
              <select
                v-model="newClass.teacherId"
                class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
              >
                <option value="">Select Teacher (Optional)</option>
                <option v-for="t in mockTeachers" :key="t.id" :value="t.id">{{ t.name }}</option>
              </select>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div class="space-y-1.5">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider"
                  >Room <span class="text-red-500">*</span></label
                >
                <input
                  v-model="newClass.room"
                  required
                  type="text"
                  placeholder="e.g. Rm 304"
                  class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
                />
              </div>
              <div class="space-y-1.5">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider"
                  >Max Students</label
                >
                <input
                  v-model="newClass.maxStudents"
                  type="number"
                  min="1"
                  class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
                />
              </div>
            </div>

            <div class="space-y-1.5">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider"
                >Schedule <span class="text-red-500">*</span></label
              >
              <input
                v-model="newClass.schedule"
                required
                type="text"
                placeholder="e.g. Mon, Wed 09:00 AM"
                class="w-full px-3 py-2 border border-stone-200 dark:border-stone-700 rounded-lg bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
              />
            </div>

            <!-- Footer -->
            <div
              class="pt-4 flex items-center justify-end gap-3 border-t border-stone-100 dark:border-stone-800"
            >
              <button
                type="button"
                @click="closeAddClassModal"
                class="px-5 py-2.5 rounded-lg text-sm font-bold text-slate-600 dark:text-slate-400 hover:bg-stone-100 dark:hover:bg-stone-800 transition-all"
              >
                Cancel
              </button>
              <button
                type="submit"
                :disabled="createLoading"
                class="flex items-center gap-2 px-8 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span
                  v-if="createLoading"
                  class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"
                ></span>
                <span>{{ createLoading ? 'Creating...' : 'Create Class' }}</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
