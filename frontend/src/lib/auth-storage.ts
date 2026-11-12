import { notifyAuthChanged } from '@/lib/auth-events'
import type { UserResponse } from '@/types/auth'

const TOKEN_KEY = 'aplikacja_trenerska_access_token'
const USER_KEY = 'aplikacja_trenerska_user'

export function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setSession(accessToken: string, user: UserResponse) {
  localStorage.setItem(TOKEN_KEY, accessToken)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  notifyAuthChanged()
}

export function getStoredUser(): UserResponse | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserResponse
  } catch {
    return null
  }
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  notifyAuthChanged()
}

export function isLoggedIn(): boolean {
  return Boolean(getAccessToken())
}
