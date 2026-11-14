// This is only a client-side expiry hint. The API verifies the signature and session.
export function tokenExpiresAt(token: string): number {
  try {
    const payload = token.split('.')[1]
    if (!payload) return 0
    const claims = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    return typeof claims.exp === 'number' && Number.isFinite(claims.exp) ? claims.exp * 1000 : 0
  } catch {
    return 0
  }
}
