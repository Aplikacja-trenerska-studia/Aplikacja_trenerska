import { apiFetch } from '@/lib/api'
import type { AuthResponse } from '@/types/auth'

export function registerUser(payload: {
  email: string
  password: string
  displayName: string
}) {
  return apiFetch<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function loginUser(payload: { email: string; password: string }) {
  return apiFetch<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function fetchCurrentUser() {
  return apiFetch<AuthResponse['user']>('/api/users/me')
}
