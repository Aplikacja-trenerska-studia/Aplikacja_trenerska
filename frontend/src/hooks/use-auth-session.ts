import { useSyncExternalStore } from 'react'

import { AUTH_CHANGED_EVENT } from '@/lib/auth-events'
import { getStoredUser, isLoggedIn } from '@/lib/auth-storage'
import type { UserResponse } from '@/types/auth'

type AuthSnapshot = {
  loggedIn: boolean
  user: UserResponse | null
}

const SERVER_SNAPSHOT: AuthSnapshot = { loggedIn: false, user: null }

let cachedLoggedIn = false
let cachedUserKey = ''
let cachedSnapshot: AuthSnapshot = SERVER_SNAPSHOT

function subscribe(onStoreChange: () => void) {
  window.addEventListener(AUTH_CHANGED_EVENT, onStoreChange)
  window.addEventListener('storage', onStoreChange)
  return () => {
    window.removeEventListener(AUTH_CHANGED_EVENT, onStoreChange)
    window.removeEventListener('storage', onStoreChange)
  }
}

function getSnapshot(): AuthSnapshot {
  const loggedIn = isLoggedIn()
  const user = getStoredUser()
  const userKey = user ? `${user.id}:${user.email}` : ''

  if (loggedIn === cachedLoggedIn && userKey === cachedUserKey) {
    return cachedSnapshot
  }

  cachedLoggedIn = loggedIn
  cachedUserKey = userKey
  cachedSnapshot = { loggedIn, user }
  return cachedSnapshot
}

export function useAuthSession() {
  return useSyncExternalStore(subscribe, getSnapshot, () => SERVER_SNAPSHOT)
}
