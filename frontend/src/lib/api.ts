import { getAccessToken } from '@/lib/auth-storage'
import type { ApiError } from '@/types/auth'

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (!headers.has('Content-Type') && init.body) {
    headers.set('Content-Type', 'application/json')
  }

  const token = getAccessToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(path, { ...init, headers })

  if (!response.ok) {
    let payload: ApiError = { message: 'Wystąpił błąd' }
    try {
      payload = (await response.json()) as ApiError
    } catch {
      // ignore
    }
    throw payload
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}
