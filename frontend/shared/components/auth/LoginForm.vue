<script setup lang="ts">
import { reactive, ref } from "vue";

// Định nghĩa Props nhận vào (ví dụ: có cho phép chọn Role không?)
const props = defineProps<{
  showRoles?: boolean; // Admin thì có thể không cần chọn role, hoặc mặc định
}>();

// Định nghĩa sự kiện bắn ra cho App cha
const emit = defineEmits(["submit", "forgot-password"]);

const form = reactive({
  role: "Student",
  username: "",
  password: "",
  remember: false,
});

const showPassword = ref(false);

const handleSubmit = () => {
  // Bắn dữ liệu ra ngoài cho Admin/Portal xử lý
  emit("submit", { ...form });
};
</script>

<template>
  <form @submit.prevent="handleSubmit" class="flex flex-col gap-5">
    <div
      v-if="showRoles"
      class="flex p-1 bg-background-light dark:bg-background-dark/50 rounded-lg border border-border-light dark:border-border-dark"
    >
      <label
        v-for="role in ['Student', 'Teacher', 'Admin']"
        :key="role"
        class="cursor-pointer flex-1 relative"
      >
        <input
          type="radio"
          name="role"
          :value="role"
          v-model="form.role"
          class="peer sr-only"
        />
        <div
          class="flex items-center justify-center py-2 text-sm font-medium rounded-md text-text-muted-light dark:text-text-muted-dark transition-all peer-checked:bg-white dark:peer-checked:bg-surface-dark peer-checked:text-primary peer-checked:shadow-sm"
        >
          {{ role }}
        </div>
      </label>
    </div>

    <label class="flex flex-col gap-1.5">
      <span class="text-sm font-medium">Username or Email</span>
      <div class="relative group">
        <span
          class="absolute left-4 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark group-focus-within:text-primary transition-colors"
        >
          <span class="material-symbols-outlined text-[20px]">person</span>
        </span>
        <input
          v-model="form.username"
          type="text"
          class="form-input w-full rounded-lg border-border-light dark:border-border-dark bg-background-light dark:bg-background-dark/50 pl-11 pr-4 py-3 text-sm focus:border-primary focus:ring-1 focus:ring-primary placeholder:text-text-muted-light/70 dark:placeholder:text-text-muted-dark/50 transition-all"
          placeholder="Enter ID or Email"
          required
        />
      </div>
    </label>

    <label class="flex flex-col gap-1.5">
      <span class="text-sm font-medium">Password</span>
      <div class="relative group">
        <span
          class="absolute left-4 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark group-focus-within:text-primary transition-colors"
        >
          <span class="material-symbols-outlined text-[20px]">lock</span>
        </span>
        <input
          v-model="form.password"
          :type="showPassword ? 'text' : 'password'"
          class="form-input w-full rounded-lg border-border-light dark:border-border-dark bg-background-light dark:bg-background-dark/50 pl-11 pr-12 py-3 text-sm focus:border-primary focus:ring-1 focus:ring-primary placeholder:text-text-muted-light/70 dark:placeholder:text-text-muted-dark/50 transition-all"
          placeholder="Enter password"
          required
        />
        <button
          type="button"
          @click="showPassword = !showPassword"
          class="absolute right-4 top-1/2 -translate-y-1/2 text-text-muted-light dark:text-text-muted-dark hover:text-primary transition-colors"
        >
          <span class="material-symbols-outlined text-[20px]">{{
            showPassword ? "visibility" : "visibility_off"
          }}</span>
        </button>
      </div>
    </label>

    <div class="flex items-center justify-between mt-1">
      <label class="flex items-center gap-2 cursor-pointer group">
        <input
          v-model="form.remember"
          type="checkbox"
          class="h-4 w-4 rounded border-border-light dark:border-border-dark text-primary focus:ring-offset-0 focus:ring-primary/20 bg-transparent group-hover:border-primary transition-colors"
        />
        <span
          class="text-sm text-text-muted-light dark:text-text-muted-dark group-hover:text-primary transition-colors"
          >Remember me</span
        >
      </label>
      <button
        type="button"
        @click="emit('forgot-password')"
        class="text-sm font-medium text-primary hover:text-primary-dark transition-colors"
      >
        Forgot Password?
      </button>
    </div>

    <button
      type="submit"
      class="w-full bg-primary hover:bg-primary-dark text-white font-medium py-3 rounded-lg shadow-md shadow-primary/20 transition-all active:scale-[0.98] mt-2 flex items-center justify-center gap-2"
    >
      <span>Login</span>
      <span class="material-symbols-outlined text-sm">arrow_forward</span>
    </button>
  </form>
</template>
