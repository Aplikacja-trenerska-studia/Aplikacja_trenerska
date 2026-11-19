import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { MailCheck, ShieldAlert } from 'lucide-react'
import { AuthCard } from '@/components/layout/AuthCard'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Notice } from '@/components/ui/notice'
import { confirmAccountDeletion, confirmEmail, resendConfirmation } from '@/lib/auth-api'
import { clearSession } from '@/lib/auth-storage'
import type { ApiError } from '@/types/auth'

export function ConfirmationPage({ kind }: { kind: 'email' | 'deletion' }) {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const deletion = kind === 'deletion'
  const [status, setStatus] = useState<'ready' | 'loading' | 'success' | 'error'>('ready')
  const [error, setError] = useState('')
  const [email, setEmail] = useState('')
  const [resent, setResent] = useState('')
  const [resending, setResending] = useState(false)
  async function confirm() {
    setStatus('loading'); setError('')
    try {
      await (deletion ? confirmAccountDeletion(token) : confirmEmail(token))
      if (deletion) clearSession()
      setStatus('success')
      window.history.replaceState(null, '', window.location.pathname)
    } catch (err) { setError((err as ApiError).message || 'Nie udało się potwierdzić operacji.'); setStatus('error') }
  }
  async function resend(event: FormEvent) {
    event.preventDefault(); setResending(true); setError('')
    try {
      const response = await resendConfirmation(email.trim())
      setResent(response.message)
    }
    catch (err) { setError((err as ApiError).message || 'Nie udało się wysłać wiadomości.') }
    finally { setResending(false) }
  }
  const success = status === 'success'
  return <AuthCard icon={deletion ? <ShieldAlert size={24} /> : <MailCheck size={24} />} title={success ? (deletion ? 'Konto zostało usunięte' : 'Adres e-mail potwierdzony') : (deletion ? 'Potwierdź usunięcie konta' : 'Potwierdź swój adres e-mail')} description={success ? (deletion ? 'Twoje konto i powiązane dane zostały trwale usunięte.' : 'Twoje konto jest aktywne. Możesz się zalogować i rozpocząć korzystanie z aplikacji.') : (deletion ? 'To ostatni krok. Usunięcie konta jest nieodwracalne i zakończy wszystkie aktywne sesje.' : 'Kliknij przycisk poniżej, aby aktywować konto i dokończyć rejestrację.')}>
    <div className="space-y-5">

      {success ? <><Notice>{deletion ? 'Dziękujemy za wspólne treningi.' : 'Wszystko gotowe. Do zobaczenia na treningu!'}</Notice><Button asChild className="w-full"><Link to={deletion ? '/' : '/logowanie'}>{deletion ? 'Wróć na stronę główną' : 'Przejdź do logowania'}</Link></Button></> : <>
        {!token && <Notice variant="error">Brakuje tokenu potwierdzenia. Otwórz pełny link z wiadomości e-mail.{deletion ? ' Nową prośbę o usunięcie wyślesz z profilu.' : ''}</Notice>}
        {deletion && <p className="text-sm leading-relaxed text-muted-foreground">Usunięcie obejmie Twoje plany, wyniki treningów, pomiary i powiązania z grupami. Kopie planów przekazane innym użytkownikom pozostaną na ich kontach.</p>}
        {error && <Notice variant="error">{error}</Notice>}
        {token && <Button onClick={confirm} disabled={status === 'loading'} className={`w-full ${deletion ? 'bg-destructive hover:bg-destructive/90' : ''}`}>{status === 'loading' ? 'Potwierdzanie…' : deletion ? 'Tak, trwale usuń moje konto' : 'Potwierdź adres e-mail'}</Button>}
        {deletion ? <Button asChild variant="outline" className="w-full"><Link to="/profil">Zachowaj konto i wróć do profilu</Link></Button> : <div className="border-t pt-5">
          <p className="mb-3 text-sm text-muted-foreground">Link wygasł lub wiadomość nie dotarła? Poproś o nową.</p>
          {resent ? <Notice variant="mail">{resent}</Notice> : <form onSubmit={resend} className="space-y-3"><Label htmlFor="confirmation-email">E-mail</Label><Input id="confirmation-email" type="email" autoComplete="email" maxLength={255} required value={email} onChange={(event) => setEmail(event.target.value)} /><Button className="h-auto min-h-10 w-full whitespace-normal text-center" variant="outline" disabled={resending}>{resending ? 'Wysyłanie…' : 'Wyślij nowy link potwierdzający'}</Button></form>}
        </div>}
      </>}
    </div>
  </AuthCard>
}
