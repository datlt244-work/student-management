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
  if (profile.value?.studentProfile) {
    return `${profile.value.studentProfile.firstName} ${profile.value.studentProfile.lastName}`
  }
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

// Form data — populated from API
const formData = ref({
  fullName: '',
  dateOfBirth: '',
  gender: '',
  email: '',
  phone: '',
  address: '',
  major: '',
})

// Summary info
const summaryCards = computed(() => [
  { label: 'Current Semester', value: profile.value?.studentProfile?.currentSemester?.displayName ?? 'N/A', isHighlight: false },
  { label: 'Department', value: profile.value?.studentProfile?.department?.name ?? 'N/A', isHighlight: false },
  { label: 'GPA', value: profile.value?.studentProfile?.gpa != null ? `${profile.value.studentProfile.gpa} / 4.0` : 'N/A', isHighlight: true },
])

// Fetch profile on mount
async function fetchProfile() {
  isLoading.value = true
  loadError.value = ''
  try {
    profile.value = await getMyProfile()
    // Populate form data from profile
    const sp = profile.value.studentProfile
    if (sp) {
      formData.value.fullName = `${sp.firstName} ${sp.lastName}`
      formData.value.dateOfBirth = sp.dob ?? ''
      formData.value.gender = sp.gender ?? ''
      formData.value.email = profile.value.email
      formData.value.phone = sp.phone ?? ''
      formData.value.address = sp.address ?? ''
      formData.value.major = sp.major ?? ''
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
    // Update profile picture URL locally
    if (profile.value) {
      profile.value.profilePictureUrl = result.fullUrl
    }
    // Update auth store so avatar updates in layout/navbar too
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
    target.value = '' // Reset file input
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
  if (!pw) return { level: 0, label: '', color: '' }

  let score = 0
  if (pw.length >= 8) score++
  if (/[a-z]/.test(pw) && /[A-Z]/.test(pw)) score++
  if (/\d/.test(pw)) score++
  if (/[^a-zA-Z0-9]/.test(pw)) score++

  if (score <= 1) return { level: 1, label: 'Weak', color: 'text-red-500' }
  if (score === 2) return { level: 2, label: 'Medium', color: 'text-primary' }
  if (score === 3) return { level: 3, label: 'Strong', color: 'text-green-600 dark:text-green-400' }
  return { level: 4, label: 'Very Strong', color: 'text-green-600 dark:text-green-400' }
})

async function handleUpdateProfile() {
  isSaving.value = true
  saveError.value = ''
  saveSuccess.value = ''
  try {
    const updated = await updateMyProfile({
      studentProfile: {
        phone: formData.value.phone,
        address: formData.value.address,
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

      <!-- Profile Layout -->
      <div v-else class="flex flex-col lg:flex-row gap-8 items-start">
        <!-- Sidebar (Profile Summary) -->
        <aside class="w-full lg:w-1/3 flex flex-col gap-6">
          <div class="bg-surface-light dark:bg-surface-dark p-8 rounded-xl shadow-sm border border-border-light dark:border-border-dark flex flex-col items-center">
            <!-- Avatar -->
            <div class="relative group">
              <div
                v-if="profile?.profilePictureUrl"
                class="bg-center bg-no-repeat aspect-square bg-cover rounded-full size-40 border-4 border-primary/20"
                :style="{ backgroundImage: `url(${profile.profilePictureUrl})` }"
              ></div>
              <div
                v-else
                class="rounded-full size-40 border-4 border-primary/20 bg-primary/10 flex items-center justify-center text-primary font-bold text-4xl"
              >
                {{ displayName.substring(0, 2).toUpperCase() }}
              </div>
              <div
                class="absolute bottom-2 right-2 bg-primary text-white p-1.5 rounded-full shadow-lg cursor-pointer hover:brightness-110 transition-all"
                @click="triggerAvatarUpload"
              >
                <span v-if="isUploadingAvatar" class="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent block"></span>
                <span v-else class="material-symbols-outlined text-base">photo_camera</span>
              </div>
              <input ref="avatarInput" type="file" accept="image/jpeg,image/png,image/webp" class="hidden" @change="handleAvatarChange" />
            </div>

            <!-- Name & Info -->
            <div class="mt-6 text-center">
              <h1 class="text-2xl font-bold leading-tight">{{ displayName }}</h1>
              <p class="text-primary font-medium mt-1">{{ profile?.studentProfile?.major ?? profile?.studentProfile?.department?.name ?? 'No Department' }}</p>
              <p class="text-text-muted-light dark:text-text-muted-dark text-sm font-normal mt-1">Student ID: {{ profile?.studentProfile?.studentCode ?? 'N/A' }}</p>
            </div>

            <!-- Edit Photo Button -->
            <button
              class="mt-8 flex w-full items-center justify-center gap-2 rounded-lg h-12 bg-primary/10 text-primary hover:bg-primary hover:text-white transition-all font-bold text-sm tracking-wide disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="isUploadingAvatar"
              @click="triggerAvatarUpload"
            >
              <span v-if="isUploadingAvatar" class="animate-spin rounded-full h-4 w-4 border-2 border-primary border-t-transparent"></span>
              <span v-else class="material-symbols-outlined text-sm">edit</span>
              <span>{{ isUploadingAvatar ? 'Uploading...' : 'Edit Photo' }}</span>
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
                  disabled
                />
              </div>
              <div class="flex flex-col gap-2">
                <label class="text-sm font-bold">Student Code</label>
                <input
                  :value="profile?.studentProfile?.studentCode ?? ''"
                  class="form-input rounded-lg border-border-light dark:border-border-dark bg-stone-50 dark:bg-stone-800 text-sm h-12 cursor-not-allowed"
                  type="text"
                  disabled
                />
              </div>
              <div class="flex flex-col gap-2">
                <label class="text-sm font-bold">Date of Birth</label>
                <input
                  v-model="formData.dateOfBirth"
                  class="form-input rounded-lg border-border-light dark:border-border-dark bg-transparent text-sm focus:border-primary focus:ring-primary h-12"
                  type="date"
                  disabled
                />
              </div>
              <div class="flex flex-col gap-2">
                <label class="text-sm font-bold">Gender</label>
                <select
                  v-model="formData.gender"
                  class="form-select rounded-lg border-border-light dark:border-border-dark bg-stone-50 dark:bg-stone-800 text-sm h-12 cursor-not-allowed"
                  disabled
                >
                  <option value="" disabled>Select gender</option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div class="flex flex-col gap-2">
                <label class="text-sm font-bold">Major</label>
                <input
                  v-model="formData.major"
                  class="form-input rounded-lg border-border-light dark:border-border-dark bg-stone-50 dark:bg-stone-800 text-sm h-12 cursor-not-allowed"
                  type="text"
                  disabled
                />
              </div>
              <div class="flex flex-col gap-2">
                <label class="text-sm font-bold">Email Address</label>
                <input
                  v-model="formData.email"
                  class="form-input rounded-lg border-border-light dark:border-border-dark bg-stone-50 dark:bg-stone-800 text-sm h-12 cursor-not-allowed"
                  type="email"
                  disabled
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

            <!-- Profile Security & Save -->
            <div class="mt-6 flex flex-col sm:flex-row items-center justify-between gap-6 p-6 rounded-xl bg-stone-50/50 dark:bg-stone-800/30 border border-border-light dark:border-border-dark">
              <div class="flex gap-4 items-center">
                <div class="flex size-12 items-center justify-center rounded-full bg-primary/20 text-primary">
                  <span class="material-symbols-outlined">verified_user</span>
                </div>
                <div>
                  <p class="text-sm font-bold">Profile Security</p>
                  <p class="text-xs text-text-muted-light dark:text-text-muted-dark">Only phone and address can be updated.</p>
                </div>
              </div>
              <button
                class="w-full sm:w-auto min-w-[180px] bg-primary text-white py-3 px-8 rounded-lg font-bold text-sm shadow-md shadow-primary/20 hover:brightness-110 transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                :disabled="isSaving"
                @click="handleUpdateProfile"
              >
                <span v-if="isSaving" class="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent"></span>
                <span v-else class="material-symbols-outlined text-sm">save</span>
                {{ isSaving ? 'Saving...' : 'Update Profile' }}
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
            <div class="flex flex-col gap-8 max-w-2xl">
              <!-- Header -->
              <div>
                <h3 class="text-lg font-bold mb-1">Change Password</h3>
                <p class="text-sm text-text-muted-light dark:text-text-muted-dark">
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
              <div class="space-y-6">
                <!-- Current Password -->
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-bold">Current Password</label>
                  <div class="relative">
                    <input
                      v-model="passwordData.currentPassword"
                      class="form-input w-full rounded-lg border-primary/50 dark:border-primary/50 focus:border-primary focus:ring-primary bg-transparent text-sm h-12 pr-10"
                      placeholder="••••••••"
                      :type="showCurrentPassword ? 'text' : 'password'"
                    />
                    <button
                      class="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark hover:text-primary transition-colors"
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
                      class="form-input w-full rounded-lg border-primary/50 dark:border-primary/50 focus:border-primary focus:ring-primary bg-transparent text-sm h-12 pr-10"
                      placeholder="Enter new password"
                      :type="showNewPassword ? 'text' : 'password'"
                    />
                    <button
                      class="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark hover:text-primary transition-colors"
                      type="button"
                      @click="showNewPassword = !showNewPassword"
                    >
                      <span class="material-symbols-outlined text-lg">{{ showNewPassword ? 'visibility' : 'visibility_off' }}</span>
                    </button>
                  </div>
                  <!-- Strength Indicator -->
                  <div v-if="passwordData.newPassword" class="mt-2">
                    <div class="flex justify-between items-center mb-1">
                      <span class="text-xs font-medium text-text-muted-light dark:text-text-muted-dark">Password strength:</span>
                      <span :class="['text-xs font-bold', passwordStrength.color]">{{ passwordStrength.label }}</span>
                    </div>
                    <div class="flex gap-1 h-1.5 w-full">
                      <div
                        v-for="i in 4"
                        :key="i"
                        :class="[
                          'flex-1 rounded-full transition-colors',
                          i <= passwordStrength.level ? 'bg-primary' : 'bg-border-light dark:bg-border-dark',
                        ]"
                      ></div>
                    </div>
                    <p class="text-xs text-text-muted-light dark:text-text-muted-dark mt-2">
                      Use at least 8 characters with a mix of letters, numbers &amp; symbols.
                    </p>
                  </div>
                </div>

                <!-- Confirm Password -->
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-bold">Confirm New Password</label>
                  <div class="relative">
                    <input
                      v-model="passwordData.confirmPassword"
                      class="form-input w-full rounded-lg border-primary/50 dark:border-primary/50 focus:border-primary focus:ring-primary bg-transparent text-sm h-12 pr-10"
                      placeholder="Confirm new password"
                      :type="showConfirmPassword ? 'text' : 'password'"
                    />
                    <button
                      class="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark hover:text-primary transition-colors"
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

              <!-- Secure Action Bar -->
              <div class="mt-4 flex flex-col sm:flex-row items-center justify-between gap-6 p-6 rounded-xl bg-stone-50/50 dark:bg-stone-800/30 border border-border-light dark:border-border-dark">
                <div class="flex gap-4 items-center">
                  <div class="flex size-12 items-center justify-center rounded-full bg-primary/20 text-primary">
                    <span class="material-symbols-outlined">lock_reset</span>
                  </div>
                  <div>
                    <p class="text-sm font-bold">Secure Action</p>
                    <p class="text-xs text-text-muted-light dark:text-text-muted-dark">You will be logged out of other sessions.</p>
                  </div>
                </div>
                <button
                  class="w-full sm:w-auto min-w-[180px] bg-primary text-white py-3 px-8 rounded-lg font-bold text-sm shadow-md shadow-primary/20 hover:brightness-110 transition-all flex items-center justify-center gap-2 disabled:opacity-60 disabled:pointer-events-none"
                  :disabled="isChangingPassword"
                  @click="handleUpdatePassword"
                >
                  <span v-if="isChangingPassword" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
                  <span v-else class="material-symbols-outlined text-sm">check_circle</span>
                  {{ isChangingPassword ? 'Updating...' : 'Update Password' }}
                </button>
              </div>
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
