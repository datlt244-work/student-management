/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_DEV_SERVER_PORT: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_BACKEND_URL: string
  readonly VITE_BACKEND_PORT: string
  readonly VITE_API_BASE_PATH: string
  readonly VITE_NGINX_SERVER_NAME: string
  readonly VITE_NGINX_LISTEN_PORT: string
  readonly VITE_BACKEND_CONTAINER_NAME: string
  readonly VITE_BACKEND_CONTAINER_PORT: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
