<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const user = computed(() => authStore.user)

const displayName = computed(() => {
  if (user.value?.email) {
    const name = user.value.email.split('@')[0]
    return (name?.charAt(0).toUpperCase() ?? '') + (name?.slice(1) ?? '')
  }
  return 'Student'
})

// Active tab
const activeTab = ref<'personal' | 'academic' | 'settings'>('personal')

const tabs = [
  { key: 'personal' as const, label: 'Personal Info' },
  { key: 'academic' as const, label: 'Academic Status' },
  { key: 'settings' as const, label: 'Account Settings' },
]

// Profile sidebar nav
const sidebarNav = [
  { label: 'Personal Profile', icon: 'person', routeName: 'student-profile' },
  { label: 'Courses', icon: 'school', routeName: 'student-courses' },
  { label: 'Grades', icon: 'analytics', routeName: 'student-grades' },
  { label: 'Financials', icon: 'payments', routeName: '' },
]

// Form data — will be fetched from API later
const formData = ref({
  fullName: 'Alex Johnson',
  dateOfBirth: '2002-05-15',
  gender: 'Male',
  email: 'alex.johnson@university.edu',
  phone: '+1 (555) 012-3456',
  address: '123 Campus Dr, Apt 4B',
})

// Summary info
const summaryCards = [
  { label: 'Current Semester', value: 'Fall 2024', isHighlight: false },
  { label: 'Academic Advisor', value: 'Dr. Sarah Miller', isHighlight: false },
  { label: 'GPA', value: '3.82 / 4.0', isHighlight: true },
]

function handleUpdateProfile() {
  // TODO: call API to update profile
  console.log('Update profile:', formData.value)
}
</script>

<template>
  <div class="flex-1 flex flex-col items-center py-6 px-4 lg:px-20">
    <div class="w-full max-w-[1200px]">
      <!-- Breadcrumbs -->
      <div class="flex flex-wrap gap-2 py-4 items-center">
        <router-link
          :to="{ name: 'student-dashboard' }"
          class="text-text-muted-light dark:text-text-muted-dark text-sm font-medium leading-normal hover:text-primary transition-colors"
        >
          Dashboard
        </router-link>
        <span class="material-symbols-outlined text-sm text-text-muted-light dark:text-text-muted-dark">chevron_right</span>
        <span class="text-sm font-medium leading-normal">Personal Profile</span>
      </div>

      <!-- Profile Layout -->
      <div class="flex flex-col lg:flex-row gap-8 items-start">
        <!-- Sidebar (Profile Summary) -->
        <aside class="w-full lg:w-1/3 flex flex-col gap-6">
          <div class="bg-surface-light dark:bg-surface-dark p-8 rounded-xl shadow-sm border border-border-light dark:border-border-dark flex flex-col items-center">
            <!-- Avatar -->
            <div class="relative group">
              <div
                v-if="user?.profilePictureUrl"
                class="bg-center bg-no-repeat aspect-square bg-cover rounded-full size-40 border-4 border-primary/20"
                :style="{ backgroundImage: `url(${user.profilePictureUrl})` }"
              ></div>
              <div
                v-else
                class="rounded-full size-40 border-4 border-primary/20 bg-primary/10 flex items-center justify-center text-primary font-bold text-4xl"
              >
                {{ displayName.substring(0, 2).toUpperCase() }}
              </div>
              <div class="absolute bottom-2 right-2 bg-primary text-white p-1.5 rounded-full shadow-lg cursor-pointer hover:brightness-110 transition-all">
                <span class="material-symbols-outlined text-base">photo_camera</span>
              </div>
            </div>

            <!-- Name & Info -->
            <div class="mt-6 text-center">
              <h1 class="text-2xl font-bold leading-tight">{{ displayName }}</h1>
              <p class="text-primary font-medium mt-1">B.Sc. in Computer Science</p>
              <p class="text-text-muted-light dark:text-text-muted-dark text-sm font-normal mt-1">Student ID: 202300154</p>
            </div>

            <!-- Edit Photo Button -->
            <button class="mt-8 flex w-full items-center justify-center gap-2 rounded-lg h-12 bg-primary/10 text-primary hover:bg-primary hover:text-white transition-all font-bold text-sm tracking-wide">
              <span class="material-symbols-outlined text-sm">edit</span>
              <span>Edit Photo</span>
            </button>

            <hr class="w-full my-8 border-border-light dark:border-border-dark" />

            <!-- Sidebar Nav -->
            <div class="w-full flex flex-col gap-3">
              <router-link
                v-for="item in sidebarNav"
                :key="item.label"
                :to="item.routeName ? { name: item.routeName } : '#'"
                :class="[
                  'flex items-center gap-3 px-3 py-3 rounded-lg transition-colors',
                  item.routeName === 'student-profile'
                    ? 'bg-primary/10 text-primary'
                    : 'hover:bg-stone-100 dark:hover:bg-stone-800 cursor-pointer',
                ]"
              >
                <span class="material-symbols-outlined">{{ item.icon }}</span>
                <p :class="['text-sm', item.routeName === 'student-profile' ? 'font-bold' : 'font-medium']">
                  {{ item.label }}
                </p>
              </router-link>
            </div>
          </div>
        </aside>

        <!-- Main Section (Tabbed Info) -->
        <section class="flex-1 w-full bg-surface-light dark:bg-surface-dark rounded-xl shadow-sm border border-border-light dark:border-border-dark overflow-hidden">
          <!-- Tabs Navigation -->
          <div class="border-b border-border-light dark:border-border-dark px-6 flex gap-8">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              :class="[
                'flex flex-col items-center justify-center border-b-[3px] pb-3 pt-5 transition-colors text-sm font-bold tracking-wide',
                activeTab === tab.key
                  ? 'border-primary'
                  : 'border-transparent text-text-muted-light dark:text-text-muted-dark hover:text-primary',
              ]"
              @click="activeTab = tab.key"
            >
              {{ tab.label }}
            </button>
          </div>

          <!-- Personal Info Tab -->
          <div v-if="activeTab === 'personal'" class="p-8">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
              <div class="flex flex-col gap-2">
                <label class="text-sm font-bold">Full Name</label>
                <input
                  v-model="formData.fullName"
                  class="form-input rounded-lg border-border-light dark:border-border-dark bg-transparent text-sm focus:border-primary focus:ring-primary h-12"
                  type="text"
                />
              </div>
              <div class="flex flex-col gap-2">
                <label class="text-sm font-bold">Date of Birth</label>
                <input
                  v-model="formData.dateOfBirth"
                  class="form-input rounded-lg border-border-light dark:border-border-dark bg-transparent text-sm focus:border-primary focus:ring-primary h-12"
                  type="date"
                />
              </div>
              <div class="flex flex-col gap-2">
                <label class="text-sm font-bold">Gender</label>
                <select
                  v-model="formData.gender"
                  class="form-select rounded-lg border-border-light dark:border-border-dark bg-transparent text-sm focus:border-primary focus:ring-primary h-12"
                >
                  <option>Male</option>
                  <option>Female</option>
                  <option>Other</option>
                  <option>Prefer not to say</option>
                </select>
              </div>
              <div class="flex flex-col gap-2">
                <label class="text-sm font-bold">Email Address</label>
                <input
                  v-model="formData.email"
                  class="form-input rounded-lg border-border-light dark:border-border-dark bg-transparent text-sm focus:border-primary focus:ring-primary h-12"
                  type="email"
                />
              </div>
              <div class="flex flex-col gap-2">
                <label class="text-sm font-bold">Phone Number</label>
                <input
                  v-model="formData.phone"
                  class="form-input rounded-lg border-border-light dark:border-border-dark bg-transparent text-sm focus:border-primary focus:ring-primary h-12"
                  type="tel"
                />
              </div>
              <div class="flex flex-col gap-2">
                <label class="text-sm font-bold">Address</label>
                <input
                  v-model="formData.address"
                  class="form-input rounded-lg border-border-light dark:border-border-dark bg-transparent text-sm focus:border-primary focus:ring-primary h-12"
                  type="text"
                />
              </div>
            </div>

            <!-- Profile Security & Save -->
            <div class="mt-12 flex flex-col sm:flex-row items-center justify-between gap-6 p-6 rounded-xl bg-stone-50/50 dark:bg-stone-800/30 border border-border-light dark:border-border-dark">
              <div class="flex gap-4 items-center">
                <div class="flex size-12 items-center justify-center rounded-full bg-primary/20 text-primary">
                  <span class="material-symbols-outlined">verified_user</span>
                </div>
                <div>
                  <p class="text-sm font-bold">Profile Security</p>
                  <p class="text-xs text-text-muted-light dark:text-text-muted-dark">Last updated 2 days ago</p>
                </div>
              </div>
              <button
                class="w-full sm:w-auto min-w-[180px] bg-primary text-white py-3 px-8 rounded-lg font-bold text-sm shadow-md shadow-primary/20 hover:brightness-110 transition-all flex items-center justify-center gap-2"
                @click="handleUpdateProfile"
              >
                <span class="material-symbols-outlined text-sm">save</span>
                Update Profile
              </button>
            </div>
          </div>

          <!-- Academic Status Tab -->
          <div v-if="activeTab === 'academic'" class="p-8">
            <div class="text-center py-16 text-text-muted-light dark:text-text-muted-dark">
              <span class="material-symbols-outlined text-5xl mb-4 block">school</span>
              <p class="text-lg font-bold">Academic Status</p>
              <p class="text-sm mt-2">Detailed academic information will be displayed here.</p>
            </div>
          </div>

          <!-- Account Settings Tab -->
          <div v-if="activeTab === 'settings'" class="p-8">
            <div class="text-center py-16 text-text-muted-light dark:text-text-muted-dark">
              <span class="material-symbols-outlined text-5xl mb-4 block">settings</span>
              <p class="text-lg font-bold">Account Settings</p>
              <p class="text-sm mt-2">Account configuration options will be displayed here.</p>
            </div>
          </div>

          <!-- Summary Cards -->
          <div class="px-8 pb-8 grid grid-cols-1 md:grid-cols-3 gap-4">
            <div
              v-for="card in summaryCards"
              :key="card.label"
              class="bg-background-light dark:bg-stone-800 p-4 rounded-lg flex flex-col gap-1 border border-border-light dark:border-border-dark"
            >
              <span class="text-text-muted-light dark:text-text-muted-dark text-xs font-bold uppercase tracking-wider">{{ card.label }}</span>
              <span :class="['font-bold', card.isHighlight ? 'text-primary' : '']">{{ card.value }}</span>
            </div>
          </div>
        </section>
      </div>
    </div>

    <!-- Footer -->
    <footer class="w-full max-w-[1200px] py-8 border-t border-border-light dark:border-border-dark text-center mt-12">
      <p class="text-text-muted-light dark:text-text-muted-dark text-xs">&copy; 2024 University Student Management System. All rights reserved.</p>
    </footer>
  </div>
</template>
