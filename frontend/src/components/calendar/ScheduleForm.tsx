import { useEffect, useState, type FormEvent } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Notice } from '@/components/ui/notice'
import { useApiResource } from '@/hooks/use-api-resource'
import { apiFetch } from '@/lib/api'
import { parseIsoDate } from '@/lib/schedule'
import type { ApiError } from '@/types/auth'
import type { ScheduleRequest, ScheduledWorkout } from '@/types/schedule'
import type { PlanSummary, WorkoutPlan } from '@/types/workout'

const selectClass = 'h-10 w-full min-w-0 rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

export function ScheduleForm({ plans, entryId, initial, onSaved, onCancel }: {
  plans: PlanSummary[]
  entryId?: number
  initial: { date: string; planId: number | null; planDayId: number | null; note: string }
  onSaved: (entry: ScheduledWorkout) => void
  onCancel?: () => void
}) {
  const [date, setDate] = useState(initial.date)
  const [planId, setPlanId] = useState<number | null>(initial.planId && plans.some((plan) => plan.id === initial.planId) ? initial.planId : null)
  const [dayId, setDayId] = useState<number | null>(initial.planDayId)
  const [note, setNote] = useState(initial.note)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const prefix = entryId ? `schedule-edit-${entryId}` : 'schedule-new'

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    setError('')
    if (!parseIsoDate(date)) { setError('Wybierz prawidłową datę.'); return }
    if (!planId || !dayId) { setError('Wybierz plan i dzień treningowy.'); return }
    if (note.trim().length > 500) { setError('Notatka może mieć maksymalnie 500 znaków.'); return }
    const body: ScheduleRequest = { date, planId, planDayId: dayId, note: note.trim() || null }
    setBusy(true)
    try {
      const saved = await apiFetch<ScheduledWorkout>(entryId ? `/api/schedule/${entryId}` : '/api/schedule', { method: entryId ? 'PUT' : 'POST', body: JSON.stringify(body) })
      onSaved(saved)
    } catch (failure) { setError((failure as ApiError).message || 'Nie udało się zapisać treningu w kalendarzu.') }
    finally { setBusy(false) }
  }

  if (!plans.length) return <p className="rounded-lg border border-dashed p-4 text-sm text-muted-foreground">Aby zaplanować trening, najpierw utwórz plan lub przyjmij plan od trenera.</p>
  return <form onSubmit={submit} noValidate>
    <fieldset disabled={busy} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="min-w-0 space-y-2"><Label htmlFor={`${prefix}-date`}>Data</Label><Input id={`${prefix}-date`} type="date" min="2000-01-01" max="2100-12-31" value={date} onChange={(event) => setDate(event.target.value)} required /></div>
        <div className="min-w-0 space-y-2"><Label htmlFor={`${prefix}-plan`}>Plan</Label><select id={`${prefix}-plan`} className={selectClass} value={planId ?? ''} onChange={(event) => { setPlanId(event.target.value ? Number(event.target.value) : null); setDayId(null) }}><option value="">Wybierz plan</option>{plans.map((plan) => <option key={plan.id} value={plan.id}>{plan.name}</option>)}</select></div>
      </div>
      {planId && <PlanDaySelect key={planId} id={`${prefix}-day`} planId={planId} value={dayId} onChange={setDayId} />}
      <div className="space-y-2"><Label htmlFor={`${prefix}-note`}>Notatka (opcjonalnie)</Label><Input id={`${prefix}-note`} maxLength={500} value={note} placeholder="np. rano przed pracą" onChange={(event) => setNote(event.target.value)} /></div>
      {error && <Notice variant="error">{error}</Notice>}
      <div className="flex flex-wrap gap-2">
        <Button type="submit">{busy ? 'Zapisywanie…' : entryId ? 'Zapisz zmiany' : 'Dodaj do kalendarza'}</Button>
        {onCancel && <Button type="button" variant="ghost" onClick={onCancel}>Anuluj</Button>}
      </div>
    </fieldset>
  </form>
}

function PlanDaySelect({ id, planId, value, onChange }: { id: string; planId: number; value: number | null; onChange: (dayId: number | null) => void }) {
  const plan = useApiResource<WorkoutPlan>(`/api/plans/${planId}`)
  const days = plan.data?.days
  useEffect(() => {
    if (days && !days.some((day) => day.id === value)) onChange(days[0]?.id ?? null)
  }, [days, value, onChange])
  if (plan.loading) return <p role="status" className="text-sm text-muted-foreground">Wczytywanie dni planu…</p>
  if (plan.error) return <div className="space-y-2"><Notice variant="error">{plan.error.message}</Notice><Button type="button" variant="outline" size="sm" onClick={plan.reload}>Spróbuj ponownie</Button></div>
  return <div className="space-y-2"><Label htmlFor={id}>Dzień planu</Label><select id={id} className={selectClass} value={value ?? ''} onChange={(event) => onChange(Number(event.target.value))}>{days?.map((day) => <option key={day.id} value={day.id}>{day.dayIndex + 1}. {day.name}</option>)}</select></div>
}
