import { useEffect, useState } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { useAuthSession } from '@/hooks/use-auth-session'
import { fetchCurrentUser } from '@/lib/auth-api'
import { getAccessToken, setSession } from '@/lib/auth-storage'
import type { ApiError, UserResponse } from '@/types/auth'

export function ProtectedRoute() {
  const { loggedIn, token } = useAuthSession()
  const location = useLocation()
  const [verified, setVerified] = useState<{ token: string; user: UserResponse } | null>(null)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    if (!loggedIn || !token) return
    let active = true
    fetchCurrentUser().then((user) => {
      if (active && getAccessToken() === token) {
        setSession(token, user)
        setVerified({ token, user })
      }
    }).catch((err: ApiError) => {
      if (active) setError(err.message ?? 'Nie udało się sprawdzić sesji.')
    })
    return () => { active = false }
  }, [loggedIn, token, attempt])

  if (!loggedIn) {
    return <Navigate to="/logowanie" state={{ from: location.pathname + location.search }} replace />
  }
  if (!verified || verified.token !== token) {
    return <div className="rounded-xl border bg-card p-6 text-center" aria-live="polite">
      <p className="text-sm text-muted-foreground">{error || 'Sprawdzanie sesji…'}</p>
      {error && <Button className="mt-4" variant="outline" onClick={() => { setError(''); setAttempt((value) => value + 1) }}>Spróbuj ponownie</Button>}
    </div>
  }
  return <Outlet context={verified.user} />
}
