import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Load env file based on `mode` in the current working directory.
  const env = loadEnv(mode, process.cwd(), '')
  
  return {
    plugins: [
      vue(),
      vueDevTools(),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    // --- CẤU HÌNH SERVER LOCAL ---
    server: {
      port: Number(env.VITE_DEV_SERVER_PORT) || 5173,
      proxy: {
        // Khi frontend gọi /api/v1 -> Chuyển hướng sang Backend
        [env.VITE_API_BASE_PATH || '/api/v1']: {
          target: env.VITE_BACKEND_URL || 'http://localhost:6868',
          changeOrigin: true,
          secure: false
        }
      }
    }
  }
})
