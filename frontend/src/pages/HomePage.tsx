import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuthSession } from '@/hooks/use-auth-session'
import { fetchCurrentUser } from '@/lib/auth-api'
import { clearSession } from '@/lib/auth-storage'
import type { UserResponse } from '@/types/auth'

export function HomePage() {
  const navigate = useNavigate()
  const { user: storedUser } = useAuthSession()
  const [user, setUser] = useState<UserResponse | null>(storedUser)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchCurrentUser()
      .then(setUser)
      .catch(() => {
        setError('Sesja wygasła — zaloguj się ponownie.')
        clearSession()
      })
  }, [])

  return (
    <Card className="flex-1">
      <CardHeader>
        <CardTitle>Witaj{user ? `, ${user.displayName}` : ''}!</CardTitle>
        <CardDescription>
          Jesteś zalogowany. W kolejnych sprintach pojawią się plany treningowe i progres.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-3 text-sm">
        {error ? <p className="text-destructive">{error}</p> : null}
        {user ? (
          <dl className="space-y-2 rounded-lg border bg-muted/40 p-4">
            <div>
              <dt className="text-muted-foreground">E-mail</dt>
              <dd className="font-medium">{user.email}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Rola</dt>
              <dd className="font-medium">{user.role}</dd>
            </div>
          </dl>
        ) : null}
        <Button
          type="button"
          variant="outline"
          onClick={() => {
            clearSession()
            navigate('/logowanie', { replace: true })
          }}
        >
          Wyloguj
        </Button>
      </CardContent>
    </Card>
  )
}
