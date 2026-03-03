<script setup lang="ts">
import { ref, computed } from 'vue'

// ===== Tab Management =====
type TabName = 'system-logs' | 'audit-logs' | 'redis-analytics'
const activeTab = ref<TabName>('system-logs')

const tabs: { name: TabName; label: string }[] = [
  { name: 'system-logs', label: 'System Logs (Loki)' },
  { name: 'audit-logs', label: 'Audit Logs' },
  { name: 'redis-analytics', label: 'Redis Analytics' },
]

// ===== System Logs =====
const logFilter = ref('')
const logLevel = ref('all')
const isPaused = ref(false)

interface LogEntry {
  time: string
  level: 'INFO' | 'WARN' | 'ERROR'
  service: string
  message: string
}

const mockLogs: LogEntry[] = [
  { time: '10:42:15', level: 'INFO', service: 'auth-service', message: 'User logged in successfully (ID: 8842)' },
  { time: '10:42:18', level: 'INFO', service: 'course-manager', message: 'Cache hit for course_list_v2' },
  { time: '10:42:22', level: 'WARN', service: 'payment-gateway', message: 'Response time deviation detected (>500ms)' },
  { time: '10:42:25', level: 'INFO', service: 'auth-service', message: 'Token refreshed for session_id_9921' },
  { time: '10:42:29', level: 'ERROR', service: 'notification-worker', message: 'Failed to send FCM message. Reason: InvalidToken' },
  { time: '10:42:33', level: 'INFO', service: 'course-manager', message: 'New material uploaded for Class 10-A' },
  { time: '10:42:34', level: 'INFO', service: 'api-gateway', message: 'GET /api/v1/students/profile 200 OK 45ms' },
  { time: '10:42:38', level: 'INFO', service: 'api-gateway', message: 'POST /api/v1/attendance/mark 201 Created 120ms' },
  { time: '10:42:41', level: 'WARN', service: 'redis-cache', message: 'Memory usage approaching 85% threshold' },
  { time: '10:42:45', level: 'INFO', service: 'auth-service', message: 'Rate limit quota updated for tenant: school_01' },
  { time: '10:42:47', level: 'ERROR', service: 'db-connector', message: 'Connection timeout while acquiring pool connection' },
  { time: '10:42:50', level: 'INFO', service: 'api-gateway', message: 'GET /health 200 OK 2ms' },
  { time: '10:42:52', level: 'INFO', service: 'enrollment-service', message: 'Redis slot reserved for class 101 (35/40)' },
  { time: '10:42:55', level: 'WARN', service: 'redis-cache', message: 'Key TTL expiring soon: semester:5:classes' },
  { time: '10:42:58', level: 'INFO', service: 'enrollment-service', message: 'Student enrolled in class 103 (Redis reservation: true)' },
]

const filteredLogs = computed(() => {
  return mockLogs.filter((log) => {
    const matchLevel = logLevel.value === 'all' || log.level.toLowerCase() === logLevel.value
    const matchFilter =
      !logFilter.value ||
      log.service.toLowerCase().includes(logFilter.value.toLowerCase()) ||
      log.message.toLowerCase().includes(logFilter.value.toLowerCase())
    return matchLevel && matchFilter
  })
})

function getLevelClass(level: string) {
  switch (level) {
    case 'INFO':
      return 'text-green-400'
    case 'WARN':
      return 'text-yellow-400'
    case 'ERROR':
      return 'text-red-500'
    default:
      return 'text-gray-400'
  }
}

function getServiceClass(service: string) {
  if (service.includes('auth')) return 'text-purple-400'
  if (service.includes('course') || service.includes('enrollment')) return 'text-blue-400'
  if (service.includes('api')) return 'text-cyan-400'
  if (service.includes('redis')) return 'text-orange-300'
  if (service.includes('notification')) return 'text-red-300'
  if (service.includes('db')) return 'text-red-300'
  return 'text-yellow-200'
}

// ===== Audit Logs =====
interface AuditEntry {
  timestamp: string
  user: string
  action: string
  ip: string
  status: 'Success' | 'Failed'
}

const mockAuditLogs: AuditEntry[] = [
  { timestamp: 'Mar 03, 09:42 AM', user: 'admin@school.edu', action: 'Published Semester (Spring 2026)', ip: '192.168.1.42', status: 'Success' },
  { timestamp: 'Mar 03, 09:38 AM', user: 'admin@school.edu', action: 'Created new user (teacher)', ip: '192.168.1.42', status: 'Success' },
  { timestamp: 'Mar 03, 09:30 AM', user: 'admin@school.edu', action: 'Updated System Settings', ip: '192.168.1.42', status: 'Success' },
  { timestamp: 'Mar 03, 09:15 AM', user: 'admin@school.edu', action: 'Imported 150 students via Excel', ip: '192.168.1.42', status: 'Success' },
  { timestamp: 'Mar 03, 08:55 AM', user: 'admin@school.edu', action: 'Deleted inactive user (ID: 2341)', ip: '192.168.1.42', status: 'Success' },
  { timestamp: 'Mar 02, 04:10 PM', user: 'admin@school.edu', action: 'Set current semester', ip: '10.0.0.15', status: 'Success' },
  { timestamp: 'Mar 02, 03:45 PM', user: 'admin@school.edu', action: 'Created 12 new classes', ip: '10.0.0.15', status: 'Success' },
  { timestamp: 'Mar 02, 02:30 PM', user: 'admin@school.edu', action: 'Reset password for user ID: 5521', ip: '10.0.0.15', status: 'Failed' },
]

// ===== Redis Analytics =====
interface ClassCapacity {
  name: string
  courseCode: string
  current: number
  max: number
}

const mockClassCapacities: ClassCapacity[] = [
  { name: 'CS101.01', courseCode: 'CS101', current: 40, max: 40 },
  { name: 'CS102.01', courseCode: 'CS102', current: 34, max: 40 },
  { name: 'CS201.02', courseCode: 'CS201', current: 28, max: 35 },
  { name: 'MATH101.01', courseCode: 'MATH101', current: 18, max: 40 },
  { name: 'ENG201.01', courseCode: 'ENG201', current: 12, max: 30 },
  { name: 'PHY101.01', courseCode: 'PHY101', current: 5, max: 35 },
]

const totalSlots = computed(() => mockClassCapacities.reduce((s, c) => s + c.max, 0))
const filledSlots = computed(() => mockClassCapacities.reduce((s, c) => s + c.current, 0))
const fillRate = computed(() => Math.round((filledSlots.value / totalSlots.value) * 100))

function getCapacityColor(current: number, max: number) {
  const pct = (current / max) * 100
  if (pct >= 100) return 'bg-red-500'
  if (pct >= 75) return 'bg-primary'
  if (pct >= 50) return 'bg-yellow-500'
  return 'bg-green-500'
}

function getCapacityTextColor(current: number, max: number) {
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
      <!-- ===== System Logs Tab ===== -->
      <div
        v-if="activeTab === 'system-logs'"
        class="absolute inset-0 flex flex-col gap-4"
      >
        <!-- Filters -->
        <div
          class="flex flex-col sm:flex-row gap-4 items-center justify-between bg-surface-light dark:bg-surface-dark p-4 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm flex-shrink-0"
        >
          <div class="flex items-center gap-4 w-full sm:w-auto">
            <div class="relative w-full sm:w-64">
              <span
                class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[18px]"
                >search</span
              >
              <input
                v-model="logFilter"
                class="w-full pl-9 pr-4 h-9 text-sm"
                placeholder="Filter by service name..."
                type="text"
              />
            </div>
            <select v-model="logLevel" class="h-9 py-0 pl-3 pr-8 text-sm">
              <option value="all">All Levels</option>
              <option value="info">Info</option>
              <option value="warn">Warning</option>
              <option value="error">Error</option>
            </select>
          </div>
          <div class="flex gap-2">
            <button
              class="p-2 transition-colors"
              :class="isPaused ? 'text-primary' : 'text-slate-500 hover:text-primary'"
              title="Pause Live Stream"
              @click="isPaused = !isPaused"
            >
              <span class="material-symbols-outlined">{{
                isPaused ? 'play_circle' : 'pause_circle'
              }}</span>
            </button>
            <button
              class="p-2 text-slate-500 hover:text-primary transition-colors"
              title="Clear Logs"
            >
              <span class="material-symbols-outlined">delete_sweep</span>
            </button>
            <button
              class="p-2 text-slate-500 hover:text-primary transition-colors"
              title="Download Logs"
            >
              <span class="material-symbols-outlined">download</span>
            </button>
          </div>
        </div>

        <!-- Terminal -->
        <div
          class="flex-1 bg-[#1e1e1e] rounded-xl border border-stone-800 shadow-inner overflow-hidden flex flex-col font-mono text-sm"
        >
          <div
            class="flex items-center justify-between px-4 py-2 bg-[#2d2d2d] border-b border-[#3e3e3e]"
          >
            <div class="flex gap-2">
              <div class="w-3 h-3 rounded-full bg-red-500"></div>
              <div class="w-3 h-3 rounded-full bg-yellow-500"></div>
              <div class="w-3 h-3 rounded-full bg-green-500"></div>
            </div>
            <div class="text-xs text-gray-400">loki-promtail-stream</div>
          </div>
          <div
            class="flex-1 overflow-y-auto p-4 terminal-scroll text-gray-300 leading-relaxed"
          >
            <div
              v-for="(log, idx) in filteredLogs"
              :key="idx"
              class="pb-1 hover:bg-white/5 px-2 -mx-2 rounded"
            >
              <span class="text-gray-500 select-none">[{{ log.time }}]</span>
              <span :class="[getLevelClass(log.level), 'font-bold']"> {{ log.level }} </span>
              <span :class="getServiceClass(log.service)">{{ log.service }}</span
              >: {{ log.message }}
            </div>

            <div
              v-if="filteredLogs.length === 0"
              class="text-gray-500 text-center py-8"
            >
              No logs matching filter.
            </div>

            <div
              v-if="!isPaused"
              class="flex items-center gap-2 text-primary mt-2 animate-pulse"
            >
              <span class="material-symbols-outlined text-[16px]">terminal</span>
              <span>Listening for new logs...</span>
            </div>
            <div v-else class="flex items-center gap-2 text-yellow-400 mt-2">
              <span class="material-symbols-outlined text-[16px]">pause</span>
              <span>Stream paused</span>
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
            <h3 class="font-bold text-slate-800 dark:text-slate-200">
              Administrative Actions
            </h3>
            <button class="text-sm text-primary font-medium hover:underline">
              Export CSV
            </button>
          </div>
          <div class="overflow-auto flex-1">
            <table class="w-full text-left border-collapse">
              <thead class="bg-stone-50 dark:bg-stone-900/50 sticky top-0">
                <tr>
                  <th
                    class="p-4 text-xs font-bold uppercase text-slate-500 dark:text-slate-400"
                  >
                    Timestamp
                  </th>
                  <th
                    class="p-4 text-xs font-bold uppercase text-slate-500 dark:text-slate-400"
                  >
                    User
                  </th>
                  <th
                    class="p-4 text-xs font-bold uppercase text-slate-500 dark:text-slate-400"
                  >
                    Action
                  </th>
                  <th
                    class="p-4 text-xs font-bold uppercase text-slate-500 dark:text-slate-400"
                  >
                    IP Address
                  </th>
                  <th
                    class="p-4 text-xs font-bold uppercase text-slate-500 dark:text-slate-400"
                  >
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
      <div
        v-if="activeTab === 'redis-analytics'"
        class="absolute inset-0 overflow-y-auto"
      >
        <!-- Summary Cards -->
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
          <div
            class="bg-surface-light dark:bg-surface-dark p-5 rounded-xl border border-stone-200 dark:border-stone-800 shadow-sm"
          >
            <p class="text-xs font-bold uppercase text-slate-500 dark:text-slate-400 mb-1">
              Total Classes Cached
            </p>
            <p class="text-2xl font-bold text-slate-900 dark:text-white">
              {{ mockClassCapacities.length }}
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
            <div class="space-y-4">
              <div v-for="cls in mockClassCapacities" :key="cls.name">
                <div class="flex justify-between text-sm mb-1">
                  <span class="text-slate-700 dark:text-slate-300">
                    {{ cls.name }}
                    <span class="text-xs text-slate-400">({{ cls.courseCode }})</span>
                  </span>
                  <span
                    :class="[getCapacityTextColor(cls.current, cls.max), 'font-bold']"
                  >
                    {{ cls.current >= cls.max ? 'Full' : '' }}
                    ({{ Math.round((cls.current / cls.max) * 100) }}%)
                  </span>
                </div>
                <div
                  class="w-full bg-stone-100 dark:bg-stone-800 rounded-full h-2.5"
                >
                  <div
                    :class="[getCapacityColor(cls.current, cls.max), 'h-2.5 rounded-full transition-all duration-500']"
                    :style="{ width: Math.min((cls.current / cls.max) * 100, 100) + '%' }"
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
          <div class="font-mono text-sm bg-[#1e1e1e] text-gray-300 rounded-lg p-4 overflow-x-auto">
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
                { courseCode, courseName, maxSlot, currentSlot, sessions, ... }</span
              >
            </div>
          </div>
        </div>
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
