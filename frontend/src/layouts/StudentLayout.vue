<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { apiFetch } from '@/utils/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const user = computed(() => authStore.user)

const navItems = [
  { name: 'Dashboard', routeName: 'student-dashboard' },
  { name: 'Courses', routeName: 'student-courses' },
  { name: 'Grades', routeName: 'student-grades' },
  { name: 'Schedule', routeName: 'student-schedule' },
  { name: 'Profile', routeName: 'student-profile' },
]

function isActive(routeName: string): boolean {
  return route.name === routeName
}

async function handleLogout() {
  try {
    await apiFetch('/auth/logout', {
      method: 'POST',
      body: JSON.stringify({
        refreshToken: authStore.refreshToken,
      }),
    })
  } catch {
    // Logout from server failed, still clear local state
  }

  authStore.clearAuth()
  router.push({ name: 'login' })
}

function getInitials(email: string | undefined): string {
  if (!email) return 'ST'
  return email.substring(0, 2).toUpperCase()
}
</script>

<template>
  <div class="bg-background-light dark:bg-background-dark text-text-main-light dark:text-text-main-dark font-display min-h-screen flex flex-col antialiased">
    <!-- Top Navigation -->
    <header class="flex items-center justify-between whitespace-nowrap border-b border-border-light dark:border-border-dark px-6 lg:px-10 py-4 bg-surface-light dark:bg-surface-dark shrink-0">
      <div class="flex items-center gap-8">
        <div class="flex items-center gap-4">
          <div class="size-8 text-primary">
            <span class="material-symbols-outlined text-4xl">school</span>
          </div>
          <h2 class="text-lg font-bold leading-tight tracking-tight">Student Portal</h2>
        </div>
        <nav class="hidden md:flex items-center gap-9">
          <router-link
            v-for="item in navItems"
            :key="item.routeName"
            :to="{ name: item.routeName }"
            :class="[
              'text-sm font-medium leading-normal transition-colors',
              isActive(item.routeName)
                ? 'text-primary'
                : 'text-text-main-light dark:text-stone-200 hover:text-primary',
            ]"
          >
            {{ item.name }}
          </router-link>
        </nav>
      </div>
      <div class="flex flex-1 justify-end gap-6 items-center">
        <!-- Search -->
        <label class="hidden sm:flex flex-col min-w-40 h-10! max-w-64">
          <div class="flex w-full flex-1 items-stretch rounded-lg h-full border border-border-light dark:border-border-dark overflow-hidden">
            <div class="text-text-muted-light dark:text-text-muted-dark flex border-none bg-background-light dark:bg-stone-800 items-center justify-center pl-3">
              <span class="material-symbols-outlined text-[20px]">search</span>
            </div>
            <input
              class="form-input flex w-full min-w-0 flex-1 resize-none overflow-hidden focus:outline-0 focus:ring-0 border-none bg-background-light dark:bg-stone-800 focus:border-none h-full placeholder:text-text-muted-light dark:placeholder:text-text-muted-dark px-2 text-sm font-normal leading-normal"
              placeholder="Search..."
              type="text"
            />
          </div>
        </label>

        <!-- Notifications -->
        <button class="flex items-center justify-center rounded-lg size-10 bg-background-light dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 transition-colors relative">
          <span class="material-symbols-outlined text-[24px]">notifications</span>
          <span class="absolute top-2 right-2 size-2 bg-red-500 rounded-full border border-surface-light dark:border-surface-dark"></span>
        </button>

        <!-- Profile dropdown -->
        <div class="relative flex items-center gap-3">
          <div
            v-if="user?.profilePictureUrl"
            class="bg-center bg-no-repeat aspect-square bg-cover rounded-full size-10 border-2 border-border-light dark:border-border-dark cursor-pointer"
            :style="{ backgroundImage: `url(${user.profilePictureUrl})` }"
            @click="handleLogout"
          ></div>
          <div
            v-else
            class="size-10 rounded-full border-2 border-border-light dark:border-border-dark bg-primary/10 flex items-center justify-center text-primary font-bold text-sm cursor-pointer"
            @click="handleLogout"
            title="Click to sign out"
          >
            {{ getInitials(user?.email) }}
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 flex flex-col overflow-y-auto">
      <router-view />
    </main>
  </div>
</template>
