<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const searchQuery = ref('')
const statusFilter = ref('')
const roleFilter = ref('')
const sortBy = ref('')
const pageSize = ref(20)
const currentPage = ref(1)
const totalPages = computed(() => 12)

const paginationPages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const pages: number[] = []
  const start = Math.max(1, current - 1)
  const end = Math.min(total, current + 1)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// Placeholder data — sẽ fetch từ API sau
const users = ref([
  {
    id: 'US-001',
    userId: '00000000-0000-0000-0000-000000000001',
    name: 'Alexander Wright',
    email: 'alex.wright@edu.com',
    avatar: 'https://ui-avatars.com/api/?name=Alexander+Wright&background=f97316&color=fff',
    role: 'ADMIN',
    status: 'ACTIVE',
    createdAt: 'Oct 12, 2023',
    roleClasses: 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400',
    statusClasses: 'bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-400 ring-green-600/20',
    statusDot: 'bg-green-500',
  },
  {
    id: 'US-002',
    userId: '00000000-0000-0000-0000-000000000002',
    name: 'Sarah Chen',
    email: 's.chen@school.org',
    avatar: 'https://ui-avatars.com/api/?name=Sarah+Chen&background=9333ea&color=fff',
    role: 'TEACHER',
    status: 'PENDING_VERIFICATION',
    createdAt: 'Oct 14, 2023',
    roleClasses: 'bg-purple-50 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400',
    statusClasses: 'bg-yellow-50 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400 ring-yellow-600/20',
    statusDot: 'bg-yellow-500',
  },
  {
    id: 'US-003',
    userId: '00000000-0000-0000-0000-000000000003',
    name: 'Michael Ross',
    email: 'm.ross@student.edu',
    avatar: 'https://ui-avatars.com/api/?name=Michael+Ross&background=4f46e5&color=fff',
    role: 'STUDENT',
    status: 'INACTIVE',
    createdAt: 'Oct 15, 2023',
    roleClasses: 'bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400',
    statusClasses: 'bg-stone-100 dark:bg-stone-800 text-stone-600 dark:text-stone-400 ring-stone-500/20',
    statusDot: 'bg-stone-400',
  },
  {
    id: 'US-004',
    userId: '00000000-0000-0000-0000-000000000004',
    name: 'Robert Thorne',
    email: 'r.thorne@edu.com',
    avatar: 'https://ui-avatars.com/api/?name=Robert+Thorne&background=e11d48&color=fff',
    role: 'STUDENT',
    status: 'BLOCKED',
    createdAt: 'Sep 28, 2023',
    roleClasses: 'bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400',
    statusClasses: 'bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-400 ring-red-600/20',
    statusDot: 'bg-red-500',
  },
])

const filteredUsers = computed(() => {
  let list = [...users.value]
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    list = list.filter((u) => u.email.toLowerCase().includes(q) || u.name.toLowerCase().includes(q))
  }
  if (statusFilter.value) {
    list = list.filter((u) => u.status === statusFilter.value)
  }
  if (roleFilter.value) {
    list = list.filter((u) => u.role === roleFilter.value)
  }
  return list
})

function clearFilters() {
  searchQuery.value = ''
  statusFilter.value = ''
  roleFilter.value = ''
  sortBy.value = ''
}

function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

function handleEdit(user: (typeof users.value)[0]) {
  if (user.role === 'STUDENT' || user.role === 'TEACHER') {
    const uid = (user as { userId?: string }).userId ?? user.id
    router.push({ name: 'admin-user-detail', params: { userId: uid } })
  } else {
    // TODO: mở modal/form edit cho admin
    console.log('Edit', user)
  }
}

function handleDelete(user: (typeof users.value)[0]) {
  console.log('Delete', user)
  // TODO: confirm + gọi API
}

function handleAddUser() {
  // TODO: mở modal/form thêm user
}
</script>

<template>
  <div class="max-w-[1400px] w-full mx-auto p-6 md:p-10 flex flex-col gap-8">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-slate-900 dark:text-white text-3xl font-bold leading-tight tracking-tight">User Management</h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">Manage student, teacher, and administrator accounts across the system.</p>
      </div>
      <button
        class="flex items-center gap-2 px-6 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg font-bold shadow-lg shadow-primary/20 transition-all active:scale-95 shrink-0"
        @click="handleAddUser"
      >
        <span class="material-symbols-outlined text-[20px]">person_add</span>
        Add New User
      </button>
    </div>

    <!-- Filters -->
    <div class="bg-surface-light dark:bg-surface-dark p-4 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex flex-col lg:flex-row gap-4">
      <div class="relative flex-1 min-w-0">
        <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]">search</span>
        <input
          v-model="searchQuery"
          class="w-full pl-10 pr-4 h-11 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
          placeholder="Search by email..."
          type="text"
        />
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <select
          v-model="statusFilter"
          class="h-11 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
        >
          <option value="">Status</option>
          <option value="ACTIVE">ACTIVE</option>
          <option value="INACTIVE">INACTIVE</option>
          <option value="BLOCKED">BLOCKED</option>
          <option value="PENDING_VERIFICATION">PENDING_VERIFICATION</option>
        </select>
        <select
          v-model="roleFilter"
          class="h-11 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
        >
          <option value="">Role</option>
          <option value="ADMIN">ADMIN</option>
          <option value="TEACHER">TEACHER</option>
          <option value="STUDENT">STUDENT</option>
        </select>
        <select
          v-model="sortBy"
          class="h-11 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-sm focus:ring-primary focus:border-primary"
        >
          <option value="">Sort By</option>
          <option value="newest">Newest First</option>
          <option value="oldest">Oldest First</option>
          <option value="name_asc">Name A-Z</option>
          <option value="name_desc">Name Z-A</option>
        </select>
        <button
          class="flex items-center justify-center gap-2 h-11 px-4 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-slate-700 dark:text-slate-300 rounded-lg font-semibold transition-all"
          @click="clearFilters"
        >
          <span class="material-symbols-outlined text-[20px]">filter_list</span>
          Clear
        </button>
      </div>
    </div>

    <!-- Table -->
    <div class="w-full overflow-hidden rounded-xl border border-stone-200 dark:border-stone-800 bg-surface-light dark:bg-surface-dark shadow-sm">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-stone-50 dark:bg-stone-900/50 border-b border-stone-200 dark:border-stone-800">
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap">ID</th>
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap">User</th>
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap">Role</th>
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap">Status</th>
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap">Created At</th>
              <th class="p-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 whitespace-nowrap text-right">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-200 dark:divide-stone-800">
            <tr
              v-for="user in filteredUsers"
              :key="user.id"
              class="hover:bg-stone-50 dark:hover:bg-stone-800/50 transition-colors"
            >
              <td class="p-4 text-sm font-medium text-slate-500 whitespace-nowrap">{{ user.id }}</td>
              <td class="p-4 whitespace-nowrap">
                <div class="flex items-center gap-3">
                  <div
                    class="w-10 h-10 rounded-full overflow-hidden border border-primary/20 flex items-center justify-center bg-orange-100 dark:bg-orange-900/20"
                  >
                    <img
                      v-if="user.avatar"
                      :src="user.avatar"
                      :alt="user.name"
                      class="w-full h-full object-cover"
                    />
                    <span v-else class="text-primary font-bold text-sm">{{ user.name.substring(0, 2).toUpperCase() }}</span>
                  </div>
                  <div class="flex flex-col min-w-0">
                    <span class="text-sm font-bold text-slate-900 dark:text-white leading-none truncate">{{ user.name }}</span>
                    <span class="text-xs text-slate-500 dark:text-slate-400 mt-1 truncate">{{ user.email }}</span>
                  </div>
                </div>
              </td>
              <td class="p-4">
                <span :class="['text-xs font-bold px-2 py-1 rounded-md', user.roleClasses]">{{ user.role }}</span>
              </td>
              <td class="p-4">
                <span :class="['inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-bold ring-1 ring-inset', user.statusClasses]">
                  <span :class="['w-1.5 h-1.5 rounded-full', user.statusDot]"></span>
                  {{ user.status }}
                </span>
              </td>
              <td class="p-4 text-sm text-slate-600 dark:text-slate-400 whitespace-nowrap">{{ user.createdAt }}</td>
              <td class="p-4 text-right whitespace-nowrap">
                <div class="flex items-center justify-end gap-2">
                  <button
                    class="p-2 text-slate-400 hover:text-primary transition-colors rounded-lg hover:bg-stone-100 dark:hover:bg-stone-800"
                    aria-label="View detail"
                    @click="handleEdit(user)"
                  >
                    <span class="material-symbols-outlined text-[20px]">visibility</span>
                  </button>
                  <button
                    class="p-2 text-slate-400 hover:text-red-500 transition-colors rounded-lg hover:bg-stone-100 dark:hover:bg-stone-800"
                    aria-label="Delete user"
                    @click="handleDelete(user)"
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
        class="p-4 border-t border-stone-200 dark:border-stone-800 bg-stone-50/50 dark:bg-stone-900/30 flex flex-col sm:flex-row items-center justify-between gap-4"
      >
        <div class="flex items-center gap-4">
          <span class="text-sm text-slate-500 dark:text-slate-400">Records per page:</span>
          <select
            v-model="pageSize"
            class="h-9 py-0 pr-8 pl-3 rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-sm"
          >
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-slate-700 dark:text-slate-300 mr-2">Page {{ currentPage }} of {{ totalPages }}</span>
          <div class="flex gap-1">
            <button
              class="w-9 h-9 flex items-center justify-center rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-400 hover:text-primary transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="currentPage <= 1"
              aria-label="Previous page"
              @click="goToPage(currentPage - 1)"
            >
              <span class="material-symbols-outlined text-[18px]">chevron_left</span>
            </button>
            <button
              v-for="p in paginationPages"
              :key="p"
              :class="[
                'w-9 h-9 flex items-center justify-center rounded-lg font-medium text-sm transition-colors',
                currentPage === p
                  ? 'bg-primary text-white shadow-sm shadow-primary/20'
                  : 'border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-600 dark:text-slate-400 hover:bg-stone-50 dark:hover:bg-stone-800',
              ]"
              @click="goToPage(p)"
            >
              {{ p }}
            </button>
            <button
              class="w-9 h-9 flex items-center justify-center rounded-lg border border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-900 text-slate-400 hover:text-primary transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="currentPage >= totalPages"
              aria-label="Next page"
              @click="goToPage(currentPage + 1)"
            >
              <span class="material-symbols-outlined text-[18px]">chevron_right</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
