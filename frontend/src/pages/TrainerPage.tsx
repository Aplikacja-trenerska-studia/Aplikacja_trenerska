import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ArrowLeft, Plus, UsersRound } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Notice } from '@/components/ui/notice'
import { ResourceStatus } from '@/components/ui/resource-status'
import { ProgressHistory } from '@/components/progress/ProgressHistory'
import { useApiResource } from '@/hooks/use-api-resource'
import { apiFetch } from '@/lib/api'
import type { ApiError } from '@/types/auth'
import type { PlanSummary } from '@/types/workout'
import type { PlanAssignment, TrainingGroup } from '@/types/trainer'

const selectClass = 'h-10 w-full min-w-0 rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

export function TrainerPage() {
  const groups = useApiResource<TrainingGroup[]>('/api/groups')
  const [params, setParams] = useSearchParams()
  const [name, setName] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const groupId = params.get('grupa')
  async function create(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    setBusy(true); setError('')
    try { const group = await apiFetch<TrainingGroup>('/api/groups', { method: 'POST', body: JSON.stringify({ name: name.trim() }) }); setName(''); groups.reload(); setParams({ grupa: String(group.id) }) }
    catch (err) { setError((err as ApiError).message || 'Nie udało się utworzyć grupy.') }
    finally { setBusy(false) }
  }
  return <div className="space-y-6"><div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Twoi podopieczni</p><h1 className="text-3xl font-semibold tracking-tight">Panel trenera</h1><p className="mt-2 text-sm text-muted-foreground">Twórz grupy, przypisuj plany i sprawdzaj postępy podopiecznych, którzy przyjęli zaproszenie.</p></div>{groupId ? <><Link to="/trener" className="inline-flex items-center gap-2 text-sm font-medium text-primary"><ArrowLeft size={16} aria-hidden="true" />Wszystkie grupy</Link><GroupDetail key={groupId} id={groupId} onDeleted={() => { groups.reload(); setParams({}) }} /></> : <>
    <Card><CardHeader><CardTitle className="text-lg">Nowa grupa</CardTitle></CardHeader><CardContent><form onSubmit={create} className="space-y-4"><div className="space-y-2"><Label htmlFor="group-name">Nazwa grupy</Label><Input id="group-name" value={name} onChange={(event) => setName(event.target.value)} minLength={1} maxLength={120} required disabled={busy} placeholder="np. Grupa początkująca" /></div>{error && <Notice variant="error">{error}</Notice>}<Button type="submit" disabled={busy || !name.trim()}><Plus size={16} aria-hidden="true" />{busy ? 'Tworzenie…' : 'Utwórz grupę'}</Button></form></CardContent></Card>
    <section className="space-y-3" aria-labelledby="groups-heading"><h2 className="text-xl font-semibold" id="groups-heading">Twoje grupy</h2><ResourceStatus loading={groups.loading} error={groups.error} onRetry={groups.reload} label="Wczytywanie grup…" />{!groups.loading && !groups.error && groups.data && (groups.data.length ? <ul className="grid gap-3 sm:grid-cols-2">{groups.data.map((group) => <li key={group.id}><Link to={`/trener?grupa=${group.id}`} className="block h-full rounded-xl border bg-card p-5 shadow-sm hover:border-primary/40"><UsersRound className="mb-3 text-primary" size={24} aria-hidden="true" /><h3 className="break-words text-lg font-semibold">{group.name}</h3><p className="mt-2 text-sm text-muted-foreground">Podopieczni: {group.members.filter((member) => member.status === 'ACCEPTED').length} · Zaproszenia: {group.members.filter((member) => member.status === 'PENDING').length}</p><p className="mt-4 text-sm font-medium text-primary">Otwórz grupę →</p></Link></li>)}</ul> : <p className="rounded-xl border border-dashed p-6 text-sm text-muted-foreground">Utwórz pierwszą grupę, aby zaprosić podopiecznych.</p>)}</section>
    </>}</div>
}

function GroupDetail({ id, onDeleted }: { id: string; onDeleted: () => void }) {
  const group = useApiResource<TrainingGroup>(`/api/groups/${encodeURIComponent(id)}`)
  const plans = useApiResource<PlanSummary[]>('/api/plans')
  const [email, setEmail] = useState('')
  const [planId, setPlanId] = useState('')
  const [userId, setUserId] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [removing, setRemoving] = useState<number | 'group' | null>(null)
  const [progressUser, setProgressUser] = useState<number | null>(null)
  const accepted = group.data?.members.filter((member) => member.status === 'ACCEPTED') || []
  const member = accepted.find((item) => item.userId === progressUser)
  async function action(work: () => Promise<void>) {
    if (busy) return
    setBusy(true); setError(''); setNotice('')
    try { await work() }
    catch (err) { setError((err as ApiError).message || 'Nie udało się zapisać zmiany.') }
    finally { setBusy(false) }
  }
  function invite(event: FormEvent) {
    event.preventDefault()
    void action(async () => { await apiFetch(`/api/groups/${id}/members`, { method: 'POST', body: JSON.stringify({ email: email.trim() }) }); setEmail(''); setNotice('Zaproszenie jest widoczne na koncie podopiecznego w zakładce Współpraca.'); group.reload() })
  }
  function assign(event: FormEvent) {
    event.preventDefault()
    void action(async () => { const assigned = await apiFetch<PlanAssignment[]>(`/api/groups/${id}/assign-plan`, { method: 'POST', body: JSON.stringify({ planId: Number(planId), ...(userId ? { userId: Number(userId) } : {}) }) }); setNotice(`Plan przypisany. Liczba przypisań: ${assigned.length}. Podopieczni znajdą plany w zakładce Współpraca i na liście własnych planów. Ponowne przypisanie nie nadpisuje ich wcześniejszych kopii.`) })
  }
  function remove() {
    void action(async () => {
      await apiFetch(`/api/groups/${id}${removing === 'group' ? '' : `/members/${removing}`}`, { method: 'DELETE' })
      if (removing === 'group') onDeleted()
      else { setProgressUser(null); setRemoving(null); setNotice('Podopieczny usunięty z grupy.'); group.reload() }
    })
  }
  return <div className="space-y-6"><ResourceStatus loading={group.loading} error={group.error} onRetry={group.reload} label="Wczytywanie grupy…" />{!group.loading && !group.error && group.data && <>
    <div className="flex flex-wrap items-start justify-between gap-3"><h2 className="break-words text-2xl font-semibold">{group.data.name}</h2><Button variant="outline" size="sm" disabled={busy} onClick={() => setRemoving('group')}>Usuń grupę</Button></div>
    {error && <Notice variant="error">{error}</Notice>}{notice && <Notice>{notice}</Notice>}
    {removing !== null && <div className="space-y-3 rounded-xl border border-destructive/30 bg-card p-5" role="alert"><p className="text-sm">{removing === 'group' ? 'Usunąć grupę i zakończyć współpracę z jej podopiecznymi? Ich własne kopie planów i wyniki pozostaną zachowane.' : 'Usunąć podopiecznego z grupy? Utracisz dostęp do jego progresu przez tę grupę.'}</p><div className="flex flex-wrap gap-2"><Button disabled={busy} className="bg-destructive hover:bg-destructive/90" onClick={remove}>{busy ? 'Usuwanie…' : 'Potwierdź usunięcie'}</Button><Button disabled={busy} variant="outline" onClick={() => setRemoving(null)}>Anuluj usuwanie</Button></div></div>}
    <Card><CardHeader><CardTitle className="text-lg">Zaproś podopiecznego</CardTitle><p className="text-sm text-muted-foreground">Podaj e-mail aktywnego konta użytkownika. Po przyjęciu zaproszenia udostępni Ci swoje wyniki i pomiary.</p></CardHeader><CardContent><form onSubmit={invite} className="space-y-4"><div className="space-y-2"><Label htmlFor="member-email">E-mail podopiecznego</Label><Input id="member-email" type="email" maxLength={255} required value={email} disabled={busy} onChange={(event) => setEmail(event.target.value)} /></div><Button disabled={busy || !email.trim()} type="submit">Zaproś do grupy</Button></form></CardContent></Card>
    <section className="space-y-3" aria-labelledby="members-heading"><h3 id="members-heading" className="text-xl font-semibold">Podopieczni i zaproszenia</h3>{group.data.members.length ? <ul className="divide-y rounded-xl border bg-card px-4 sm:px-5">{group.data.members.map((item) => <li className="space-y-3 py-4" key={item.userId}><div className="min-w-0"><p className="break-words font-medium">{item.displayName}</p><p className="mt-1 break-all text-xs text-muted-foreground">{item.email}</p><p className="mt-2 text-xs font-medium text-primary">{item.status === 'ACCEPTED' ? 'Współpraca aktywna' : 'Oczekuje na przyjęcie zaproszenia'}</p></div><div className="flex flex-wrap gap-2">{item.status === 'ACCEPTED' && <Button variant="outline" size="sm" disabled={busy} onClick={() => setProgressUser(item.userId)} aria-label={`Pokaż progres: ${item.displayName}`}>Pokaż progres</Button>}<Button variant="ghost" size="sm" disabled={busy} onClick={() => setRemoving(item.userId)} aria-label={`Usuń z grupy: ${item.displayName}`}>Usuń z grupy</Button></div></li>)}</ul> : <p className="rounded-xl border border-dashed p-6 text-sm text-muted-foreground">Grupa nie ma jeszcze podopiecznych.</p>}</section>
    <Card><CardHeader><CardTitle className="text-lg">Przypisz plan</CardTitle><p className="text-sm text-muted-foreground">Każdy podopieczny otrzyma osobną kopię aktualnej wersji planu.</p></CardHeader><CardContent className="space-y-4"><ResourceStatus loading={plans.loading} error={plans.error} onRetry={plans.reload} label="Wczytywanie planów…" />{!plans.loading && !plans.error && (!plans.data?.length ? <p className="text-sm text-muted-foreground">Najpierw <Link to="/plany/nowy" className="text-primary underline">utwórz własny plan</Link>.</p> : !accepted.length ? <p className="text-sm text-muted-foreground">Przypisywanie będzie dostępne, gdy podopieczny przyjmie zaproszenie.</p> : <form onSubmit={assign} className="space-y-4"><fieldset disabled={busy} className="space-y-4"><div className="space-y-2"><Label htmlFor="assignment-plan">Plan do przypisania</Label><select id="assignment-plan" className={selectClass} required value={planId} onChange={(event) => setPlanId(event.target.value)}><option value="">Wybierz plan</option>{plans.data.map((plan) => <option key={plan.id} value={plan.id}>{plan.name}</option>)}</select></div><div className="space-y-2"><Label htmlFor="assignment-member">Odbiorca planu</Label><select id="assignment-member" className={selectClass} value={userId} onChange={(event) => setUserId(event.target.value)}><option value="">Cała grupa ({accepted.length})</option>{accepted.map((item) => <option key={item.userId} value={item.userId}>{item.displayName} ({item.email})</option>)}</select></div><Button type="submit" disabled={busy || !planId}>{busy ? 'Zapisywanie…' : 'Przypisz plan'}</Button></fieldset></form>)}</CardContent></Card>
    {member && <section className="space-y-5 border-t pt-6" aria-labelledby="member-progress-heading"><div className="flex flex-wrap items-center justify-between gap-3"><h3 id="member-progress-heading" className="break-words text-xl font-semibold">Progres: {member.displayName}</h3><Button variant="ghost" size="sm" onClick={() => setProgressUser(null)}>Zamknij progres</Button></div><p className="text-sm text-muted-foreground">Podgląd historii podopiecznego. Możesz odczytywać wyniki i pomiary.</p><ProgressHistory key={member.userId} basePath={`/api/groups/${id}/members/${member.userId}`} canDelete={false} /></section>}
    </>}</div>
}
