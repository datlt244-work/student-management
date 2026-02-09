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

// Password change form
const passwordData = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const showCurrentPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)
const passwordError = ref('')
const passwordSuccess = ref('')

const passwordStrength = computed(() => {
  const pw = passwordData.value.newPassword
  if (!pw) return { level: 0, label: '', color: '', percent: '0%' }

  let score = 0
  if (pw.length >= 8) score++
  if (/[a-z]/.test(pw) && /[A-Z]/.test(pw)) score++
  if (/\d/.test(pw)) score++
  if (/[^a-zA-Z0-9]/.test(pw)) score++

  const percents = ['0%', '25%', '50%', '75%', '100%']
  if (score <= 1) return { level: 1, label: 'Weak', color: 'text-red-500', percent: percents[1] }
  if (score === 2) return { level: 2, label: 'Medium', color: 'text-primary', percent: percents[2] }
  if (score === 3) return { level: 3, label: 'Strong', color: 'text-green-600 dark:text-green-400', percent: percents[3] }
  return { level: 4, label: 'Very Strong', color: 'text-green-600 dark:text-green-400', percent: percents[4] }
})

// Security requirements checklist
const securityChecks = computed(() => {
  const pw = passwordData.value.newPassword
  return [
    { label: 'Minimum 8 characters long', met: pw.length >= 8 },
    { label: 'At least one uppercase character', met: /[A-Z]/.test(pw) },
    { label: 'At least one number, symbol or whitespace character', met: /[\d\s\W]/.test(pw) },
  ]
})

function handleSave() {
  // TODO: call API to save profile
  console.log('Save profile:', formData.value)
}

function handleDiscard() {
  // TODO: reset form data to original values
  console.log('Discard changes')
}

function handleUpdatePassword() {
  passwordError.value = ''
  passwordSuccess.value = ''

  if (!passwordData.value.currentPassword) {
    passwordError.value = 'Please enter your current password.'
    return
  }
  if (!passwordData.value.newPassword) {
    passwordError.value = 'Please enter a new password.'
    return
  }
  if (passwordData.value.newPassword.length < 8) {
    passwordError.value = 'New password must be at least 8 characters long.'
    return
  }
  if (passwordData.value.newPassword !== passwordData.value.confirmPassword) {
    passwordError.value = 'New password and confirmation do not match.'
    return
  }

  // TODO: call API to change password
  console.log('Change password')
  passwordSuccess.value = 'Password updated successfully!'
  passwordData.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
}

function handleCancelPassword() {
  passwordData.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
  passwordError.value = ''
  passwordSuccess.value = ''
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
              <div class="space-y-6">
                <!-- Header -->
                <div class="flex flex-col gap-1">
                  <h3 class="text-lg font-bold">Change Password</h3>
                  <p class="text-text-muted-light dark:text-text-muted-dark text-sm">
                    Ensure your account is using a long, random password to stay secure.
                  </p>
                </div>

                <!-- Error / Success Messages -->
                <div
                  v-if="passwordError"
                  class="flex items-center gap-2 p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 text-sm"
                >
                  <span class="material-symbols-outlined text-[18px]">error</span>
                  <span>{{ passwordError }}</span>
                </div>
                <div
                  v-if="passwordSuccess"
                  class="flex items-center gap-2 p-3 rounded-lg bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 text-green-700 dark:text-green-400 text-sm"
                >
                  <span class="material-symbols-outlined text-[18px]">check_circle</span>
                  <span>{{ passwordSuccess }}</span>
                </div>

                <!-- Password Fields -->
                <div class="space-y-5">
                  <!-- Current Password -->
                  <div class="flex flex-col gap-2">
                    <label class="text-sm font-bold">Current Password</label>
                    <div class="relative">
                      <input
                        v-model="passwordData.currentPassword"
                        class="form-input w-full rounded-lg border-border-light dark:border-border-dark bg-background-light dark:bg-stone-800 text-sm focus:border-primary focus:ring-primary pr-10"
                        placeholder="Enter current password"
                        :type="showCurrentPassword ? 'text' : 'password'"
                      />
                      <button
                        class="absolute inset-y-0 right-0 flex items-center pr-3 text-text-muted-light dark:text-text-muted-dark hover:text-primary transition-colors"
                        type="button"
                        @click="showCurrentPassword = !showCurrentPassword"
                      >
                        <span class="material-symbols-outlined text-lg">{{ showCurrentPassword ? 'visibility' : 'visibility_off' }}</span>
                      </button>
                    </div>
                  </div>

                  <!-- New Password -->
                  <div class="flex flex-col gap-2">
                    <label class="text-sm font-bold">New Password</label>
                    <div class="relative">
                      <input
                        v-model="passwordData.newPassword"
                        class="form-input w-full rounded-lg border-border-light dark:border-border-dark bg-background-light dark:bg-stone-800 text-sm focus:border-primary focus:ring-primary pr-10"
                        placeholder="Enter new password"
                        :type="showNewPassword ? 'text' : 'password'"
                      />
                      <button
                        class="absolute inset-y-0 right-0 flex items-center pr-3 text-text-muted-light dark:text-text-muted-dark hover:text-primary transition-colors"
                        type="button"
                        @click="showNewPassword = !showNewPassword"
                      >
                        <span class="material-symbols-outlined text-lg">{{ showNewPassword ? 'visibility' : 'visibility_off' }}</span>
                      </button>
                    </div>
                    <!-- Strength Indicator -->
                    <div v-if="passwordData.newPassword" class="mt-2 space-y-2">
                      <div class="flex justify-between text-xs font-medium">
                        <span class="text-text-muted-light dark:text-text-muted-dark">Password Strength</span>
                        <span :class="['font-bold', passwordStrength.color]">{{ passwordStrength.label }}</span>
                      </div>
                      <div class="h-1.5 w-full bg-border-light dark:bg-border-dark rounded-full overflow-hidden">
                        <div
                          class="h-full bg-primary rounded-full transition-all duration-300"
                          :style="{ width: passwordStrength.percent }"
                        ></div>
                      </div>
                    </div>
                  </div>

                  <!-- Confirm Password -->
                  <div class="flex flex-col gap-2">
                    <label class="text-sm font-bold">Confirm New Password</label>
                    <div class="relative">
                      <input
                        v-model="passwordData.confirmPassword"
                        class="form-input w-full rounded-lg border-border-light dark:border-border-dark bg-background-light dark:bg-stone-800 text-sm focus:border-primary focus:ring-primary pr-10"
                        placeholder="Confirm new password"
                        :type="showConfirmPassword ? 'text' : 'password'"
                      />
                      <button
                        class="absolute inset-y-0 right-0 flex items-center pr-3 text-text-muted-light dark:text-text-muted-dark hover:text-primary transition-colors"
                        type="button"
                        @click="showConfirmPassword = !showConfirmPassword"
                      >
                        <span class="material-symbols-outlined text-lg">{{ showConfirmPassword ? 'visibility' : 'visibility_off' }}</span>
                      </button>
                    </div>
                    <!-- Mismatch warning -->
                    <p
                      v-if="passwordData.confirmPassword && passwordData.newPassword !== passwordData.confirmPassword"
                      class="text-xs text-red-500 mt-1 flex items-center gap-1"
                    >
                      <span class="material-symbols-outlined text-[14px]">error</span>
                      Passwords do not match.
                    </p>
                  </div>
                </div>

                <!-- Security Requirements Checklist -->
                <div class="bg-background-light dark:bg-stone-800/50 p-4 rounded-lg border border-border-light dark:border-border-dark">
                  <h4 class="text-sm font-bold mb-3">Security Requirements</h4>
                  <ul class="space-y-2">
                    <li
                      v-for="check in securityChecks"
                      :key="check.label"
                      class="flex items-center gap-2 text-xs text-text-muted-light dark:text-text-muted-dark"
                    >
                      <span
                        :class="[
                          'material-symbols-outlined text-sm',
                          check.met ? 'text-green-500' : 'text-text-muted-light dark:text-text-muted-dark',
                        ]"
                      >
                        {{ check.met ? 'check_circle' : 'radio_button_unchecked' }}
                      </span>
                      <span>{{ check.label }}</span>
                    </li>
                  </ul>
                </div>
              </div>

              <!-- Actions -->
              <div class="mt-10 pt-6 border-t border-border-light dark:border-border-dark flex justify-end gap-3">
                <button
                  class="px-6 py-2 rounded-lg bg-background-light dark:bg-stone-800 font-bold text-sm hover:bg-stone-200 dark:hover:bg-stone-700 transition-colors"
                  @click="handleCancelPassword"
                >
                  Cancel
                </button>
                <button
                  class="px-6 py-2 rounded-lg bg-primary text-white font-bold text-sm shadow-md hover:brightness-110 transition-all flex items-center gap-2"
                  @click="handleUpdatePassword"
                >
                  <span class="material-symbols-outlined text-sm">lock_reset</span>
                  Update Password
                </button>
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
