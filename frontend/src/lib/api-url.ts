export function normalizeApiOrigin(value = '', requireHttps = false): string {
  const raw = value.trim()
  if (!raw) return ''

  let url: URL
  try {
    url = new URL(raw)
  } catch {
    throw new Error('VITE_API_URL musi być pełnym adresem backendu, np. https://backend.vercel.app.')
  }

  const loopback = ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname)
  if (url.protocol !== 'https:' && !(url.protocol === 'http:' && loopback && !requireHttps)) {
    throw new Error('VITE_API_URL wymaga HTTPS. HTTP jest dozwolone tylko lokalnie poza Vercel.')
  }
  if (url.username || url.password || url.pathname !== '/' || raw.includes('?') || raw.includes('#')) {
    throw new Error('VITE_API_URL musi zawierać tylko origin backendu, bez /api, ścieżki, danych logowania ani parametrów.')
  }

  return url.origin
}

export function resolveApiUrl(path: string, apiOrigin = ''): string {
  // Requests always stay in our API namespace, including when a JWT is attached.
  const parsed = new URL(path, 'https://local.invalid')
  if (!path.startsWith('/') || path.startsWith('//') || path.includes('\\') || path.includes('#')
    || !(parsed.pathname === '/api' || parsed.pathname.startsWith('/api/'))) {
    throw new Error('Żądanie musi wskazywać ścieżkę /api aplikacji.')
  }
  return `${normalizeApiOrigin(apiOrigin)}${path}`
}
