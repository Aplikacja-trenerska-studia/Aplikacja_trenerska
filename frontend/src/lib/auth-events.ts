export const AUTH_CHANGED_EVENT = 'aplikacja-trenerska-auth-changed'

export function notifyAuthChanged() {
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT))
}
