import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { registerUser } from '@/lib/auth-api'
import { Notice } from '@/components/ui/notice'
import { MailDemoNotice } from '@/components/ui/mail-demo-notice'
import { isMailDemo } from '@/lib/mail-mode'
import { passwordError } from '@/lib/password-validation'
import type { ApiError } from '@/types/auth'

export function RegisterPage() {
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const validation = passwordError(password)
    if (validation) { setError(validation); return }
    setLoading(true)
    try {
      const response = await registerUser({ email: email.trim(), password, displayName: displayName.trim() })
      setMessage(isMailDemo ? 'Konto zostało utworzone. Aktywuj je za pomocą linku udostępnionego przez prowadzącego prezentację.' : response.message)
      setPassword('')
    } catch (err) {
      const apiErr = err as ApiError
      setError(apiErr.message ?? 'Rejestracja nie powiodła się')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Rejestracja</CardTitle>
        <CardDescription>{message ? 'Jeszcze jeden krok — potwierdź adres e-mail.' : 'Utwórz konto w aplikacji trenerskiej.'}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-5">
        <MailDemoNotice />
        {message ? <div className="space-y-5"><Notice variant="mail">{message}</Notice><p className="text-sm text-muted-foreground">{isMailDemo ? 'Otwórz otrzymany link aktywacyjny, a następnie potwierdź operację.' : 'Sprawdź skrzynkę odbiorczą i spam, a następnie otwórz link aktywacyjny.'}</p><Button className="h-auto min-h-10 w-full whitespace-normal text-center" asChild variant="outline"><Link to="/confirm-email">{isMailDemo ? 'Poproś o nowy link aktywacyjny' : 'Wyślij ponownie link aktywacyjny'}</Link></Button><Button className="w-full" asChild><Link to="/logowanie">Przejdź do logowania</Link></Button></div> : <form className="flex flex-col gap-4" onSubmit={onSubmit}>
          <div className="space-y-2">
            <Label htmlFor="displayName">Imię lub nazwa wyświetlana</Label>
            <Input
              id="displayName"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              required
              minLength={2}
              maxLength={120}
              // App-specific nicknames should not trigger browser profile autofill (Brave #59759).
              autoComplete="off"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="email">E-mail</Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              maxLength={255}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">Hasło (min. 8 znaków)</Label>
            <Input
              id="password"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
              maxLength={72}
            />
          </div>
          {error ? <Notice variant="error">{error}</Notice> : null}
          <Button type="submit" disabled={loading} className="w-full">
            {loading ? 'Tworzenie konta…' : 'Załóż konto'}
          </Button>
          <p className="text-center text-sm text-muted-foreground">
            Masz już konto?{' '}
            <Link to="/logowanie" className="text-primary underline-offset-4 hover:underline">
              Zaloguj się
            </Link>
          </p>
        </form>}
      </CardContent>
    </Card>
  )
}
