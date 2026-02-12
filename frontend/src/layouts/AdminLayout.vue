<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { apiFetch } from '@/utils/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const user = computed(() => authStore.user)
const mobileMenuOpen = ref(false)

function closeMobileMenu() {
  mobileMenuOpen.value = false
}

const navItems = [
  { name: 'Dashboard', icon: 'dashboard', routeName: 'admin-dashboard' },
  { name: 'User Management', icon: 'group', routeName: 'admin-users' },
  { name: 'Department Management', icon: 'account_tree', routeName: 'admin-departments' },
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
    <!-- Mobile menu button -->
    <button
      class="md:hidden fixed top-4 left-4 z-50 size-10 flex items-center justify-center rounded-lg bg-surface-light dark:bg-surface-dark border border-stone-200 dark:border-stone-800 shadow-md"
      aria-label="Open menu"
      @click="mobileMenuOpen = true"
    >
      <span class="material-symbols-outlined text-2xl">menu</span>
    </button>

    <!-- Mobile full-screen overlay menu -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="mobileMenuOpen"
          class="fixed inset-0 z-[100] flex flex-col bg-surface-light dark:bg-surface-dark md:hidden"
        >
          <div class="flex items-center justify-between p-4 border-b border-stone-200 dark:border-stone-800">
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
              <div>
                <h1 class="text-slate-900 dark:text-white text-base font-bold">Admin Portal</h1>
                <p class="text-primary text-sm truncate max-w-[180px]">{{ user?.email || 'Administrator' }}</p>
              </div>
            </div>
            <button
              class="size-10 flex items-center justify-center rounded-lg hover:bg-stone-100 dark:hover:bg-stone-800"
              aria-label="Close menu"
              @click="closeMobileMenu"
            >
              <span class="material-symbols-outlined text-2xl">close</span>
            </button>
          </div>
          <nav class="flex-1 overflow-y-auto p-4 flex flex-col gap-2">
            <router-link
              v-for="item in navItems"
              :key="item.routeName"
              :to="{ name: item.routeName }"
              :class="[
                'flex items-center gap-3 px-4 py-3 rounded-lg transition-all',
                isActive(item.routeName)
                  ? 'bg-primary/10 border-l-4 border-primary'
                  : 'hover:bg-stone-100 dark:hover:bg-stone-800 text-slate-600 dark:text-slate-400',
              ]"
              @click="closeMobileMenu"
            >
              <span class="material-symbols-outlined" :class="isActive(item.routeName) ? 'text-primary' : ''">{{ item.icon }}</span>
              <p class="text-sm font-medium" :class="isActive(item.routeName) ? 'text-slate-900 dark:text-white' : ''">{{ item.name }}</p>
            </router-link>
          </nav>
          <div class="p-4 border-t border-stone-200 dark:border-stone-800">
            <button
              class="flex w-full items-center justify-center gap-2 rounded-lg h-12 px-4 bg-primary hover:bg-primary-dark text-white text-sm font-bold shadow-md"
              @click="handleLogout"
            >
              <span class="material-symbols-outlined text-[20px]">logout</span>
              <span>Log Out</span>
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Side Navigation (desktop) -->
    <aside class="hidden md:flex w-72 h-full flex-col border-r border-stone-200 dark:border-stone-800 bg-surface-light dark:bg-surface-dark shrink-0">
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
    <main class="flex-1 flex flex-col h-full overflow-y-auto bg-background-light dark:bg-background-dark scroll-smooth pt-16 md:pt-0">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

