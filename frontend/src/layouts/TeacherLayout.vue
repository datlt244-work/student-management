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
  { name: 'Dashboard', icon: 'dashboard', routeName: 'teacher-dashboard' },
  { name: 'Students', icon: 'groups', routeName: 'teacher-students' },
  { name: 'Classes', icon: 'menu_book', routeName: 'teacher-classes' },
  { name: 'Grades', icon: 'grade', routeName: 'teacher-grades' },
  { name: 'Schedule', icon: 'calendar_today', routeName: 'teacher-schedule' },
  { name: 'Profile', icon: 'person', routeName: 'teacher-profile' },
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
  if (!email) return 'TC'
  return email.substring(0, 2).toUpperCase()
}
</script>

<template>
  <div class="relative flex min-h-screen w-full flex-col overflow-x-hidden bg-background-light dark:bg-background-dark text-text-main-light dark:text-text-main-dark font-display antialiased">
    <!-- Header / Top Navigation -->
    <header class="flex items-center justify-between border-b border-border-light dark:border-border-dark bg-surface-light dark:bg-surface-dark px-6 py-3 lg:px-10 shrink-0">
      <div class="flex items-center gap-8">
        <div class="flex items-center gap-3">
          <div class="size-8 flex items-center justify-center bg-primary rounded-lg text-white">
            <span class="material-symbols-outlined">school</span>
          </div>
          <h2 class="text-lg font-bold leading-tight tracking-tight hidden md:block">EduPortal</h2>
        </div>
        <div class="hidden sm:flex items-center bg-stone-100 dark:bg-stone-800 rounded-lg px-3 py-1.5 w-64 lg:w-80">
          <span class="material-symbols-outlined text-text-muted-light dark:text-text-muted-dark text-[20px]">search</span>
          <input
            class="bg-transparent border-none focus:ring-0 text-sm w-full placeholder:text-text-muted-light dark:placeholder:text-text-muted-dark"
            placeholder="Search students, classes..."
            type="text"
          />
        </div>
      </div>
      <div class="flex items-center gap-4">
        <button class="relative p-2 text-text-muted-light dark:text-text-muted-dark hover:bg-stone-100 dark:hover:bg-stone-800 rounded-lg transition-colors">
          <span class="material-symbols-outlined">notifications</span>
          <span class="absolute top-1.5 right-1.5 size-2 bg-red-500 rounded-full border-2 border-surface-light dark:border-surface-dark"></span>
        </button>
        <button class="p-2 text-text-muted-light dark:text-text-muted-dark hover:bg-stone-100 dark:hover:bg-stone-800 rounded-lg transition-colors">
          <span class="material-symbols-outlined">help</span>
        </button>
        <div class="flex items-center gap-3 ml-2 pl-4 border-l border-border-light dark:border-border-dark">
          <div class="text-right hidden lg:block">
            <p class="text-xs font-bold">{{ user?.email || 'Teacher' }}</p>
            <p class="text-[10px] text-text-muted-light dark:text-text-muted-dark">Educator</p>
          </div>
          <div
            v-if="user?.profilePictureUrl"
            class="size-10 rounded-full bg-cover bg-center ring-2 ring-primary/20"
            :style="{ backgroundImage: `url(${user.profilePictureUrl})` }"
          ></div>
          <div
            v-else
            class="size-10 rounded-full ring-2 ring-primary/20 bg-primary/10 flex items-center justify-center text-primary font-bold text-sm"
          >
            {{ getInitials(user?.email) }}
          </div>
        </div>
      </div>
    </header>

    <div class="flex flex-1 overflow-hidden">
      <!-- Sidebar Navigation -->
      <aside class="hidden md:flex flex-col w-64 bg-surface-light dark:bg-surface-dark border-r border-border-light dark:border-border-dark p-4 gap-2 shrink-0">
        <div class="mb-4 px-3 py-2">
          <p class="text-[10px] font-bold uppercase tracking-widest text-text-muted-light dark:text-text-muted-dark">Main Menu</p>
        </div>
        <nav class="flex flex-col gap-1">
          <router-link
            v-for="item in navItems"
            :key="item.routeName"
            :to="{ name: item.routeName }"
            :class="[
              'flex items-center gap-3 px-3 py-2 rounded-lg transition-colors font-medium',
              isActive(item.routeName)
                ? 'bg-primary text-white'
                : 'text-text-muted-light dark:text-text-muted-dark hover:bg-stone-100 dark:hover:bg-stone-800',
            ]"
          >
            <span class="material-symbols-outlined">{{ item.icon }}</span>
            <span>{{ item.name }}</span>
          </router-link>
        </nav>
        <div class="mt-auto flex flex-col gap-1">
          <a class="flex items-center gap-3 px-3 py-2 rounded-lg text-text-muted-light dark:text-text-muted-dark hover:bg-stone-100 dark:hover:bg-stone-800 transition-colors cursor-pointer" href="#">
            <span class="material-symbols-outlined">settings</span>
            <span>Settings</span>
          </a>
          <button
            class="flex items-center gap-3 px-3 py-2 rounded-lg text-red-500 hover:bg-red-50 dark:hover:bg-red-900/10 transition-colors w-full"
            @click="handleLogout"
          >
            <span class="material-symbols-outlined">logout</span>
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      <!-- Main Content Area -->
      <main class="flex-1 overflow-y-auto">
        <router-view />
      </main>
    </div>
  </div>
</template>
