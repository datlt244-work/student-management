<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const classId = route.params.classId as string

const classInfo = {
  id: classId || 'CS101-A',
  name: 'Computer Science Basics',
  semester: 'Fall Semester 2023',
  section: 'Section A',
  teacher: 'Sarah Chen',
  teacherImg:
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBMBwAJx3-XV4d-gjhyxJozgYvifSOBHqdertmb8497BlDrr7CXMVmseYW3niej5h9e-XzAyQpQ3et72D1qgWzpz3NQ95Oq4hcjoI3q4FVwaQsKWLavzff2aTLg4W6M3_QcUcNTlPHgsUbteaUOKSEMdiwMmcRZteSC743RYii8m5JRwY1-YLZ7X0HWECKxkaQsw74wSRxJfNX0MTgdMzCHxlchYD46BmG49kvUChK_LqnyfXLGy-mMZY14WR_L9LjtLjnMNiK46-o',
  schedule: 'Mon, Wed 10:00 AM',
  room: 'Lab 304',
  students: 28,
  capacity: 30,
}

const students = ref([
  {
    id: '#ST-2023-001',
    name: 'Michael Ross',
    email: 'm.ross@student.edu',
    img: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBDVVxA7UrKPxSoyjVtItFa9p4ajqomzKE45Y8WR2hW7RvgY99LgNgP9-IEJr-0-Q0irBy9733fFqYg-rlkSxhdPPaODQi5Ux82-Z3BPpsOHzBusf8KN9zYlpDSEjFYe-oypQeY7QeZFkDsSCdLFmj3KIG03iWBnjyS7nJENj9zZkqgk_xhkAdHeYJe-UpZ3tdgIROkw_kMGppdJLuKgexNYF7-33xxUTmbYpVlGYy-CbfKW5ALn18YhsBGzzCbucEM_X4zae42TZ8',
    attendance: 95,
  },
  {
    id: '#ST-2023-045',
    name: 'Robert Thorne',
    email: 'r.thorne@edu.com',
    img: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAncuoAztnF7-cFNCfogpH__DkcaOcjkWyDmXkqHAneXIwrHmU7d5bVz4azBjtrj35_LcdcCB6Pfvk05G78xoI730ufebp313yIsuI8UzwgojOeBTo0jhBk1dM3dah1FwyLe2y4LRg2vJBmy-nWBtdrIm42gOHrcOFTirJB4OdmEk1F4KFobuqsw_LuRfJrgnyeEDkJfv0iVXzRXrIMUULD6tdzjUDElxcnxj5OBRDPFRkJ7_FwDBJEIQqXSB7QAH3q3Dundy4cuq0',
    attendance: 78,
  },
  {
    id: '#ST-2023-089',
    name: 'Jessica Bloom',
    email: 'j.bloom@student.edu',
    img: '',
    attendance: 92,
  },
  {
    id: '#ST-2023-112',
    name: 'David Kim',
    email: 'd.kim@student.edu',
    img: '',
    attendance: 45,
  },
])

const searchQuery = ref('')
</script>

<template>
  <div class="max-w-[1400px] w-full mx-auto p-6 md:p-10 flex flex-col gap-8">
    <!-- Breadcrumbs & Header -->
    <div class="flex flex-col gap-6">
      <div class="flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
        <router-link :to="{ name: 'admin-classes' }" class="hover:text-primary transition-colors"
          >Classes</router-link
        >
        <span class="material-symbols-outlined text-xs">chevron_right</span>
        <span class="font-medium text-slate-900 dark:text-white">{{ classInfo.id }}</span>
      </div>
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1
            class="text-slate-900 dark:text-white text-3xl font-bold leading-tight tracking-tight"
          >
            {{ classInfo.id }} : {{ classInfo.name }}
          </h1>
          <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">
            {{ classInfo.semester }} • {{ classInfo.section }}
          </p>
        </div>
        <div class="flex items-center gap-3">
          <button
            class="flex items-center gap-2 px-6 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95"
          >
            <span class="material-symbols-outlined text-[20px]">person_add</span>
            Add Student
          </button>
        </div>
      </div>
    </div>

    <!-- Info Cards -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div
        class="lg:col-span-2 grid grid-cols-1 sm:grid-cols-3 gap-4 bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm"
      >
        <div class="flex flex-col gap-1">
          <span
            class="text-xs uppercase tracking-wider font-bold text-slate-500 dark:text-slate-400"
            >Teacher</span
          >
          <div class="flex items-center gap-2 mt-1">
            <div
              class="w-8 h-8 rounded-full bg-stone-100 dark:bg-stone-800 flex items-center justify-center"
            >
              <img
                :src="classInfo.teacherImg"
                alt="Teacher"
                class="w-full h-full object-cover rounded-full"
              />
            </div>
            <span class="font-semibold text-slate-900 dark:text-white">{{
              classInfo.teacher
            }}</span>
          </div>
        </div>
        <div class="flex flex-col gap-1 border-stone-100 dark:border-stone-800 sm:border-l sm:pl-4">
          <span
            class="text-xs uppercase tracking-wider font-bold text-slate-500 dark:text-slate-400"
            >Schedule</span
          >
          <div class="flex items-center gap-2 mt-1">
            <span class="material-symbols-outlined text-primary text-[20px]">schedule</span>
            <span class="font-medium text-slate-900 dark:text-white">{{ classInfo.schedule }}</span>
          </div>
        </div>
        <div class="flex flex-col gap-1 border-stone-100 dark:border-stone-800 sm:border-l sm:pl-4">
          <span
            class="text-xs uppercase tracking-wider font-bold text-slate-500 dark:text-slate-400"
            >Room</span
          >
          <div class="flex items-center gap-2 mt-1">
            <span class="material-symbols-outlined text-primary text-[20px]">meeting_room</span>
            <span class="font-medium text-slate-900 dark:text-white">{{ classInfo.room }}</span>
          </div>
        </div>
      </div>
      <div
        class="bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex flex-col justify-center"
      >
        <div class="flex items-center justify-between mb-2">
          <span class="text-sm font-semibold text-slate-700 dark:text-slate-200"
            >Class Capacity</span
          >
          <span class="text-sm font-bold text-primary"
            >{{ classInfo.students }}/{{ classInfo.capacity }} Filled</span
          >
        </div>
        <div class="w-full bg-stone-100 dark:bg-stone-800 rounded-full h-3 overflow-hidden">
          <div
            class="bg-primary h-3 rounded-full transition-all duration-500"
            :style="{ width: (classInfo.students / classInfo.capacity) * 100 + '%' }"
          ></div>
        </div>
        <p class="text-xs text-slate-500 dark:text-slate-400 mt-2">
          Only {{ classInfo.capacity - classInfo.students }} seats remaining for this semester.
        </p>
      </div>
    </div>

    <!-- Students Table -->
    <div
      class="@container w-full overflow-hidden rounded-xl border border-stone-200 dark:border-stone-800 bg-surface-light dark:bg-surface-dark shadow-sm"
    >
      <div
        class="p-4 border-b border-stone-200 dark:border-stone-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
      >
        <h3 class="font-bold text-lg text-slate-900 dark:text-white">Enrolled Students</h3>
        <div class="relative w-full sm:w-64">
          <span
            class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
            >search</span
          >
          <input
            v-model="searchQuery"
            class="w-full pl-10 pr-4 h-10 bg-stone-50 dark:bg-stone-900 border-stone-200 dark:border-stone-700 rounded-lg text-sm transition-all focus:ring-2 focus:ring-primary focus:border-primary text-slate-900 dark:text-white"
            placeholder="Search student..."
            type="text"
          />
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr
              class="bg-stone-50 dark:bg-stone-900/50 border-b border-stone-200 dark:border-stone-800"
            >
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Student ID
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Student Name
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Email
              </th>
              <th
                class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap"
              >
                Attendance Rate
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
              v-for="student in students"
              :key="student.id"
              class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors"
            >
              <td class="p-4 text-sm font-medium text-slate-500 whitespace-nowrap">
                {{ student.id }}
              </td>
              <td class="p-4 whitespace-nowrap">
                <div class="flex items-center gap-3">
                  <div
                    class="w-8 h-8 rounded-full bg-indigo-100 dark:bg-indigo-900/20 text-indigo-600 flex items-center justify-center font-bold overflow-hidden border border-indigo-200/20"
                  >
                    <img
                      v-if="student.img"
                      :src="student.img"
                      alt="Student"
                      class="w-full h-full object-cover"
                    />
                    <span v-else class="material-symbols-outlined text-lg">person</span>
                  </div>
                  <span class="text-sm font-bold text-slate-900 dark:text-white">{{
                    student.name
                  }}</span>
                </div>
              </td>
              <td class="p-4 text-sm text-slate-600 dark:text-slate-400">{{ student.email }}</td>
              <td class="p-4">
                <div class="flex items-center gap-2">
                  <div class="w-24 bg-stone-200 dark:bg-stone-700 rounded-full h-2 overflow-hidden">
                    <div
                      class="h-2 rounded-full"
                      :class="{
                        'bg-green-500': student.attendance >= 90,
                        'bg-yellow-500': student.attendance >= 70 && student.attendance < 90,
                        'bg-red-500': student.attendance < 70,
                      }"
                      :style="{ width: student.attendance + '%' }"
                    ></div>
                  </div>
                  <span class="text-xs font-bold text-slate-700 dark:text-slate-300"
                    >{{ student.attendance }}%</span
                  >
                </div>
              </td>
              <td class="p-4 text-right whitespace-nowrap">
                <button
                  class="text-red-500 hover:text-red-700 dark:hover:text-red-400 text-xs font-medium border border-red-200 dark:border-red-900/30 bg-red-50 dark:bg-red-900/10 px-3 py-1.5 rounded-md transition-colors"
                >
                  Remove from Class
                </button>
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
            >Page 1 of 2</span
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
              class="w-9 h-9 flex items-center justify-center rounded border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-400 hover:text-primary transition-colors"
            >
              <span class="material-symbols-outlined text-[18px]">chevron_right</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
