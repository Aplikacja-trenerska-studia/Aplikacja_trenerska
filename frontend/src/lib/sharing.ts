const TOKEN = /^[A-Za-z0-9_-]{43}$/

export function isShareToken(value: string | undefined | null): value is string {
  return typeof value === 'string' && TOKEN.test(value)
}

export function shareUrl(token: string, origin: string): string {
  return `${origin.replace(/\/+$/, '')}/udostepnione/${token}`
}
