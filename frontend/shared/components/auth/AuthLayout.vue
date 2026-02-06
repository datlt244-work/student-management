<script setup lang="ts">
defineProps<{
  title?: string;
  subtitle?: string;
}>();
</script>

<template>
  <div
    class="bg-background-light dark:bg-background-dark text-text-main-light dark:text-text-main-dark font-display antialiased min-h-screen flex flex-col overflow-x-hidden geo-pattern"
  >
    <div
      class="layout-container flex h-full grow flex-col items-center justify-center p-4 sm:p-6 lg:p-8"
    >
      <div
        class="w-full max-w-md bg-surface-light dark:bg-surface-dark rounded-xl shadow-[0_4px_20px_rgba(0,0,0,0.05)] dark:shadow-[0_4px_20px_rgba(0,0,0,0.4)] border border-border-light dark:border-border-dark overflow-hidden relative"
      >
        <div class="h-2 w-full bg-primary"></div>
        <div class="p-8 flex flex-col gap-6">
          <div class="flex flex-col items-center gap-4 text-center">
            <div
              class="h-16 w-16 bg-primary/10 rounded-full flex items-center justify-center text-primary"
            >
              <span class="material-symbols-outlined text-4xl">school</span>
            </div>
            <div>
              <h1 class="text-2xl font-bold tracking-tight">
                {{ title || "Welcome Back" }}
              </h1>
              <p
                class="text-text-muted-light dark:text-text-muted-dark text-sm mt-1"
              >
                {{ subtitle || "Please sign in to your account" }}
              </p>
            </div>
          </div>

          <slot />
        </div>

        <div
          class="bg-background-light dark:bg-background-dark/30 py-3 px-8 border-t border-border-light dark:border-border-dark text-center"
        >
          <slot name="footer" />
        </div>
      </div>

      <div class="mt-8 text-center">
        <p
          class="text-xs text-text-muted-light dark:text-text-muted-dark font-medium opacity-80"
        >
          Student Management System v1.0
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Copy class .geo-pattern vào đây */
.geo-pattern {
  background-image: radial-gradient(
      circle at 10% 20%,
      rgba(244, 157, 37, 0.05) 0%,
      transparent 20%
    ),
    radial-gradient(
      circle at 90% 80%,
      rgba(244, 157, 37, 0.05) 0%,
      transparent 20%
    ),
    linear-gradient(
      135deg,
      transparent 0%,
      transparent 45%,
      rgba(244, 157, 37, 0.03) 45%,
      rgba(244, 157, 37, 0.03) 55%,
      transparent 55%,
      transparent 100%
    ),
    linear-gradient(
      45deg,
      transparent 0%,
      transparent 45%,
      rgba(244, 157, 37, 0.03) 45%,
      rgba(244, 157, 37, 0.03) 55%,
      transparent 55%,
      transparent 100%
    );
  background-size: 100% 100%, 100% 100%, 60px 60px, 60px 60px;
}
</style>
