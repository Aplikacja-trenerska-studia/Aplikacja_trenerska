import { useSyncExternalStore } from 'react'

import { AUTH_CHANGED_EVENT } from '@/lib/auth-events'
import { getAccessToken, getStoredUser, isLoggedIn } from '@/lib/auth-storage'
import { tokenExpiresAt } from '@/lib/token-expiry'
import type { UserResponse } from '@/types/auth'

type AuthSnapshot = {
  loggedIn: boolean
  user: UserResponse | null
  token: string | null
}

const SERVER_SNAPSHOT: AuthSnapshot = { loggedIn: false, user: null, token: null }

let cachedLoggedIn = false
let cachedUserKey = ''
let cachedToken: string | null = null
let cachedSnapshot: AuthSnapshot = SERVER_SNAPSHOT

function subscribe(onStoreChange: () => void) {
  let timer: ReturnType<typeof setTimeout>
  function refresh() {
    clearTimeout(timer)
    const remaining = tokenExpiresAt(getAccessToken() ?? '') - Date.now()
    if (remaining > 0) timer = setTimeout(onStoreChange, Math.min(remaining + 10, 2_147_483_647))
    onStoreChange()
  }
  refresh()
  window.addEventListener(AUTH_CHANGED_EVENT, refresh)
  window.addEventListener('storage', refresh)
  window.addEventListener('focus', refresh)
  return () => {
    clearTimeout(timer)
    window.removeEventListener(AUTH_CHANGED_EVENT, refresh)
    window.removeEventListener('storage', refresh)
    window.removeEventListener('focus', refresh)
  }
}

function getSnapshot(): AuthSnapshot {
  const loggedIn = isLoggedIn()
  const user = getStoredUser()
  const token = getAccessToken()
  const userKey = JSON.stringify(user)

  if (loggedIn === cachedLoggedIn && userKey === cachedUserKey && token === cachedToken) {
    return cachedSnapshot
  }

  cachedLoggedIn = loggedIn
  cachedUserKey = userKey
  cachedToken = token
  cachedSnapshot = { loggedIn, user, token }
  return cachedSnapshot
}

export function useAuthSession() {
  return useSyncExternalStore(subscribe, getSnapshot, () => SERVER_SNAPSHOT)
}
