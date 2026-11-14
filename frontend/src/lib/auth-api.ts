import { apiFetch } from '@/lib/api'
import type { AuthResponse, MessageResponse } from '@/types/auth'

export function registerUser(payload: {
  email: string
  password: string
  displayName: string
}) {
  return apiFetch<MessageResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, false)
}

export function loginUser(payload: { email: string; password: string }) {
  return apiFetch<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, false)
}

export function fetchCurrentUser() {
  return apiFetch<AuthResponse['user']>('/api/users/me')
}

export function requestPasswordReset(email: string) {
  return apiFetch<MessageResponse>('/api/auth/forgot-password', {
    method: 'POST', body: JSON.stringify({ email }),
  }, false)
}

export function resetPassword(token: string, password: string) {
  return apiFetch<MessageResponse>('/api/auth/reset-password', {
    method: 'POST', body: JSON.stringify({ token, password }),
  }, false)
}

export function confirmEmail(token: string) {
  return apiFetch<MessageResponse>('/api/auth/confirm', {
    method: 'POST', body: JSON.stringify({ token }),
  }, false)
}

export function resendConfirmation(email: string) {
  return apiFetch<MessageResponse>('/api/auth/resend-confirmation', {
    method: 'POST', body: JSON.stringify({ email }),
  }, false)
}

export function requestAccountDeletion(password: string) {
  return apiFetch<MessageResponse>('/api/users/me/deletion-request', {
    method: 'POST', body: JSON.stringify({ password }),
  })
}

export function confirmAccountDeletion(token: string) {
  return apiFetch<MessageResponse>('/api/auth/confirm-deletion', {
    method: 'POST', body: JSON.stringify({ token }),
  }, false)
}
