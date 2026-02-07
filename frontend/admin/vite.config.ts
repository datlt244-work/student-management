import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      // @shared trỏ về shared module (đặt trước @ để tránh bị match nhầm)
      '@shared': fileURLToPath(new URL('../shared', import.meta.url)),

      // Chỉ match import dạng "@/" để không ảnh hưởng "@shared"
      '@/': `${fileURLToPath(new URL('./src', import.meta.url))}/`,
    },
  },
})
