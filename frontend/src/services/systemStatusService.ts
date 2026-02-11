import { apiFetch } from '@/utils/api'

export interface ComponentStatusDto {
  status: string
  message: string
  latencyMs?: number | null
}

export interface SystemHealthResponse {
  overallStatus: string
  timestamp: string
  api: ComponentStatusDto
  database: ComponentStatusDto
  redis: ComponentStatusDto
  mail: ComponentStatusDto
  minio: ComponentStatusDto
  nginx: ComponentStatusDto
  frontend: ComponentStatusDto
}

export async function getSystemHealthOverall(): Promise<SystemHealthResponse> {
  const response = await apiFetch('/admin/system/health/overall')
  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch system health (${response.status})`)
  }
  const data = await response.json()
  return (data.result || data) as SystemHealthResponse
}

export async function getSystemHealthApi(): Promise<ComponentStatusDto> {
  const response = await apiFetch('/admin/system/health/api')
  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch API status (${response.status})`)
  }
  const data = await response.json()
  return (data.result || data) as ComponentStatusDto
}

export async function getSystemHealthDb(): Promise<ComponentStatusDto> {
  const response = await apiFetch('/admin/system/health/db')
  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch DB status (${response.status})`)
  }
  const data = await response.json()
  return (data.result || data) as ComponentStatusDto
}

export async function getSystemHealthRedis(): Promise<ComponentStatusDto> {
  const response = await apiFetch('/admin/system/health/redis')
  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch Redis status (${response.status})`)
  }
  const data = await response.json()
  return (data.result || data) as ComponentStatusDto
}

export async function getSystemHealthMail(): Promise<ComponentStatusDto> {
  const response = await apiFetch('/admin/system/health/mail')
  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch Mail status (${response.status})`)
  }
  const data = await response.json()
  return (data.result || data) as ComponentStatusDto
}

export async function getSystemHealthMinio(): Promise<ComponentStatusDto> {
  const response = await apiFetch('/admin/system/health/minio')
  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch MinIO status (${response.status})`)
  }
  const data = await response.json()
  return (data.result || data) as ComponentStatusDto
}

export async function getSystemHealthNginx(): Promise<ComponentStatusDto> {
  const response = await apiFetch('/admin/system/health/nginx')
  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch Nginx status (${response.status})`)
  }
  const data = await response.json()
  return (data.result || data) as ComponentStatusDto
}

export async function getSystemHealthFrontend(): Promise<ComponentStatusDto> {
  const response = await apiFetch('/admin/system/health/frontend')
  if (!response.ok) {
    const errorData = await response.json().catch(() => null)
    throw new Error(errorData?.message || `Failed to fetch Frontend status (${response.status})`)
  }
  const data = await response.json()
  return (data.result || data) as ComponentStatusDto
}


