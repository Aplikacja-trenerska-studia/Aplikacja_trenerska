import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { loginUser } from '@/lib/auth-api'
import { setSession } from '@/lib/auth-storage'
import type { ApiError } from '@/types/auth'
import { Notice } from '@/components/ui/notice'
import { loginReturnPath } from '@/lib/return-path'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const auth = await loginUser({ email, password })
      setSession(auth.accessToken, auth.user)
      navigate(loginReturnPath(location.state?.from), { replace: true })
    } catch (err) {
      const apiErr = err as ApiError
      setError(apiErr.message ?? 'Logowanie nie powiodło się')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Logowanie</CardTitle>
        <CardDescription>Zaloguj się, aby kontynuować treningi.</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={onSubmit}>
          <div className="space-y-2">
            <Label htmlFor="email">E-mail</Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">Hasło</Label>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
            />
          </div>
          <Link to="/forgot-password" className="self-end text-sm text-primary hover:underline">Nie pamiętasz hasła?</Link>
          {error ? <Notice variant="error">{error}</Notice> : null}
          <Button type="submit" disabled={loading} className="w-full">
            {loading ? 'Logowanie…' : 'Zaloguj się'}
          </Button>
          <p className="text-center text-sm text-muted-foreground">
            Nie masz konta?{' '}
            <Link to="/rejestracja" className="text-primary underline-offset-4 hover:underline">
              Zarejestruj się
            </Link>
          </p>
          <p className="text-center text-sm text-muted-foreground">Konto nieaktywne? <Link to="/confirm-email" className="text-primary hover:underline">Potwierdź e-mail</Link></p>
        </form>
      </CardContent>
    </Card>
  )
}
