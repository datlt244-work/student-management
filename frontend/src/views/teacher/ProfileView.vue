<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getMyProfile, updateMyProfile, uploadAvatar, type CombinedProfile } from '@/services/profileService'
import { changePassword } from '@/services/authService'

const authStore = useAuthStore()
const user = computed(() => authStore.user)

// Profile data from API
const profile = ref<CombinedProfile | null>(null)
const isLoading = ref(false)
const loadError = ref('')

const displayName = computed(() => {
  if (profile.value?.teacherProfile) {
    return `${profile.value.teacherProfile.firstName} ${profile.value.teacherProfile.lastName}`
  }
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

// Form data — populated from API
const formData = ref({
  academicRank: 'Lecturer',
  specialization: '',
  officeRoom: '',
  workEmail: '',
  phone: '',
})

// Profile info (computed from API data)
const profileInfo = computed(() => [
  { icon: 'badge', label: 'Teacher Code', value: profile.value?.teacherProfile?.teacherCode ?? 'N/A' },
  { icon: 'account_balance', label: 'Department', value: profile.value?.teacherProfile?.department?.name ?? 'N/A' },
  { icon: 'event_available', label: 'Joined Date', value: profile.value?.createdAt ? new Date(profile.value.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) : 'N/A' },
])

// Stats cards (computed from API data)
const statsCards = computed(() => [
  { icon: 'military_tech', label: 'Academic Rank', value: profile.value?.teacherProfile?.academicRank ?? 'N/A' },
  { icon: 'meeting_room', label: 'Office Room', value: profile.value?.teacherProfile?.officeRoom ?? 'N/A' },
  { icon: 'verified_user', label: 'Status', value: profile.value?.status ?? 'N/A' },
])

// Fetch profile on mount
async function fetchProfile() {
  isLoading.value = true
  loadError.value = ''
  try {
    profile.value = await getMyProfile()
    // Populate form data from profile
    const tp = profile.value.teacherProfile
    if (tp) {
      formData.value.academicRank = tp.academicRank ?? 'Lecturer'
      formData.value.specialization = tp.specialization ?? ''
      formData.value.officeRoom = tp.officeRoom ?? ''
      formData.value.workEmail = profile.value.email
      formData.value.phone = tp.phone ?? ''
    }
  } catch (err: unknown) {
    loadError.value = err instanceof Error ? err.message : 'Failed to load profile'
  } finally {
    isLoading.value = false
  }
}

onMounted(fetchProfile)

// Avatar upload
const avatarInput = ref<HTMLInputElement | null>(null)
const isUploadingAvatar = ref(false)

function triggerAvatarUpload() {
  avatarInput.value?.click()
}

async function handleAvatarChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  isUploadingAvatar.value = true
  try {
    const result = await uploadAvatar(file)
    if (profile.value) {
      profile.value.profilePictureUrl = result.fullUrl
    }
    if (authStore.user) {
      const rememberMe = localStorage.getItem('rememberMe') === 'true'
      authStore.setAuth({
        accessToken: authStore.accessToken!,
        refreshToken: authStore.refreshToken!,
        userId: authStore.user.userId,
        email: authStore.user.email,
        role: authStore.user.role,
        profilePictureUrl: result.fullUrl,
      }, rememberMe)
    }
  } catch (err: unknown) {
    alert(err instanceof Error ? err.message : 'Failed to upload avatar')
  } finally {
    isUploadingAvatar.value = false
    target.value = ''
  }
}

// Profile update
const isSaving = ref(false)
const saveError = ref('')
const saveSuccess = ref('')

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
const isChangingPassword = ref(false)

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

async function handleSave() {
  isSaving.value = true
  saveError.value = ''
  saveSuccess.value = ''
  try {
    const updated = await updateMyProfile({
      teacherProfile: {
        phone: formData.value.phone,
      },
    })
    profile.value = updated
    saveSuccess.value = 'Profile updated successfully!'
    setTimeout(() => { saveSuccess.value = '' }, 3000)
  } catch (err: unknown) {
    saveError.value = err instanceof Error ? err.message : 'Failed to update profile'
  } finally {
    isSaving.value = false
  }
}

function handleDiscard() {
  // Reset form data to API values
  const tp = profile.value?.teacherProfile
  if (tp) {
    formData.value.academicRank = tp.academicRank ?? 'Lecturer'
    formData.value.specialization = tp.specialization ?? ''
    formData.value.officeRoom = tp.officeRoom ?? ''
    formData.value.workEmail = profile.value?.email ?? ''
    formData.value.phone = tp.phone ?? ''
  }
}

async function handleUpdatePassword() {
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

  isChangingPassword.value = true
  try {
    const result = await changePassword({
      currentPassword: passwordData.value.currentPassword,
      newPassword: passwordData.value.newPassword,
      confirmPassword: passwordData.value.confirmPassword,
      logoutOtherDevices: true,
    })
    passwordSuccess.value = result.message || 'Password changed successfully. Please login again.'
    passwordData.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
    // Sau khi đổi mật khẩu, bắt buộc user đăng nhập lại
    setTimeout(() => {
      authStore.clearAuth()
      window.location.href = '/login'
    }, 1500)
  } catch (err: unknown) {
    passwordError.value = err instanceof Error ? err.message : 'Failed to change password'
  } finally {
    isChangingPassword.value = false
  }
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

      <!-- Loading State -->
      <div v-if="isLoading" class="flex items-center justify-center py-20">
        <div class="flex flex-col items-center gap-4">
          <div class="animate-spin rounded-full h-12 w-12 border-4 border-primary border-t-transparent"></div>
          <p class="text-text-muted-light dark:text-text-muted-dark text-sm">Loading profile...</p>
        </div>
      </div>

      <!-- Error State -->
      <div v-else-if="loadError" class="flex items-center justify-center py-20">
        <div class="text-center">
          <span class="material-symbols-outlined text-5xl text-red-500 mb-4 block">error</span>
          <p class="text-lg font-bold text-red-500">Failed to load profile</p>
          <p class="text-sm text-text-muted-light dark:text-text-muted-dark mt-2">{{ loadError }}</p>
          <button
            class="mt-4 px-6 py-2 rounded-lg bg-primary text-white font-bold text-sm hover:brightness-110 transition-all"
            @click="fetchProfile"
          >
            Retry
          </button>
        </div>
      </div>

      <template v-else>
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Left: Profile Card -->
        <div class="lg:col-span-1">
          <div class="bg-surface-light dark:bg-surface-dark rounded-xl border border-border-light dark:border-border-dark p-6 text-center shadow-sm">
            <!-- Avatar -->
            <div class="relative inline-block mb-4">
              <div
                v-if="profile?.profilePictureUrl"
                class="bg-center bg-no-repeat aspect-square bg-cover rounded-xl size-32 mx-auto ring-4 ring-primary/20"
                :style="{ backgroundImage: `url(${profile.profilePictureUrl})` }"
              ></div>
              <div
                v-else
                class="rounded-xl size-32 mx-auto ring-4 ring-primary/20 bg-primary/10 flex items-center justify-center text-primary font-bold text-3xl"
              >
                {{ displayName.substring(0, 2).toUpperCase() }}
              </div>
              <div
                class="absolute -bottom-2 -right-2 bg-primary text-white rounded-full p-2 shadow-lg cursor-pointer hover:brightness-110 transition-all"
                @click="triggerAvatarUpload"
              >
                <span v-if="isUploadingAvatar" class="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent block"></span>
                <span v-else class="material-symbols-outlined text-sm">edit</span>
              </div>
              <input ref="avatarInput" type="file" accept="image/jpeg,image/png,image/webp" class="hidden" @change="handleAvatarChange" />
            </div>

            <!-- Name & Title -->
            <h3 class="text-xl font-bold">{{ displayName }}</h3>
            <p class="text-primary font-medium text-sm">{{ profile?.teacherProfile?.academicRank ?? 'Teacher' }}</p>

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
                    class="form-select rounded-lg border-border-light dark:border-border-dark bg-stone-50 dark:bg-stone-800 text-sm h-10 cursor-not-allowed"
                    disabled
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
                    class="form-input rounded-lg border-border-light dark:border-border-dark bg-stone-50 dark:bg-stone-800 text-sm h-10 cursor-not-allowed"
                    type="text"
                    disabled
                  />
                </div>
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-bold">Office Room</label>
                  <input
                    v-model="formData.officeRoom"
                    class="form-input rounded-lg border-border-light dark:border-border-dark bg-stone-50 dark:bg-stone-800 text-sm h-10 cursor-not-allowed"
                    type="text"
                    disabled
                  />
                </div>
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-bold">Work Email</label>
                  <input
                    v-model="formData.workEmail"
                    class="form-input rounded-lg border-border-light dark:border-border-dark bg-stone-50 dark:bg-stone-800 text-sm h-10 cursor-not-allowed"
                    type="email"
                    disabled
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

              <!-- Save feedback -->
              <div
                v-if="saveError"
                class="mt-6 flex items-center gap-2 p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 text-sm"
              >
                <span class="material-symbols-outlined text-[18px]">error</span>
                <span>{{ saveError }}</span>
              </div>
              <div
                v-if="saveSuccess"
                class="mt-6 flex items-center gap-2 p-3 rounded-lg bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 text-green-700 dark:text-green-400 text-sm"
              >
                <span class="material-symbols-outlined text-[18px]">check_circle</span>
                <span>{{ saveSuccess }}</span>
              </div>

              <!-- Actions -->
              <div class="mt-6 pt-6 border-t border-border-light dark:border-border-dark flex justify-end gap-3">
                <button
                  class="px-6 py-2 rounded-lg bg-background-light dark:bg-stone-800 font-bold text-sm hover:bg-stone-200 dark:hover:bg-stone-700 transition-colors"
                  :disabled="isSaving"
                  @click="handleDiscard"
                >
                  Discard
                </button>
                <button
                  class="px-6 py-2 rounded-lg bg-primary text-white font-bold text-sm shadow-md hover:brightness-110 transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                  :disabled="isSaving"
                  @click="handleSave"
                >
                  <span v-if="isSaving" class="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent"></span>
                  <span v-else class="material-symbols-outlined text-sm">save</span>
                  {{ isSaving ? 'Saving...' : 'Save Changes' }}
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
                  class="px-6 py-2 rounded-lg bg-primary text-white font-bold text-sm shadow-md hover:brightness-110 transition-all flex items-center gap-2 disabled:opacity-60 disabled:pointer-events-none"
                  :disabled="isChangingPassword"
                  @click="handleUpdatePassword"
                >
                  <span v-if="isChangingPassword" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
                  <span v-else class="material-symbols-outlined text-sm">lock_reset</span>
                  {{ isChangingPassword ? 'Updating...' : 'Update Password' }}
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
      </template>
    </div>
  </div>
</template>
