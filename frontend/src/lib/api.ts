import { clearSession, getAccessToken } from '@/lib/auth-storage'
import type { ApiError } from '@/types/auth'

export async function apiFetch<T>(path: string, init: RequestInit = {}, authenticated = true): Promise<T> {
  const headers = new Headers(init.headers)
  if (!headers.has('Content-Type') && init.body) {
    headers.set('Content-Type', 'application/json')
  }

  const token = authenticated ? getAccessToken() : null
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(path, { ...init, headers })
  } catch {
    throw { message: 'Nie można połączyć się z serwerem. Sprawdź połączenie i spróbuj ponownie.' } satisfies ApiError
  }

  if (!response.ok) {
    if (response.status === 401 && token && getAccessToken() === token) clearSession()
    let payload: ApiError = { message: 'Wystąpił błąd' }
    try {
      payload = (await response.json()) as ApiError
    } catch {
      // ignore
    }
    throw { ...payload, status: response.status }
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}
