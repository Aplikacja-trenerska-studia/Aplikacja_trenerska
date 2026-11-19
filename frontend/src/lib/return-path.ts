export function loginReturnPath(value: unknown) {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) return '/profil'
  const url = new URL(value, 'http://local.invalid')
  if (url.origin !== 'http://local.invalid') return '/profil'
  if (['/profil', '/cwiczenia', '/plany', '/plany/nowy', '/trening', '/progres', '/wspolpraca', '/trener'].includes(url.pathname) || /^\/plany\/\d+(?:\/edytuj)?$/.test(url.pathname)) {
    return url.pathname + url.search
  }
  return '/profil'
}
