<template>
  <div class="p-6 md:p-8 max-w-[1600px] mx-auto space-y-8 animate-fade-in">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-3xl font-black tracking-tight text-text-main-light dark:text-text-main-dark">
          Lịch thi của tôi
        </h1>
        <p class="text-text-muted-light dark:text-text-muted-dark mt-1 text-sm md:text-base">
          {{ exams[0] ? `Học kỳ diễn ra từ ${formatPrettyDate(exams[0].semesterStartDate)} đến ${formatPrettyDate(exams[0].semesterEndDate)}` : 'Tra cứu lịch thi cuối kỳ và lịch thi lại.' }}
        </p>
      </div>
      
      <!-- Semester Selector -->
      <div class="flex items-center gap-3">
         <span class="text-xs font-black uppercase text-text-muted-light tracking-widest hidden md:block">Học kỳ:</span>
         <select 
           v-model="selectedSemesterId"
           class="bg-surface-light dark:bg-surface-dark border border-border-light dark:border-border-dark px-4 py-2.5 rounded-xl font-bold text-sm outline-none focus:ring-2 focus:ring-primary shadow-sm"
         >
           <option v-for="sem in semesters" :key="sem.semesterId" :value="sem.semesterId">
             {{ sem.displayName }}
           </option>
         </select>
      </div>
    </div>

    <!-- Error/Success -->
    <div v-if="error" class="p-4 rounded-xl bg-red-50 text-red-600 border border-red-200 text-sm font-bold">
       {{ error }}
    </div>

    <!-- Summary Banner -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
       <div class="bg-primary/5 border border-primary/20 p-5 rounded-2xl flex items-center gap-4">
          <div class="p-3 bg-primary/10 text-primary rounded-xl">
             <span class="material-symbols-outlined">event_note</span>
          </div>
          <div>
             <p class="text-[10px] font-black uppercase tracking-widest text-text-muted-light">Tổng số môn thi</p>
             <p class="text-2xl font-black text-text-main-light dark:text-text-main-dark">{{ exams.length }}</p>
          </div>
       </div>
       <div class="bg-amber-50 border border-amber-200 p-5 rounded-2xl flex items-center gap-4">
          <div class="p-3 bg-amber-100 text-amber-600 rounded-xl">
             <span class="material-symbols-outlined">verified</span>
          </div>
          <div>
             <p class="text-[10px] font-black uppercase tracking-widest text-amber-700">Đủ điều kiện dự thi</p>
             <p class="text-2xl font-black text-amber-700">{{ eligibleCount }}</p>
          </div>
       </div>
       <div class="bg-red-50 border border-red-200 p-5 rounded-2xl flex items-center gap-4">
          <div class="p-3 bg-red-100 text-red-600 rounded-xl">
             <span class="material-symbols-outlined">dangerous</span>
          </div>
          <div>
             <p class="text-[10px] font-black uppercase tracking-widest text-red-700">Bị cấm thi/Thi lại</p>
             <p class="text-2xl font-black text-red-700">{{ inEligibleCount }}</p>
          </div>
       </div>
    </div>

    <!-- Main Schedule Table -->
    <div class="bg-surface-light dark:bg-surface-dark rounded-3xl border border-border-light dark:border-border-dark shadow-sm overflow-hidden min-h-[400px]">
       <div v-if="isLoading" class="p-20 flex flex-col items-center justify-center space-y-4">
          <span class="material-symbols-outlined animate-spin !text-4xl text-primary">progress_activity</span>
          <p class="font-bold text-text-muted-light">Đang tải lịch thi...</p>
       </div>
       
       <div v-else-if="exams.length === 0" class="p-20 flex flex-col items-center justify-center text-center space-y-4">
          <div class="p-6 bg-black/5 dark:bg-white/5 rounded-full">
             <span class="material-symbols-outlined !text-5xl opacity-20">history_edu</span>
          </div>
          <p class="font-bold text-text-muted-light max-w-xs">Chưa có lịch thi chính thức cho học kỳ này. Vui lòng kiểm tra lại sau.</p>
       </div>

       <div v-else class="overflow-x-auto">
          <table class="w-full text-sm text-left">
             <thead class="bg-stone-50/50 dark:bg-stone-900/50 border-b border-border-light dark:border-border-dark">
                <tr>
                   <th class="p-5 font-black uppercase tracking-widest text-[10px] text-text-muted-light">STT</th>
                   <th class="p-5 font-black uppercase tracking-widest text-[10px] text-text-muted-light">Môn học</th>
                   <th class="p-5 font-black uppercase tracking-widest text-[10px] text-text-muted-light text-center">Ngày thi</th>
                   <th class="p-5 font-black uppercase tracking-widest text-[10px] text-text-muted-light text-center">Thời gian</th>
                   <th class="p-5 font-black uppercase tracking-widest text-[10px] text-text-muted-light">Phòng</th>
                   <th class="p-5 font-black uppercase tracking-widest text-[10px] text-text-muted-light">Trạng thái</th>
                   <th class="p-5 font-black uppercase tracking-widest text-[10px] text-text-muted-light">Loại</th>
                </tr>
             </thead>
             <tbody class="divide-y divide-border-light dark:divide-border-dark">
                <tr v-for="(exam, idx) in sortedExams" :key="exam.examId" 
                    class="hover:bg-black/[0.02] dark:hover:bg-white/[0.02] transition-colors relative"
                    :class="{'opacity-60 grayscale-[0.5]': isPassed(exam.examDate)}"
                >
                   <td class="p-5 font-bold text-text-muted-light text-center w-12">{{ idx + 1 }}</td>
                   <td class="p-5">
                      <p class="font-black text-text-main-light dark:text-text-main-dark">{{ exam.courseName }}</p>
                      <p class="text-[10px] font-bold text-text-muted-light uppercase tracking-tighter">{{ exam.courseCode }}</p>
                   </td>
                   <td class="p-5 text-center">
                      <div class="font-bold text-text-main-light dark:text-text-main-dark flex flex-col items-center">
                         <span class="text-xs text-primary uppercase font-black" v-if="getDaysRemaining(exam.examDate) > 0">
                            Trong {{ getDaysRemaining(exam.examDate) }} ngày tới
                         </span>
                         <span class="text-sm">{{ formatPrettyDate(exam.examDate) }}</span>
                      </div>
                   </td>
                   <td class="p-5 text-center">
                      <span class="px-3 py-1.5 bg-black/5 dark:bg-white/5 border border-border-light dark:border-border-dark rounded-lg font-black text-xs">
                         {{ formatTime(exam.startTime) }} - {{ formatTime(exam.endTime) }}
                      </span>
                   </td>
                   <td class="p-5 font-black text-primary font-mono">{{ exam.roomNumber }}</td>
                   <td class="p-5">
                      <div v-if="exam.isEligible" class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-green-100 text-green-700 font-black text-[10px] border border-green-200 uppercase">
                         <span class="material-symbols-outlined !text-[14px]">check_circle</span>
                         Đủ điều kiện
                      </div>
                      <div v-else class="flex flex-col gap-1">
                         <div class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-red-100 text-red-700 font-black text-[10px] border border-red-200 uppercase w-fit">
                            <span class="material-symbols-outlined !text-[14px]">dangerous</span>
                            Cấm thi
                         </div>
                         <p class="text-[10px] italic text-red-500 font-medium px-1">{{ exam.reason }}</p>
                      </div>
                   </td>
                   <td class="p-5">
                      <span class="text-[10px] font-black px-2 py-1 rounded bg-stone-100 dark:bg-stone-800 border border-stone-200 dark:border-stone-700 text-text-muted-light">
                         {{ exam.examType === 'FINAL' ? 'CUỐI KỲ' : 'THI LẠI' }}
                      </span>
                   </td>
                </tr>
             </tbody>
          </table>
       </div>
    </div>
    
    <!-- Footer Note -->
    <div class="p-6 rounded-2xl bg-stone-50/50 dark:bg-stone-900/50 border border-border-light dark:border-border-dark flex items-start gap-4">
       <span class="material-symbols-outlined text-primary">info</span>
       <div class="space-y-1">
          <p class="text-sm font-black text-text-main-light dark:text-text-main-dark uppercase tracking-widest">Lưu ý quan trọng</p>
          <ul class="text-xs text-text-muted-light space-y-1 list-disc pl-4">
             <li>Sinh viên bị cấm thi sẽ KHÔNG được phép vào phòng thi và nhận điểm 0 cho môn học đó.</li>
             <li>Vui lòng mang theo Thẻ sinh viên khi đi thi. Có mặt trước 15 phút để làm thủ tục.</li>
             <li>Mọi thắc mắc về điều kiện dự thi, liên hệ phòng Đào tạo trước khi kỳ thi bắt đầu 3 ngày.</li>
          </ul>
       </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { semesterService, type SemesterResponse } from '@/services/semesterService'
import { getStudentExams, type ExamResponse } from '@/services/examService'

const semesters = ref<SemesterResponse[]>([])
const selectedSemesterId = ref<number | null>(null)
const exams = ref<ExamResponse[]>([])
const isLoading = ref(false)
const error = ref('')

onMounted(async () => {
   try {
      const semRes = await semesterService.getAllSemesters()
      semesters.value = semRes.result
      
      const current = semesters.value.find(s => s.isCurrent)
      if (current) {
         selectedSemesterId.value = current.semesterId
      } else if (semesters.value.length > 0) {
         selectedSemesterId.value = semesters.value[0]?.semesterId || null
      }
   } catch {
      error.value = 'Không thể tải thông tin học kỳ.'
   }
})

watch(selectedSemesterId, async (newVal) => {
   if (!newVal) return
   isLoading.value = true
   exams.value = []
   try {
      exams.value = await getStudentExams(newVal)
   } catch {
      error.value = 'Không thể tải lịch thi.'
   } finally {
      isLoading.value = false
   }
})

const sortedExams = computed(() => {
   return [...exams.value].sort((a, b) => a.examDate.localeCompare(b.examDate))
})

const eligibleCount = computed(() => exams.value.filter(e => e.isEligible).length)
const inEligibleCount = computed(() => exams.value.filter(e => !e.isEligible).length)

function isPassed(dateStr: string) {
   const d = new Date(dateStr)
   d.setHours(23, 59, 59)
   return d < new Date()
}

function getDaysRemaining(dateStr: string) {
   const d = new Date(dateStr)
   const today = new Date()
   today.setHours(0, 0, 0, 0)
   const diffTime = d.getTime() - today.getTime()
   const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
   return diffDays
}

function formatPrettyDate(dateStr: string) {
   if (!dateStr) return ''
  const parts = dateStr.split('-')
  if (parts.length !== 3) return dateStr
  return `${parts[2]}/${parts[1]}/${parts[0]}`
}

function formatTime(time: string) {
   if (!time) return ''
   return time.substring(0, 5)
}
</script>
