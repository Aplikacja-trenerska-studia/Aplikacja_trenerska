export function loginReturnPath(value: unknown) {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) return '/profil'
  const url = new URL(value, 'http://local.invalid')
  if (url.origin !== 'http://local.invalid') return '/profil'
  if (['/profil', '/cwiczenia', '/plany', '/plany/nowy', '/trening', '/progres', '/wspolpraca', '/trener', '/admin', '/kalendarz', '/statystyki', '/kalkulatory'].includes(url.pathname)
    || /^\/plany\/\d+(?:\/edytuj)?$/.test(url.pathname) || /^\/udostepnione\/[A-Za-z0-9_-]{43}$/.test(url.pathname)) {
    return url.pathname + url.search
  }
  return '/profil'
}
