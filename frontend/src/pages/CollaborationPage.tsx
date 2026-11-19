import { useState } from 'react'
import { Link } from 'react-router-dom'
import { UsersRound } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Notice } from '@/components/ui/notice'
import { ResourceStatus } from '@/components/ui/resource-status'
import { useApiResource } from '@/hooks/use-api-resource'
import { apiFetch } from '@/lib/api'
import { formatMoment } from '@/lib/progress'
import type { GroupInvitation, PlanAssignment } from '@/types/trainer'
import type { ApiError } from '@/types/auth'

export function CollaborationPage() {
  const invitations = useApiResource<GroupInvitation[]>('/api/my-invitations')
  const assignments = useApiResource<PlanAssignment[]>('/api/my-assignments')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [leaving, setLeaving] = useState<number | null>(null)
  async function act(groupId: number, accept: boolean) {
    if (busy) return
    setBusy(true); setError(''); setNotice('')
    try {
      await apiFetch(`/api/my-invitations/${groupId}${accept ? '/accept' : ''}`, { method: accept ? 'POST' : 'DELETE' })
      setNotice(accept ? 'Zaproszenie przyjęte. Trener może teraz przypisywać Ci plany i oglądać Twój progres.' : 'Współpraca została zakończona. Trener utracił dostęp do Twojego progresu w tej grupie.')
      setLeaving(null); invitations.reload(); assignments.reload()
    } catch (err) { setError((err as ApiError).message || 'Nie udało się zmienić współpracy.') }
    finally { setBusy(false) }
  }
  return <div className="space-y-6"><div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Trenuj z opieką</p><h1 className="text-3xl font-semibold tracking-tight">Współpraca z trenerem</h1><p className="mt-2 text-sm leading-relaxed text-muted-foreground">Przyjmuj zaproszenia do grup i korzystaj z planów od trenera. Akceptując zaproszenie, udostępniasz trenerowi historię swoich wyników i pomiarów. Możesz zakończyć współpracę w dowolnym momencie.</p></div>
    {error && <Notice variant="error">{error}</Notice>}{notice && <Notice>{notice}</Notice>}
    <section className="space-y-3" aria-labelledby="invitations-heading"><h2 id="invitations-heading" className="text-xl font-semibold">Twoje grupy i zaproszenia</h2><ResourceStatus loading={invitations.loading} error={invitations.error} onRetry={invitations.reload} label="Wczytywanie zaproszeń…" />{!invitations.loading && !invitations.error && invitations.data && (invitations.data.length ? <ul className="space-y-3">{invitations.data.map((item) => <li key={item.groupId}><Card><CardHeader><div className="flex items-start gap-3"><UsersRound className="mt-1 shrink-0 text-primary" size={22} aria-hidden="true" /><div className="min-w-0"><CardTitle className="break-words text-lg">{item.groupName}</CardTitle><p className="mt-1 break-words text-sm text-muted-foreground">Trener: {item.trainerName}</p></div></div></CardHeader><CardContent className="space-y-4"><p className="text-xs font-medium text-primary">{item.status === 'ACCEPTED' ? 'Współpraca aktywna' : 'Oczekuje na Twoją zgodę'}</p>{leaving === item.groupId ? <div className="space-y-3 rounded-lg border p-4"><p className="text-sm">{item.status === 'ACCEPTED' ? 'Zakończyć współpracę w tej grupie? Trener utraci dostęp do Twojego progresu przez tę grupę.' : 'Odrzucić zaproszenie do tej grupy?'}</p><div className="flex flex-wrap gap-2"><Button disabled={busy} onClick={() => act(item.groupId, false)}>{busy ? 'Zapisywanie…' : 'Potwierdź'}</Button><Button variant="ghost" disabled={busy} onClick={() => setLeaving(null)}>Anuluj</Button></div></div> : <div className="flex flex-wrap gap-2">{item.status === 'PENDING' && <Button disabled={busy} onClick={() => act(item.groupId, true)}>Przyjmij zaproszenie</Button>}<Button disabled={busy} variant="outline" onClick={() => setLeaving(item.groupId)}>{item.status === 'ACCEPTED' ? 'Zakończ współpracę' : 'Odrzuć zaproszenie'}</Button></div>}</CardContent></Card></li>)}</ul> : <p className="rounded-xl border border-dashed p-6 text-sm text-muted-foreground">Nie masz jeszcze zaproszeń. Trener może zaprosić Cię, wpisując adres e-mail Twojego konta.</p>)}</section>
    <section className="space-y-3" aria-labelledby="assignments-heading"><h2 id="assignments-heading" className="text-xl font-semibold">Plany od trenera</h2><p className="text-sm text-muted-foreground">Otrzymujesz własną kopię planu. Twoje zmiany nie zmieniają planu trenera.</p><ResourceStatus loading={assignments.loading} error={assignments.error} onRetry={assignments.reload} label="Wczytywanie przypisanych planów…" />{!assignments.loading && !assignments.error && assignments.data && (assignments.data.length ? <ul className="space-y-3">{assignments.data.map((item) => <li key={item.id}><Card><CardHeader><CardTitle className="break-words text-lg">{item.planName}</CardTitle><p className="break-words text-sm text-muted-foreground">{item.trainerName} · {item.groupName}</p></CardHeader><CardContent className="space-y-4"><p className="text-xs text-muted-foreground">Przypisano: {formatMoment(item.assignedAt)}</p>{item.planId ? <div className="flex flex-wrap gap-2"><Button asChild><Link to={`/trening?planId=${item.planId}`}>Wykonaj trening</Link></Button><Button asChild variant="outline"><Link to={`/plany/${item.planId}`}>Zobacz plan</Link></Button></div> : <p className="text-sm text-muted-foreground">Kopia planu została usunięta.</p>}</CardContent></Card></li>)}</ul> : <p className="rounded-xl border border-dashed p-6 text-sm text-muted-foreground">Nie masz jeszcze planów przypisanych przez trenera.</p>)}</section>
  </div>
}
