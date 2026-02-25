import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'
import fs from 'node:fs'
import path from 'node:path'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Load env file based on `mode` in the current working directory.
  const env = loadEnv(mode, process.cwd(), '')
  const __dirname = path.dirname(fileURLToPath(import.meta.url))
  
  return {
    plugins: [
      vue(),
      vueDevTools(),
      tailwindcss(),
      {
        name: 'firebase-sw-config',
        // For production build
        closeBundle() {
          const distDir = path.resolve(__dirname, 'dist');
          const templatePath = path.join(distDir, 'firebase-messaging-sw.js.template');
          const outputPath = path.join(distDir, 'firebase-messaging-sw.js');

          if (fs.existsSync(templatePath)) {
            let content = fs.readFileSync(templatePath, 'utf-8');
            const vars = [
              'VITE_FIREBASE_API_KEY',
              'VITE_FIREBASE_AUTH_DOMAIN',
              'VITE_FIREBASE_PROJECT_ID',
              'VITE_FIREBASE_STORAGE_BUCKET',
              'VITE_FIREBASE_MESSAGING_SENDER_ID',
              'VITE_FIREBASE_APP_ID'
            ];

            vars.forEach(v => {
              content = content.replace(`__${v}__`, env[v] || '');
            });

            fs.writeFileSync(outputPath, content);
            // Optionally remove the template from dist
            // fs.unlinkSync(templatePath);
            console.log('✓ Firebase Service Worker configured for production');
          }
        },
        // For local development
        configureServer(server) {
          server.middlewares.use((req, res, next) => {
            if (req.url === '/firebase-messaging-sw.js') {
              const templatePath = path.resolve(__dirname, 'public/firebase-messaging-sw.js.template');
              
              if (fs.existsSync(templatePath)) {
                let content = fs.readFileSync(templatePath, 'utf-8');
                const vars = [
                  'VITE_FIREBASE_API_KEY',
                  'VITE_FIREBASE_AUTH_DOMAIN',
                  'VITE_FIREBASE_PROJECT_ID',
                  'VITE_FIREBASE_STORAGE_BUCKET',
                  'VITE_FIREBASE_MESSAGING_SENDER_ID',
                  'VITE_FIREBASE_APP_ID'
                ];

                vars.forEach(v => {
                  content = content.replace(`__${v}__`, env[v] || '');
                });

                res.setHeader('Content-Type', 'application/javascript');
                res.end(content);
                return;
              }
            }
            next();
          });
        }
      }
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
