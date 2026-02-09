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
  return 'Teacher'
})

// Active tab
const activeTab = ref<'professional' | 'portfolio' | 'security'>('professional')

const tabs = [
  { key: 'professional' as const, label: 'Professional Info' },
  { key: 'portfolio' as const, label: 'Teaching Portfolio' },
  { key: 'security' as const, label: 'Account Security' },
]

// Form data — will be fetched from API later
const formData = ref({
  academicRank: 'Associate Professor',
  specialization: 'Artificial Intelligence',
  officeRoom: 'Building C, Room 402',
  workEmail: 'jane.smith@university.edu',
  phone: '+1 (555) 012-3456',
})

// Profile info
const profileInfo = [
  { icon: 'badge', label: 'Employee ID', value: 'SMS-2024-001' },
  { icon: 'account_balance', label: 'Department', value: 'Computer Science' },
  { icon: 'event_available', label: 'Joined Date', value: 'August 15, 2018' },
]

// Stats cards
const statsCards = [
  { icon: 'menu_book', label: 'Active Courses', value: '4' },
  { icon: 'group', label: 'Total Students', value: '128' },
  { icon: 'star', label: 'Exp. Level', value: 'Senior' },
]

function handleSave() {
  // TODO: call API to save profile
  console.log('Save profile:', formData.value)
}

function handleDiscard() {
  // TODO: reset form data to original values
  console.log('Discard changes')
}
</script>

<template>
  <div class="flex-1 bg-background-light dark:bg-background-dark p-6 lg:p-10">
    <div class="max-w-5xl mx-auto space-y-6">
      <!-- Breadcrumbs -->
      <div class="flex flex-wrap gap-2 items-center">
        <router-link
          :to="{ name: 'teacher-dashboard' }"
          class="text-text-muted-light dark:text-text-muted-dark text-sm font-medium hover:text-primary transition-colors"
        >
          Home
        </router-link>
        <span class="material-symbols-outlined text-sm text-text-muted-light dark:text-text-muted-dark">chevron_right</span>
        <span class="text-sm font-bold">Teacher Profile</span>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Left: Profile Card -->
        <div class="lg:col-span-1">
          <div class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark p-6 text-center shadow-sm">
            <!-- Avatar -->
            <div class="relative inline-block mb-4">
              <div
                v-if="user?.profilePictureUrl"
                class="bg-center bg-no-repeat aspect-square bg-cover rounded-xl size-32 mx-auto ring-4 ring-primary/20"
                :style="{ backgroundImage: `url(${user.profilePictureUrl})` }"
              ></div>
              <div
                v-else
                class="rounded-xl size-32 mx-auto ring-4 ring-primary/20 bg-primary/10 flex items-center justify-center text-primary font-bold text-3xl"
              >
                {{ displayName.substring(0, 2).toUpperCase() }}
              </div>
              <div class="absolute -bottom-2 -right-2 bg-primary text-white rounded-full p-2 shadow-lg cursor-pointer hover:brightness-110 transition-all">
                <span class="material-symbols-outlined text-sm">edit</span>
              </div>
            </div>

            <!-- Name & Title -->
            <h3 class="text-xl font-bold">{{ displayName }}</h3>
            <p class="text-primary font-medium text-sm">Professor &amp; Researcher</p>

            <!-- Info Items -->
            <div class="mt-6 space-y-3 text-left">
              <div
                v-for="item in profileInfo"
                :key="item.label"
                class="flex items-center gap-3 text-sm"
              >
                <span class="material-symbols-outlined text-text-muted-light dark:text-text-muted-dark">{{ item.icon }}</span>
                <div>
                  <p class="text-text-muted-light dark:text-text-muted-dark text-xs">{{ item.label }}</p>
                  <p class="font-semibold">{{ item.value }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Right: Tabbed Content -->
        <div class="lg:col-span-2">
          <div class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark shadow-sm flex flex-col h-full">
            <!-- Tabs -->
            <div class="border-b border-border-light dark:border-border-dark px-6">
              <div class="flex gap-8 overflow-x-auto">
                <button
                  v-for="tab in tabs"
                  :key="tab.key"
                  :class="[
                    'flex flex-col items-center justify-center border-b-[3px] pb-3 pt-4 shrink-0 transition-colors text-sm font-bold leading-normal tracking-wide',
                    activeTab === tab.key
                      ? 'border-primary'
                      : 'border-transparent text-text-muted-light dark:text-text-muted-dark hover:text-primary',
                  ]"
                  @click="activeTab = tab.key"
                >
                  {{ tab.label }}
                </button>
              </div>
            </div>

            <!-- Professional Info Tab -->
            <div v-if="activeTab === 'professional'" class="p-8 flex-1">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-bold">Academic Rank</label>
                  <select
                    v-model="formData.academicRank"
                    class="form-select rounded-lg border-border-light dark:border-border-dark bg-background-light dark:bg-stone-800 text-sm focus:border-primary focus:ring-primary"
                  >
                    <option>Associate Professor</option>
                    <option>Assistant Professor</option>
                    <option>Professor</option>
                    <option>Lecturer</option>
                  </select>
                </div>
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-bold">Specialization</label>
                  <input
                    v-model="formData.specialization"
                    class="form-input rounded-lg border-border-light dark:border-border-dark bg-background-light dark:bg-stone-800 text-sm focus:border-primary focus:ring-primary"
                    type="text"
                  />
                </div>
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-bold">Office Room</label>
                  <input
                    v-model="formData.officeRoom"
                    class="form-input rounded-lg border-border-light dark:border-border-dark bg-background-light dark:bg-stone-800 text-sm focus:border-primary focus:ring-primary"
                    type="text"
                  />
                </div>
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-bold">Work Email</label>
                  <input
                    v-model="formData.workEmail"
                    class="form-input rounded-lg border-border-light dark:border-border-dark bg-background-light dark:bg-stone-800 text-sm focus:border-primary focus:ring-primary"
                    type="email"
                  />
                </div>
                <div class="flex flex-col gap-2 md:col-span-2">
                  <label class="text-sm font-bold">Phone Number</label>
                  <input
                    v-model="formData.phone"
                    class="form-input rounded-lg border-border-light dark:border-border-dark bg-background-light dark:bg-stone-800 text-sm focus:border-primary focus:ring-primary"
                    type="tel"
                  />
                </div>
              </div>

              <!-- Actions -->
              <div class="mt-10 pt-6 border-t border-border-light dark:border-border-dark flex justify-end gap-3">
                <button
                  class="px-6 py-2 rounded-lg bg-background-light dark:bg-stone-800 font-bold text-sm hover:bg-stone-200 dark:hover:bg-stone-700 transition-colors"
                  @click="handleDiscard"
                >
                  Discard
                </button>
                <button
                  class="px-6 py-2 rounded-lg bg-primary text-white font-bold text-sm shadow-md hover:brightness-110 transition-all flex items-center gap-2"
                  @click="handleSave"
                >
                  <span class="material-symbols-outlined text-sm">save</span>
                  Save Changes
                </button>
              </div>
            </div>

            <!-- Teaching Portfolio Tab -->
            <div v-if="activeTab === 'portfolio'" class="p-8 flex-1">
              <div class="text-center py-16 text-text-muted-light dark:text-text-muted-dark">
                <span class="material-symbols-outlined text-5xl mb-4 block">library_books</span>
                <p class="text-lg font-bold">Teaching Portfolio</p>
                <p class="text-sm mt-2">Course history, teaching materials, and publications will be displayed here.</p>
              </div>
            </div>

            <!-- Account Security Tab -->
            <div v-if="activeTab === 'security'" class="p-8 flex-1">
              <div class="text-center py-16 text-text-muted-light dark:text-text-muted-dark">
                <span class="material-symbols-outlined text-5xl mb-4 block">security</span>
                <p class="text-lg font-bold">Account Security</p>
                <p class="text-sm mt-2">Password management and two-factor authentication settings will be displayed here.</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Stats Cards -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div
          v-for="stat in statsCards"
          :key="stat.label"
          class="bg-primary/10 border border-primary/20 p-4 rounded-xl flex items-center gap-4"
        >
          <div class="bg-primary text-white p-2 rounded-lg">
            <span class="material-symbols-outlined">{{ stat.icon }}</span>
          </div>
          <div>
            <p class="text-xs text-text-muted-light dark:text-text-muted-dark font-medium uppercase tracking-wider">{{ stat.label }}</p>
            <p class="text-xl font-bold">{{ stat.value }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
