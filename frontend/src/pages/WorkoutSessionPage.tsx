import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Check, Dumbbell } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Notice } from '@/components/ui/notice'
import { ResourceStatus } from '@/components/ui/resource-status'
import { useApiResource } from '@/hooks/use-api-resource'
import { apiFetch } from '@/lib/api'
import { decimal, localDateTime, sessionDraft, toInstant, type SetDraft } from '@/lib/progress'
import type { ApiError } from '@/types/auth'
import type { PlanSummary, WorkoutPlan } from '@/types/workout'

const selectClass = 'h-10 w-full min-w-0 rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

export function WorkoutSessionPage() {
  const plans = useApiResource<PlanSummary[]>('/api/plans')
  const [params, setParams] = useSearchParams()
  const planId = params.get('planId') || ''
  return <div className="space-y-6">
    <div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Dziennik treningowy</p><h1 className="text-3xl font-semibold tracking-tight">Wykonaj trening</h1><p className="mt-2 text-sm text-muted-foreground">Wybierz dzień planu i zapisz rzeczywiście wykonane serie. Odznacz serie, które pomijasz.</p></div>
    <ResourceStatus loading={plans.loading} error={plans.error} onRetry={plans.reload} label="Wczytywanie planów…" />
    {!plans.loading && !plans.error && plans.data && (plans.data.length ? <div className="space-y-2"><Label htmlFor="session-plan">Plan treningowy</Label><select id="session-plan" className={selectClass} value={planId} onChange={(event) => setParams(event.target.value ? { planId: event.target.value } : {})}><option value="">Wybierz plan</option>{plans.data.map((plan) => <option key={plan.id} value={plan.id}>{plan.name}</option>)}</select></div> : <Card><CardContent className="space-y-4 py-8 text-center"><Dumbbell className="mx-auto text-primary" aria-hidden="true" /><p>Najpierw utwórz plan lub przyjmij plan od trenera.</p><Button asChild><Link to="/plany/nowy">Utwórz plan</Link></Button></CardContent></Card>)}
    {planId && <SelectedPlan key={planId} planId={planId} initialDay={params.get('dayId')} scheduleId={Number(params.get('scheduleId')) || null} />}
  </div>
}

function SelectedPlan({ planId, initialDay, scheduleId }: { planId: string; initialDay: string | null; scheduleId: number | null }) {
  const plan = useApiResource<WorkoutPlan>(`/api/plans/${encodeURIComponent(planId)}`)
  return <><ResourceStatus loading={plan.loading} error={plan.error} onRetry={plan.reload} label="Wczytywanie dni planu…" />{!plan.loading && !plan.error && plan.data && <SessionForm plan={plan.data} initialDay={initialDay} scheduleId={scheduleId} />}</>
}

function SessionForm({ plan, initialDay, scheduleId }: { plan: WorkoutPlan; initialDay: string | null; scheduleId: number | null }) {
  const [dayId, setDayId] = useState<number | undefined>(plan.days.find((day) => String(day.id) === initialDay)?.id || plan.days[0]?.id)
  const day = plan.days.find((item) => item.id === dayId)
  const [rows, setRows] = useState<SetDraft[]>(day ? sessionDraft(day) : [])
  const [time, setTime] = useState(localDateTime())
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [scheduleNote, setScheduleNote] = useState('')
  function update(index: number, change: Partial<SetDraft>) { setRows((current) => current.map((row, i) => i === index ? { ...row, ...change } : row)) }
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (saving || saved || !day) return
    setError('')
    const instant = toInstant(time)
    if (!instant) { setError('Wpisz prawidłową datę i godzinę treningu.'); return }
    const selected = rows.filter((row) => row.included)
    if (!selected.length) { setError('Zaznacz przynajmniej jedną wykonaną serię.'); return }
    const sets = selected.map((row) => ({ exerciseId: row.exerciseId, setNo: row.setNo, reps: Number(row.reps), weight: decimal(row.weight, 0, 2000) }))
    if (sets.some((row, index) => !/^\d+$/.test(selected[index].reps) || !Number.isInteger(row.reps) || row.reps < 1 || row.reps > 1000 || row.weight === null)) {
      setError('Każda wykonana seria wymaga 1–1000 powtórzeń i ciężaru od 0 do 2000 kg (maksymalnie dwa miejsca po przecinku).'); return
    }
    setSaving(true)
    try {
      await apiFetch('/api/results', { method: 'POST', body: JSON.stringify({ time: instant, planId: plan.id, planDayId: day.id, sets }) })
      if (scheduleId) {
        try {
          await apiFetch(`/api/schedule/${scheduleId}/complete`, { method: 'POST' })
          setScheduleNote('Trening w kalendarzu został oznaczony jako wykonany.')
        } catch { setScheduleNote('Wyniki zapisano, ale nie udało się oznaczyć treningu w kalendarzu. Możesz zrobić to ręcznie w kalendarzu.') }
      }
      setSaved(true)
    } catch (err) { setError((err as ApiError).message || 'Nie udało się zapisać treningu. Twoje wpisy pozostały w formularzu.') }
    finally { setSaving(false) }
  }
  if (saved) return <Card><CardContent className="space-y-4 py-8 text-center"><Check className="mx-auto text-primary" aria-hidden="true" /><h2 className="text-xl font-semibold">Trening zapisany</h2><p className="text-sm text-muted-foreground">Wyniki są już w historii. Sprawdź wykres progresu lub wróć do swoich planów.</p>{scheduleNote && <p className="text-sm text-muted-foreground">{scheduleNote}</p>}<div className="flex flex-wrap justify-center gap-2"><Button asChild><Link to="/progres">Zobacz progres</Link></Button><Button asChild variant="outline"><Link to="/plany">Moje plany</Link></Button></div><Button variant="ghost" onClick={() => { setSaved(false); setScheduleNote(''); setTime(localDateTime()); setRows(day ? sessionDraft(day) : []) }}>Zapisz kolejny trening</Button></CardContent></Card>
  return <form onSubmit={submit} className="space-y-5">
    <fieldset disabled={saving} className="space-y-5">
      <div className="grid gap-4 sm:grid-cols-2"><div className="space-y-2"><Label htmlFor="session-day">Dzień planu</Label><select id="session-day" className={selectClass} value={dayId} onChange={(event) => { const next = plan.days.find((item) => item.id === Number(event.target.value)); setDayId(next?.id); setRows(next ? sessionDraft(next) : []); setError('') }}>{plan.days.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div><div className="space-y-2"><Label htmlFor="session-time">Data i godzina treningu</Label><Input id="session-time" type="datetime-local" value={time} onChange={(event) => setTime(event.target.value)} required /></div></div>
      {(day?.exercises.reduce((sum, entry) => sum + entry.sets, 0) || 0) > rows.length && <Notice>Duży plan: formularz obejmuje pierwsze {rows.length} serii. Jeden zapis może zawierać do 100 serii danego ćwiczenia i 500 serii łącznie.</Notice>}
      {Array.from(new Set(rows.map((row) => row.exerciseId))).map((exerciseId) => <Card key={exerciseId}><CardHeader><CardTitle className="break-words text-lg">{rows.find((row) => row.exerciseId === exerciseId)?.exerciseName}</CardTitle></CardHeader><CardContent className="space-y-3">{rows.map((row, index) => row.exerciseId === exerciseId && <div key={`${exerciseId}-${row.setNo}`} className="grid grid-cols-[auto_1fr_1fr] items-end gap-3 rounded-lg bg-muted/60 p-3"><label className="flex min-h-10 items-center gap-2 text-sm"><input type="checkbox" checked={row.included} onChange={(event) => update(index, { included: event.target.checked })} className="size-4 accent-primary" aria-label={`Wykonana seria ${row.setNo}: ${row.exerciseName}`} /><span>#{row.setNo}</span></label><div className="min-w-0 space-y-2"><Label htmlFor={`reps-${index}`} className="text-xs">Powtórzenia</Label><Input id={`reps-${index}`} inputMode="numeric" value={row.reps} disabled={!row.included} onChange={(event) => update(index, { reps: event.target.value })} aria-label={`${row.exerciseName}, seria ${row.setNo}, powtórzenia`} /></div><div className="min-w-0 space-y-2"><Label htmlFor={`weight-${index}`} className="text-xs">Ciężar (kg)</Label><Input id={`weight-${index}`} inputMode="decimal" value={row.weight} disabled={!row.included} onChange={(event) => update(index, { weight: event.target.value })} aria-label={`${row.exerciseName}, seria ${row.setNo}, ciężar`} /></div></div>)}</CardContent></Card>)}
      <p className="text-xs text-muted-foreground">Dla ćwiczeń bez obciążenia wpisz 0 kg. Nazwa planu i dnia zostanie zachowana w historii, także po ich późniejszej zmianie.</p>
      {error && <Notice variant="error">{error}</Notice>}
      <Button type="submit" disabled={saving || !rows.length}>{saving ? 'Zapisywanie…' : 'Zapisz trening'}</Button>
    </fieldset>
  </form>
}
