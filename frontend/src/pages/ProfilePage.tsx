import { useState, type FormEvent } from 'react'
import { Link, useOutletContext } from 'react-router-dom'
import { KeyRound, ShieldCheck, Trash2, UserRound } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Notice } from '@/components/ui/notice'
import { requestAccountDeletion } from '@/lib/auth-api'
import type { ApiError, UserResponse } from '@/types/auth'

const roles = { USER: 'Użytkownik', TRAINER: 'Trener', ADMIN: 'Administrator' }

export function ProfilePage() {
  const user = useOutletContext<UserResponse>()
  const [showDeletion, setShowDeletion] = useState(false)
  const [password, setPassword] = useState('')
  const [acknowledged, setAcknowledged] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!acknowledged) return
    setError(''); setLoading(true)
    try {
      const response = await requestAccountDeletion(password)
      setMessage(response.message)
      setPassword(''); setShowDeletion(false); setAcknowledged(false)
    } catch (err) { setError((err as ApiError).message || 'Nie udało się wysłać prośby o usunięcie konta.') }
    finally { setLoading(false) }
  }
  return <div className="space-y-5">
    <div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Twoje konto</p><h1 className="text-3xl font-semibold tracking-tight">Mój profil</h1><p className="mt-2 text-sm text-muted-foreground">Dane konta i ustawienia bezpieczeństwa.</p></div>

    <Card>
      <CardHeader><div className="mb-3 flex items-center gap-4"><div className="flex size-14 shrink-0 items-center justify-center rounded-2xl bg-primary/8 text-primary"><UserRound size={28} aria-hidden="true" /></div><div className="min-w-0"><CardTitle className="break-words text-xl">{user.displayName}</CardTitle><CardDescription className="mt-1">{roles[user.role]}</CardDescription></div></div></CardHeader>
      <CardContent><dl className="space-y-4 text-sm"><div><dt className="text-muted-foreground">Adres e-mail</dt><dd className="mt-1 break-all font-medium">{user.email}</dd></div></dl><p className="mt-4 flex items-center gap-2 text-xs text-muted-foreground"><ShieldCheck size={15} aria-hidden="true" />Konto zabezpieczone hasłem</p></CardContent>
    </Card>
    <Card><CardHeader><CardTitle className="flex items-center gap-2 text-lg"><KeyRound size={19} aria-hidden="true" />Hasło</CardTitle><CardDescription className="leading-relaxed">{'Chcesz ustawić nowe hasło? Wyślemy Ci bezpieczny link na adres e-mail konta.'}</CardDescription></CardHeader><CardContent><Button variant="outline" asChild className="w-full"><Link to="/forgot-password">Zresetuj hasło</Link></Button></CardContent></Card>
    <Card className="border-destructive/25"><CardHeader><CardTitle className="flex items-center gap-2 text-lg"><Trash2 size={19} className="text-destructive" aria-hidden="true" />Usunięcie konta</CardTitle><CardDescription className="leading-relaxed">Trwale usuniesz konto, własne plany, wyniki treningów, pomiary i powiązania z grupami. Najpierw poprosimy o hasło, a następnie o potwierdzenie przez e-mail.</CardDescription></CardHeader><CardContent className="space-y-4">
      {user.role === 'TRAINER' && <p className="text-sm leading-relaxed text-muted-foreground">Twoje grupy zostaną usunięte. Kopie planów przekazane wcześniej podopiecznym pozostaną na ich kontach.</p>}
      {message && <Notice variant="mail">{message} Konto pozostaje aktywne, dopóki nie potwierdzisz usunięcia za pomocą linku.</Notice>}
      {showDeletion ? <form onSubmit={submit} className="space-y-4">
        <div className="space-y-2"><Label htmlFor="deletion-password">Aktualne hasło</Label><Input id="deletion-password" type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} /></div>
        <label className="flex cursor-pointer items-start gap-3 text-sm leading-relaxed"><input type="checkbox" className="mt-1 size-4 shrink-0 accent-primary" required checked={acknowledged} onChange={(event) => setAcknowledged(event.target.checked)} /><span>Rozumiem, że po potwierdzeniu za pomocą linku moje konto zostanie bezpowrotnie usunięte.</span></label>
        {error && <Notice variant="error">{error}</Notice>}
        <Button disabled={loading || !acknowledged} className="h-auto min-h-10 w-full whitespace-normal bg-destructive text-center hover:bg-destructive/90">{loading ? 'Wysyłanie…' : 'Wyślij e-mail z potwierdzeniem usunięcia'}</Button>
        <Button type="button" variant="ghost" className="w-full" disabled={loading} onClick={() => { setShowDeletion(false); setPassword(''); setError(''); setAcknowledged(false) }}>Anuluj</Button>
      </form> : <Button variant="outline" className="w-full border-destructive/30 text-destructive hover:bg-destructive/5 hover:text-destructive" onClick={() => { setMessage(''); setShowDeletion(true) }}>Chcę usunąć konto</Button>}
    </CardContent></Card>
  </div>
}
