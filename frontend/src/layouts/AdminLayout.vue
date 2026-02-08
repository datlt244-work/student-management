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
  { name: 'Dashboard', icon: 'dashboard', routeName: 'admin-dashboard' },
  { name: 'User Management', icon: 'group', routeName: 'admin-users' },
  { name: 'Course Management', icon: 'menu_book', routeName: 'admin-courses' },
  { name: 'Class Management', icon: 'school', routeName: 'admin-classes' },
  { name: 'System Logs', icon: 'description', routeName: 'admin-logs' },
]

function isActive(routeName: string): boolean {
  return route.name === routeName
}

async function handleLogout() {
  try {
    await apiFetch('/auth/logout', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${authStore.accessToken}`,
      },
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
  if (!email) return 'AD'
  return email.substring(0, 2).toUpperCase()
}
</script>

<template>
  <div class="flex h-screen w-full bg-background-light dark:bg-background-dark text-slate-900 dark:text-slate-100 font-display antialiased overflow-hidden selection:bg-primary selection:text-white">
    <!-- Side Navigation -->
    <aside class="w-72 h-full flex flex-col border-r border-stone-200 dark:border-stone-800 bg-surface-light dark:bg-surface-dark transition-colors duration-300 flex-shrink-0">
      <div class="p-6 flex flex-col h-full justify-between">
        <!-- Header / User Info -->
        <div class="flex flex-col gap-8">
          <div class="flex items-center gap-3">
            <div
              v-if="user?.profilePictureUrl"
              class="bg-center bg-no-repeat bg-cover rounded-full h-12 w-12 border-2 border-primary"
              :style="{ backgroundImage: `url(${user.profilePictureUrl})` }"
            ></div>
            <div
              v-else
              class="rounded-full h-12 w-12 border-2 border-primary bg-primary/10 flex items-center justify-center text-primary font-bold text-sm"
            >
              {{ getInitials(user?.email) }}
            </div>
            <div class="flex flex-col">
              <h1 class="text-slate-900 dark:text-white text-base font-bold leading-tight">Admin Portal</h1>
              <p class="text-primary text-sm font-normal truncate max-w-[170px]">{{ user?.email || 'Administrator' }}</p>
            </div>
          </div>

          <!-- Navigation Links -->
          <nav class="flex flex-col gap-2">
            <router-link
              v-for="item in navItems"
              :key="item.routeName"
              :to="{ name: item.routeName }"
              :class="[
                'flex items-center gap-3 px-4 py-3 rounded-lg transition-all group',
                isActive(item.routeName)
                  ? 'bg-primary/10 border-l-4 border-primary'
                  : 'hover:bg-stone-100 dark:hover:bg-stone-800 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white',
              ]"
            >
              <span
                :class="[
                  'material-symbols-outlined group-hover:scale-110 transition-transform',
                  isActive(item.routeName) ? 'text-primary' : '',
                ]"
              >
                {{ item.icon }}
              </span>
              <p
                :class="[
                  'text-sm',
                  isActive(item.routeName)
                    ? 'text-slate-900 dark:text-white font-semibold'
                    : 'font-medium',
                ]"
              >
                {{ item.name }}
              </p>
            </router-link>
          </nav>
        </div>

        <!-- Logout -->
        <button
          class="flex w-full items-center justify-center gap-2 rounded-lg h-10 px-4 bg-primary hover:bg-primary-dark text-white text-sm font-bold shadow-md shadow-orange-500/20 transition-all active:scale-95"
          @click="handleLogout"
        >
          <span class="material-symbols-outlined text-[20px]">logout</span>
          <span>Log Out</span>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="flex-1 flex flex-col h-full overflow-y-auto bg-background-light dark:bg-background-dark scroll-smooth">
      <router-view />
    </main>
  </div>
</template>

