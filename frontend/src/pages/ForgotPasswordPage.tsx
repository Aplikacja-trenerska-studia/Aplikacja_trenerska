import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, KeyRound } from 'lucide-react'
import { AuthCard } from '@/components/layout/AuthCard'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Notice } from '@/components/ui/notice'
import { requestPasswordReset } from '@/lib/auth-api'
import type { ApiError } from '@/types/auth'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const response = await requestPasswordReset(email.trim())
      setMessage(response.message)
    }
    catch (err) { setError((err as ApiError).message || 'Nie udało się wysłać prośby.') }
    finally { setLoading(false) }
  }
  return <AuthCard icon={<KeyRound size={24} />} title="Nie pamiętasz hasła?" description={'Podaj adres e-mail powiązany z kontem. Wyślemy na niego link do ustawienia nowego hasła.'}>
    <div className="space-y-5">

      {message ? <><Notice variant="mail">{message}</Notice><p className="text-sm leading-relaxed text-muted-foreground">{'Sprawdź także folder spam. Link jest jednorazowy; termin ważności znajdziesz w wiadomości.'}</p><Button variant="outline" className="w-full" onClick={() => setMessage('')}>{'Wyślij ponownie lub zmień adres'}</Button></> : <form onSubmit={submit} className="space-y-4">
        <div className="space-y-2"><Label htmlFor="email">E-mail</Label><Input id="email" type="email" autoComplete="email" maxLength={255} required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="ty@przyklad.pl" /></div>
        {error && <Notice variant="error">{error}</Notice>}
        <Button className="w-full" disabled={loading}>{loading ? 'Wysyłanie…' : 'Wyślij link do resetu hasła'}</Button>
      </form>}
      <Link to="/logowanie" className="flex items-center justify-center gap-2 text-sm text-primary hover:underline"><ArrowLeft size={15} aria-hidden="true" />Wróć do logowania</Link>
    </div>
  </AuthCard>
}
