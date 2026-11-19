import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { LockKeyhole } from 'lucide-react'
import { AuthCard } from '@/components/layout/AuthCard'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Notice } from '@/components/ui/notice'
import { resetPassword } from '@/lib/auth-api'
import { clearSession } from '@/lib/auth-storage'
import { passwordError } from '@/lib/password-validation'
import type { ApiError } from '@/types/auth'

export function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const [password, setPassword] = useState('')
  const [repeat, setRepeat] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)
  async function submit(event: FormEvent) {
    event.preventDefault()
    const validation = passwordError(password, repeat)
    if (validation) { setError(validation); return }
    setError(''); setLoading(true)
    try {
      await resetPassword(token, password)
      clearSession(); setPassword(''); setRepeat(''); setSuccess(true)
      window.history.replaceState(null, '', window.location.pathname)
    } catch (err) { setError((err as ApiError).message || 'Nie udało się zmienić hasła.') }
    finally { setLoading(false) }
  }
  return <AuthCard icon={<LockKeyhole size={24} />} title={success ? 'Hasło zostało zmienione' : 'Ustaw nowe hasło'} description={success ? 'Dla bezpieczeństwa poprzednie sesje zostały zakończone. Zaloguj się nowym hasłem.' : 'Wybierz nowe hasło do swojego konta. Link możesz wykorzystać tylko raz.'}>
    {success ? <div className="space-y-5"><Notice>Możesz już bezpiecznie wrócić do konta.</Notice><Button asChild className="w-full"><Link to="/logowanie">Przejdź do logowania</Link></Button></div> : !token ? <div className="space-y-5"><Notice variant="error">W tym adresie brakuje linku do resetu. {'Otwórz pełny link z wiadomości e-mail lub poproś o nowy.'}</Notice><Button asChild className="w-full"><Link to="/forgot-password">Poproś o nowy link</Link></Button></div> : <form onSubmit={submit} className="space-y-4">
      <div className="space-y-2"><Label htmlFor="password">Nowe hasło</Label><Input id="password" type="password" autoComplete="new-password" minLength={8} maxLength={72} required value={password} onChange={(event) => setPassword(event.target.value)} aria-describedby="password-hint" /><p id="password-hint" className="text-xs text-muted-foreground">Co najmniej 8 znaków. Użyj unikalnego hasła.</p></div>
      <div className="space-y-2"><Label htmlFor="repeat">Powtórz nowe hasło</Label><Input id="repeat" type="password" autoComplete="new-password" minLength={8} maxLength={72} required value={repeat} onChange={(event) => setRepeat(event.target.value)} /></div>
      {error && <Notice variant="error">{error}</Notice>}
      <Button className="w-full" disabled={loading}>{loading ? 'Zapisywanie…' : 'Zapisz nowe hasło'}</Button>
      <p className="text-center text-sm text-muted-foreground">Link wygasł? <Link to="/forgot-password" className="text-primary hover:underline">Poproś o nowy</Link></p>
    </form>}
  </AuthCard>
}
