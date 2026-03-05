<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import {
  getEnrollmentStats,
  type EnrollmentStatsResponse,
  type ClassEnrollmentStat,
} from '@/services/enrollmentStatsService'
import { getAdminSemesterList, type AdminSemesterListItem } from '@/services/adminUserService'

// ===== Tab Management =====
type TabName = 'system-logs' | 'audit-logs' | 'redis-analytics'
const activeTab = ref<TabName>('system-logs')

const tabs: { name: TabName; label: string }[] = [
  { name: 'system-logs', label: 'System Logs' },
  { name: 'audit-logs', label: 'Audit Logs' },
  { name: 'redis-analytics', label: 'Redis Analytics' },
]

// ===== Grafana =====
const grafanaUrl = import.meta.env.VITE_GRAFANA_URL || 'http://localhost:3001'

// ===== Audit Logs =====
interface AuditEntry {
  timestamp: string
  user: string
  action: string
  ip: string
  status: 'Success' | 'Failed'
}

const mockAuditLogs: AuditEntry[] = [
  {
    timestamp: 'Mar 03, 09:42 AM',
    user: 'admin@school.edu',
    action: 'Published Semester (Spring 2026)',
    ip: '192.168.1.42',
    status: 'Success',
  },
  {
    timestamp: 'Mar 03, 09:38 AM',
    user: 'admin@school.edu',
    action: 'Created new user (teacher)',
    ip: '192.168.1.42',
    status: 'Success',
  },
  {
    timestamp: 'Mar 03, 09:30 AM',
    user: 'admin@school.edu',
    action: 'Updated System Settings',
    ip: '192.168.1.42',
    status: 'Success',
  },
  {
    timestamp: 'Mar 03, 09:15 AM',
    user: 'admin@school.edu',
    action: 'Imported 150 students via Excel',
    ip: '192.168.1.42',
    status: 'Success',
  },
  {
    timestamp: 'Mar 03, 08:55 AM',
    user: 'admin@school.edu',
    action: 'Deleted inactive user (ID: 2341)',
    ip: '192.168.1.42',
    status: 'Success',
  },
  {
    timestamp: 'Mar 02, 04:10 PM',
    user: 'admin@school.edu',
    action: 'Set current semester',
    ip: '10.0.0.15',
    status: 'Success',
  },
  {
    timestamp: 'Mar 02, 03:45 PM',
    user: 'admin@school.edu',
    action: 'Created 12 new classes',
    ip: '10.0.0.15',
    status: 'Success',
  },
  {
    timestamp: 'Mar 02, 02:30 PM',
    user: 'admin@school.edu',
    action: 'Reset password for user ID: 5521',
    ip: '10.0.0.15',
    status: 'Failed',
  },
]

// ===== Redis Analytics (Real API) =====
const semesters = ref<AdminSemesterListItem[]>([])
const selectedSemesterId = ref<number | null>(null)
const statsLoading = ref(false)
const statsError = ref<string | null>(null)
const enrollmentStats = ref<EnrollmentStatsResponse | null>(null)
let refreshInterval: ReturnType<typeof setInterval> | null = null

// Computed from real data
const classCapacities = computed<ClassEnrollmentStat[]>(() => enrollmentStats.value?.classes ?? [])
const totalSlots = computed(() => enrollmentStats.value?.totalSlots ?? 0)
const filledSlots = computed(() => enrollmentStats.value?.filledSlots ?? 0)
const fillRate = computed(() =>
  totalSlots.value > 0 ? Math.round((filledSlots.value / totalSlots.value) * 100) : 0,
)
const cacheActive = computed(() => enrollmentStats.value?.cacheActive ?? false)

async function loadSemesters() {
  try {
    const result = await getAdminSemesterList({ page: 0, size: 100 })
    semesters.value = result.content
    // Tự động chọn semester current
    const current = semesters.value.find((s) => s.isCurrent)
    if (current) {
      selectedSemesterId.value = current.semesterId
    } else if (semesters.value.length > 0) {
      selectedSemesterId.value = semesters.value[0]?.semesterId || null
    }
  } catch (e) {
    console.error('Failed to load semesters:', e)
  }
}

async function loadStats() {
  if (!selectedSemesterId.value) return
  try {
    statsLoading.value = true
    statsError.value = null
    enrollmentStats.value = await getEnrollmentStats(selectedSemesterId.value)
  } catch (e: unknown) {
    statsError.value = e instanceof Error ? e.message : 'Failed to load stats'
    console.error('Failed to load enrollment stats:', e)
  } finally {
    statsLoading.value = false
  }
}

function startAutoRefresh() {
  stopAutoRefresh()
  refreshInterval = setInterval(loadStats, 10000) // Refresh every 10s
}

function stopAutoRefresh() {
  if (refreshInterval) {
    clearInterval(refreshInterval)
    refreshInterval = null
  }
}

watch(selectedSemesterId, () => {
  loadStats()
})

watch(activeTab, (tab) => {
  if (tab === 'redis-analytics') {
    loadStats()
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
})

onMounted(async () => {
  await loadSemesters()
})

onUnmounted(() => {
  stopAutoRefresh()
})

function getCapacityColor(current: number, max: number) {
  const pct = (current / max) * 100
  if (pct >= 100) return 'bg-red-500'
  if (pct >= 75) return 'bg-primary'
  if (pct >= 50) return 'bg-yellow-500'
  return 'bg-green-500'
}

function getCapacityTextColor(current: number, max: number) {
  if (max === 0) return 'text-slate-500'
  const pct = (current / max) * 100
  if (pct >= 100) return 'text-red-500'
  if (pct >= 75) return 'text-primary'
  if (pct >= 50) return 'text-yellow-500'
  return 'text-green-500'
}
</script>

<template>
  <div class="h-full flex flex-col w-full max-w-[1600px] mx-auto p-6 md:p-8 gap-6">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 flex-shrink-0">
      <div>
        <h1
          class="text-slate-900 dark:text-white text-2xl md:text-3xl font-bold leading-tight tracking-tight"
        >
          System Monitoring
        </h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">
          Real-time logs, audit trails, and infrastructure analytics.
        </p>
      </div>
      <div class="flex items-center gap-3">
        <div
          class="flex items-center gap-2 px-3 py-1 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 rounded-full text-xs font-medium border border-green-200 dark:border-green-800"
        >
          <span class="relative flex h-2 w-2">
            <span
              class="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"
            ></span>
            <span class="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
          </span>
          System Healthy
        </div>
        <span class="text-xs text-slate-400">Last updated: Just now</span>
      </div>
    </div>

    <!-- Tabs -->
    <div class="flex-shrink-0 border-b border-stone-200 dark:border-stone-800">
      <nav aria-label="Tabs" class="flex gap-6">
        <button
          v-for="tab in tabs"
          :key="tab.name"
          :class="[
            'border-b-2 py-4 px-1 text-sm font-medium transition-colors',
            activeTab === tab.name
              ? 'border-primary text-primary font-bold'
              : 'border-transparent text-slate-500 hover:border-stone-300 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-300',
          ]"
          @click="activeTab = tab.name"
        >
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- Tab Content -->
    <div class="flex-1 min-h-0 relative">
      <!-- ===== System Logs Tab (Grafana Link) ===== -->
      <div
        v-if="activeTab === 'system-logs'"
        class="absolute inset-0 flex flex-col items-center justify-center gap-6"
      >
        <div
          class="w-full max-w-2xl bg-surface-light dark:bg-surface-dark rounded-2xl border border-stone-200 dark:border-stone-800 shadow-sm p-8 md:p-12 text-center"
        >
          <!-- Grafana Icon -->
          <div
            class="mx-auto w-20 h-20 rounded-2xl bg-gradient-to-br from-orange-400 to-orange-600 flex items-center justify-center mb-6 shadow-lg shadow-orange-500/20"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="white"
              class="w-10 h-10"
            >
              <path
                d="M22 12c0-5.523-4.477-10-10-10S2 6.477 2 12s4.477 10 10 10 10-4.477 10-10zm-10 8a8 8 0 110-16 8 8 0 010 16zm0-14a6 6 0 100 12 6 6 0 000-12zm0 2a4 4 0 110 8 4 4 0 010-8zm0 2a2 2 0 100 4 2 2 0 000-4z"
              />
            </svg>
          </div>

          <h2 class="text-xl md:text-2xl font-bold text-slate-900 dark:text-white mb-2">
            Grafana Log Dashboard
          </h2>
          <p class="text-slate-500 dark:text-slate-400 text-sm md:text-base max-w-md mx-auto mb-8">
            System logs are managed via <strong>Grafana + Loki</strong>. Click the button below to
            open the Grafana dashboard for real-time log monitoring, filtering, and alerting.
          </p>

          <a
            :href="grafanaUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="inline-flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-orange-500 to-orange-600 hover:from-orange-600 hover:to-orange-700 text-white font-semibold rounded-xl shadow-md shadow-orange-500/20 transition-all duration-200 hover:shadow-lg hover:shadow-orange-500/30 hover:-translate-y-0.5"
          >
            <span class="material-symbols-outlined text-[20px]">open_in_new</span>
            Open Grafana Dashboard
          </a>

          <!-- Quick Links -->
          <div class="mt-8 pt-6 border-t border-stone-200 dark:border-stone-800">
            <p class="text-xs font-bold uppercase text-slate-400 mb-4 tracking-wider">
              Quick Links
            </p>
            <div class="flex flex-wrap justify-center gap-3">
              <a
                :href="`${grafanaUrl}/explore`"
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 rounded-lg transition-colors"
              >
                <span class="material-symbols-outlined text-[16px]">search</span>
                Explore Logs
              </a>
              <a
                :href="`${grafanaUrl}/alerting/list`"
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 rounded-lg transition-colors"
              >
                <span class="material-symbols-outlined text-[16px]">notifications</span>
                Alert Rules
              </a>
              <a
                :href="`${grafanaUrl}/dashboards`"
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 rounded-lg transition-colors"
              >
                <span class="material-symbols-outlined text-[16px]">dashboard</span>
                Dashboards
              </a>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== Audit Logs Tab ===== -->
      <div
        v-if="activeTab === 'audit-logs'"
        class="absolute inset-0 flex flex-col gap-4 overflow-hidden"
      >
        <div
          class="bg-surface-light dark:bg-surface-dark rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm overflow-hidden flex flex-col h-full"
        >
          <div
            class="p-4 border-b border-stone-200 dark:border-stone-800 bg-stone-50/50 dark:bg-stone-900/30 flex justify-between items-center flex-shrink-0"
          >
            <h3 class="font-bold text-slate-800 dark:text-slate-200">Administrative Actions</h3>
            <button class="text-sm text-primary font-medium hover:underline">Export CSV</button>
          </div>
          <div class="overflow-auto flex-1">
            <table class="w-full text-left border-collapse">
              <thead class="bg-stone-50 dark:bg-stone-900/50 sticky top-0">
                <tr>
                  <th class="p-4 text-xs font-bold uppercase text-slate-500 dark:text-slate-400">
                    Timestamp
                  </th>
                  <th class="p-4 text-xs font-bold uppercase text-slate-500 dark:text-slate-400">
                    User
                  </th>
                  <th class="p-4 text-xs font-bold uppercase text-slate-500 dark:text-slate-400">
                    Action
                  </th>
                  <th class="p-4 text-xs font-bold uppercase text-slate-500 dark:text-slate-400">
                    IP Address
                  </th>
                  <th class="p-4 text-xs font-bold uppercase text-slate-500 dark:text-slate-400">
                    Status
                  </th>
                </tr>
              </thead>
              <tbody class="divide-y divide-stone-200 dark:divide-stone-800">
                <tr
                  v-for="(entry, idx) in mockAuditLogs"
                  :key="idx"
                  class="hover:bg-stone-50 dark:hover:bg-stone-900/30 transition-colors"
                >
                  <td class="p-4 text-sm text-slate-500">{{ entry.timestamp }}</td>
                  <td class="p-4 text-sm font-medium text-slate-900 dark:text-white">
                    {{ entry.user }}
                  </td>
                  <td class="p-4 text-sm text-slate-700 dark:text-slate-300">
                    {{ entry.action }}
                  </td>
                  <td class="p-4 text-sm font-mono text-slate-500">{{ entry.ip }}</td>
                  <td class="p-4">
                    <span
                      :class="[
                        'px-2 py-1 rounded-full text-xs font-bold',
                        entry.status === 'Success'
                          ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'
                          : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300',
                      ]"
                    >
                      {{ entry.status }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- ===== Redis Analytics Tab ===== -->
      <div v-if="activeTab === 'redis-analytics'" class="absolute inset-0 overflow-y-auto">
        <!-- Semester Selector -->
        <div
          class="mb-6 flex justify-between items-center bg-surface-light dark:bg-surface-dark p-4 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm"
        >
          <div>
            <h3 class="text-base font-bold text-slate-900 dark:text-white">
              Live Registration Status
            </h3>
            <p class="text-sm text-slate-500">Monitor Redis cache fill rates in real-time</p>
          </div>
          <div class="flex items-center gap-3">
            <label class="text-sm font-medium text-slate-700 dark:text-slate-300">Semester</label>
            <select
              v-model="selectedSemesterId"
              class="h-9 py-0 pl-3 pr-8 text-sm w-48 bg-stone-50 dark:bg-[#1a1a1a] border-stone-200 dark:border-stone-800 rounded-lg"
            >
              <option v-for="s in semesters" :key="s.semesterId" :value="s.semesterId">
                {{ s.displayName || `${s.name} ${s.year}` }}
              </option>
            </select>
          </div>
        </div>

        <div
          v-if="statsLoading && !enrollmentStats"
          class="flex items-center gap-2 justify-center py-12 text-primary"
        >
          <span class="material-symbols-outlined animate-spin">progress_activity</span>
          <span>Loading Redis metrics...</span>
        </div>

        <div
          v-else-if="statsError"
          class="py-12 text-center text-red-500 bg-red-50 dark:bg-red-900/10 rounded-xl border border-red-200 dark:border-red-900/30"
        >
          <span class="material-symbols-outlined text-4xl mb-2">error</span>
          <p class="font-bold">Failed to load analytics</p>
          <p class="text-sm opacity-80">{{ statsError }}</p>
          <button
            class="mt-4 px-4 py-2 border border-red-200 dark:border-red-800 rounded-lg text-sm font-medium hover:bg-red-100 dark:hover:bg-red-900/20 transition-colors"
            @click="loadStats"
          >
            Retry
          </button>
        </div>

        <div
          v-else-if="!cacheActive"
          class="py-16 text-center text-slate-500 bg-stone-50 dark:bg-[#1e1e1e] rounded-xl border border-stone-200 dark:border-stone-800 flex flex-col items-center justify-center"
        >
          <span class="material-symbols-outlined text-4xl mb-3 text-slate-300 dark:text-slate-600"
            >cloud_off</span
          >
          <h3 class="text-lg font-bold text-slate-700 dark:text-slate-300 mb-1">
            Cache Not Active
          </h3>
          <p class="text-sm max-w-sm">
            Registration is not currently open for this semester. Publish the semester using the
            Admin tools to sync class data to Redis.
          </p>
        </div>

        <template v-else>
          <!-- Summary Cards -->
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
            <div
              class="bg-surface-light dark:bg-surface-dark p-5 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm"
            >
              <p class="text-xs font-bold uppercase text-slate-500 dark:text-slate-400 mb-1">
                Total Classes Cached
              </p>
              <p class="text-2xl font-bold text-slate-900 dark:text-white">
                {{ classCapacities.length }}
              </p>
            </div>
            <div
              class="bg-surface-light dark:bg-surface-dark p-5 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm"
            >
              <p class="text-xs font-bold uppercase text-slate-500 dark:text-slate-400 mb-1">
                Total Slots
              </p>
              <p class="text-2xl font-bold text-slate-900 dark:text-white">
                {{ filledSlots }} / {{ totalSlots }}
              </p>
            </div>
            <div
              class="bg-surface-light dark:bg-surface-dark p-5 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm"
            >
              <p class="text-xs font-bold uppercase text-slate-500 dark:text-slate-400 mb-1">
                Overall Fill Rate
              </p>
              <p class="text-2xl font-bold" :class="getCapacityTextColor(filledSlots, totalSlots)">
                {{ fillRate }}%
              </p>
            </div>
          </div>

          <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <!-- Gauge Chart -->
            <div
              class="bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex flex-col items-center justify-center"
            >
              <h3 class="text-lg font-bold text-slate-900 dark:text-white mb-6">
                Redis Slot Fill Rate
              </h3>
              <div class="relative w-64 h-32 overflow-hidden">
                <!-- Background arc -->
                <div
                  class="absolute w-64 h-64 rounded-full border-[20px] border-stone-100 dark:border-stone-800 top-0 left-0 box-border"
                ></div>
                <!-- Fill arc -->
                <div
                  class="absolute w-64 h-64 rounded-full border-[20px] border-primary border-t-transparent border-r-transparent border-l-transparent top-0 left-0 box-border"
                  :style="{
                    clipPath: 'polygon(0 0, 100% 0, 100% 50%, 0 50%)',
                    transform: `rotate(${45 + (fillRate / 100) * 180}deg)`,
                  }"
                ></div>
                <!-- Value -->
                <div class="absolute bottom-0 left-1/2 -translate-x-1/2 text-center">
                  <span class="text-4xl font-bold text-slate-900 dark:text-white"
                    >{{ fillRate }}%</span
                  >
                  <p class="text-xs text-slate-500">Capacity Used</p>
                </div>
              </div>
            </div>

            <!-- Class Capacity Bars -->
            <div
              class="bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm"
            >
              <h3 class="text-lg font-bold text-slate-900 dark:text-white mb-4">
                Class Capacity Status
              </h3>
              <div class="space-y-4 max-h-[400px] overflow-y-auto pr-2">
                <div v-for="cls in classCapacities" :key="cls.classId">
                  <div class="flex justify-between text-sm mb-1">
                    <span class="text-slate-700 dark:text-slate-300 font-medium">
                      {{ cls.courseName }}
                      <span class="text-xs text-slate-400"
                        >({{ cls.courseCode }} - ID: {{ cls.classId }})</span
                      >
                    </span>
                    <span
                      :class="[getCapacityTextColor(cls.currentSlot, cls.maxSlot), 'font-bold']"
                    >
                      {{ cls.currentSlot >= cls.maxSlot && cls.maxSlot > 0 ? 'Full' : '' }}
                      {{ cls.currentSlot }} / {{ cls.maxSlot }} ({{
                        Math.round((cls.maxSlot > 0 ? cls.currentSlot / cls.maxSlot : 0) * 100)
                      }}%)
                    </span>
                  </div>
                  <div class="w-full bg-stone-100 dark:bg-stone-800 rounded-full h-2.5">
                    <div
                      :class="[
                        getCapacityColor(cls.currentSlot, cls.maxSlot),
                        'h-2.5 rounded-full transition-all duration-500',
                      ]"
                      :style="{
                        width:
                          Math.min(
                            (cls.maxSlot > 0 ? cls.currentSlot / cls.maxSlot : 0) * 100,
                            100,
                          ) + '%',
                      }"
                    ></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Redis Key Info -->
          <div
            class="mt-6 bg-surface-light dark:bg-surface-dark p-6 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm"
          >
            <h3 class="text-lg font-bold text-slate-900 dark:text-white mb-4">
              Redis Key Structure
            </h3>
            <div
              class="font-mono text-sm bg-[#1e1e1e] text-gray-300 rounded-lg p-4 overflow-x-auto"
            >
              <div class="text-gray-500"># Danh sách class IDs của semester</div>
              <div>
                <span class="text-cyan-400">semester:{semesterId}:classes</span>
                <span class="text-gray-500"> → </span>
                <span class="text-yellow-300">SET</span>
                <span class="text-green-400"> [1, 2, 3, ...]</span>
              </div>
              <div class="mt-3 text-gray-500"># Chi tiết từng class (Hash)</div>
              <div>
                <span class="text-cyan-400">class:{classId}</span>
                <span class="text-gray-500"> → </span>
                <span class="text-yellow-300">HASH</span>
                <span class="text-green-400">
                  { courseCode, courseName, maxSlot, currentSlot, status, ... }</span
                >
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.terminal-scroll::-webkit-scrollbar {
  width: 8px;
}
.terminal-scroll::-webkit-scrollbar-track {
  background: #1e1e1e;
}
.terminal-scroll::-webkit-scrollbar-thumb {
  background: #444;
  border-radius: 4px;
}
.terminal-scroll::-webkit-scrollbar-thumb:hover {
  background: #666;
}
</style>
