import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowDown, ArrowUp, Plus, Save, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Notice } from '@/components/ui/notice'
import { ExercisePicker } from '@/components/workout/ExercisePicker'
import { apiFetch } from '@/lib/api'
import { moveItem, newDraftDay, newDraftEntry, planToDraft, validatePlanDraft, type DraftDay, type PlanDraft } from '@/lib/plan-draft'
import type { ApiError } from '@/types/auth'
import { muscleGroupLabels, type Exercise, type WorkoutPlan } from '@/types/workout'

function FieldError({ id, error }: { id: string; error?: string }) {
  return error ? <p id={`${id}-error`} className="mt-1 text-xs text-destructive">{error}</p> : null
}

export function PlanEditor({ catalog, plan }: { catalog: Exercise[]; plan?: WorkoutPlan }) {
  const navigate = useNavigate()
  const [initial] = useState<PlanDraft>(() => plan ? planToDraft(plan) : { name: '', description: '', days: [newDraftDay(0)] })
  const [draft, setDraft] = useState(initial)
  const [picker, setPicker] = useState<string | null>(null)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const summary = useRef<HTMLDivElement>(null)
  const dirty = JSON.stringify(initial) !== JSON.stringify(draft)

  useEffect(() => {
    if (!dirty) return
    function warn(event: BeforeUnloadEvent) { event.preventDefault(); event.returnValue = '' }
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [dirty])

  function change(update: (current: PlanDraft) => PlanDraft) {
    setDraft(update); setErrors({}); setError('')
  }
  function changeDay(key: string, update: (day: DraftDay) => DraftDay) {
    change((current) => ({ ...current, days: current.days.map((day) => day.key === key ? update(day) : day) }))
  }
  function invalid(message: string, fields: Record<string, string> = {}) {
    setErrors(fields); setError(message)
    requestAnimationFrame(() => summary.current?.focus())
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving) return
    const result = validatePlanDraft(draft, catalog)
    if (!result.payload) { invalid('Sprawdź zaznaczone pola i uzupełnij plan.', result.errors); return }
    setSaving(true); setErrors({}); setError('')
    try {
      const saved = await apiFetch<WorkoutPlan>(plan ? `/api/plans/${plan.id}` : '/api/plans', { method: plan ? 'PUT' : 'POST', body: JSON.stringify(result.payload) })
      navigate(`/plany/${saved.id}`, { replace: true, state: { notice: plan ? 'Zmiany zostały zapisane.' : 'Plan został utworzony.' } })
    } catch (failure) {
      const problem = failure as ApiError
      invalid(problem.message || 'Nie udało się zapisać planu. Spróbuj ponownie.', problem.errors)
    } finally { setSaving(false) }
  }
  function cancel() {
    if (!dirty || window.confirm('Odrzucić niezapisane zmiany w planie?')) navigate(plan ? `/plany/${plan.id}` : '/plany')
  }

  return <form onSubmit={submit} noValidate className="space-y-6">
    <div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Twój trening</p><h1 className="text-3xl font-semibold tracking-tight">{plan ? 'Edytuj plan' : 'Nowy plan'}</h1><p className="mt-2 text-sm leading-relaxed text-muted-foreground">Nazwij plan, dodaj dni i wybierz ćwiczenia. Kolejność możesz zmieniać strzałkami.</p></div>
    {error && <div ref={summary} tabIndex={-1}><Notice variant="error">{error} Wpisane dane pozostają w formularzu.</Notice></div>}
    <fieldset disabled={saving} className="min-w-0 space-y-6">
      <Card><CardHeader><CardTitle className="text-lg">Informacje o planie</CardTitle></CardHeader><CardContent className="space-y-4">
        <div className="space-y-2"><Label htmlFor="plan-name">Nazwa planu</Label><Input id="plan-name" maxLength={120} required value={draft.name} aria-invalid={!!errors.name} aria-describedby={errors.name ? 'plan-name-error' : undefined} onChange={(event) => change((current) => ({ ...current, name: event.target.value }))} placeholder="Np. Trening całego ciała" /><FieldError id="plan-name" error={errors.name} /></div>
        <div className="space-y-2"><Label htmlFor="plan-description">Opis (opcjonalnie)</Label><textarea id="plan-description" maxLength={2000} rows={3} className="w-full rounded-md border bg-background px-3 py-2 text-base focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring md:text-sm" value={draft.description} aria-invalid={!!errors.description} aria-describedby={errors.description ? 'plan-description-error' : undefined} onChange={(event) => change((current) => ({ ...current, description: event.target.value }))} /><FieldError id="plan-description" error={errors.description} /></div>
      </CardContent></Card>
      <FieldError id="plan-days" error={errors.days} />
      {draft.days.map((day, dayIndex) => {
        const field = `days[${dayIndex}]`
        return <section key={day.key} aria-label={`Dzień ${dayIndex + 1}`}><Card><CardHeader><div className="flex flex-wrap items-center justify-between gap-3"><CardTitle className="text-lg">Dzień {dayIndex + 1}</CardTitle><div className="flex gap-1">
          <Button type="button" variant="ghost" size="sm" className="h-11 w-11 p-0" aria-label={`Dzień ${dayIndex + 1}: w górę`} disabled={dayIndex === 0} onClick={() => change((current) => ({ ...current, days: moveItem(current.days, dayIndex, dayIndex - 1) }))}><ArrowUp size={17} aria-hidden="true" /></Button>
          <Button type="button" variant="ghost" size="sm" className="h-11 w-11 p-0" aria-label={`Dzień ${dayIndex + 1}: w dół`} disabled={dayIndex === draft.days.length - 1} onClick={() => change((current) => ({ ...current, days: moveItem(current.days, dayIndex, dayIndex + 1) }))}><ArrowDown size={17} aria-hidden="true" /></Button>
          <Button type="button" variant="ghost" size="sm" className="h-11 text-destructive" disabled={draft.days.length === 1} aria-label={`Usuń dzień ${dayIndex + 1}`} onClick={() => change((current) => ({ ...current, days: current.days.filter((item) => item.key !== day.key) }))}><Trash2 size={16} aria-hidden="true" />Usuń dzień</Button>
        </div></div></CardHeader><CardContent className="space-y-4">
          <div className="space-y-2"><Label htmlFor={`day-${day.key}`}>Nazwa dnia</Label><Input id={`day-${day.key}`} maxLength={120} required value={day.name} aria-invalid={!!errors[`${field}.name`]} aria-describedby={errors[`${field}.name`] ? `day-${day.key}-error` : undefined} onChange={(event) => changeDay(day.key, (current) => ({ ...current, name: event.target.value }))} /><FieldError id={`day-${day.key}`} error={errors[`${field}.name`]} /></div>
          {day.exercises.map((entry, index) => {
            const exercise = catalog.find((item) => item.id === entry.exerciseId)
            const prefix = `${field}.exercises[${index}]`
            return <div role="group" aria-label={`Ćwiczenie ${index + 1}: ${exercise?.name || 'Ćwiczenie'}`} key={entry.key} className="space-y-3 rounded-lg border p-3 sm:p-4">
              <div><h3 className="break-words font-medium">{index + 1}. {exercise?.name || 'Ćwiczenie niedostępne'}</h3>{exercise && <p className="mt-1 text-xs text-muted-foreground">{muscleGroupLabels[exercise.muscleGroup]}</p>}<FieldError id={`exercise-${entry.key}`} error={errors[`${prefix}.exerciseId`]} /></div>
              <div className="grid grid-cols-3 gap-2">{([['sets', 'Serie'], ['reps', 'Powtórzenia'], ['weight', 'Ciężar (kg)']] as const).map(([key, label]) => {
                const inputId = `${key}-${entry.key}`
                return <div key={key} className="min-w-0 space-y-2"><Label htmlFor={inputId} className="text-xs">{label}</Label><Input id={inputId} inputMode={key === 'weight' ? 'decimal' : 'numeric'} required value={entry[key]} aria-invalid={!!errors[`${prefix}.${key}`]} aria-describedby={errors[`${prefix}.${key}`] ? `${inputId}-error` : undefined} onChange={(event) => changeDay(day.key, (current) => ({ ...current, exercises: current.exercises.map((item) => item.key === entry.key ? { ...item, [key]: event.target.value } : item) }))} /><FieldError id={inputId} error={errors[`${prefix}.${key}`]} /></div>
              })}</div>
              <div className="flex justify-end gap-1">
                <Button type="button" variant="ghost" size="sm" className="h-11 w-11 p-0" aria-label={`Ćwiczenie ${index + 1}: w górę`} disabled={index === 0} onClick={() => changeDay(day.key, (current) => ({ ...current, exercises: moveItem(current.exercises, index, index - 1) }))}><ArrowUp size={16} aria-hidden="true" /></Button>
                <Button type="button" variant="ghost" size="sm" className="h-11 w-11 p-0" aria-label={`Ćwiczenie ${index + 1}: w dół`} disabled={index === day.exercises.length - 1} onClick={() => changeDay(day.key, (current) => ({ ...current, exercises: moveItem(current.exercises, index, index + 1) }))}><ArrowDown size={16} aria-hidden="true" /></Button>
                <Button type="button" variant="ghost" size="sm" className="h-11 text-destructive" aria-label={`Usuń ćwiczenie ${index + 1}`} onClick={() => changeDay(day.key, (current) => ({ ...current, exercises: current.exercises.filter((item) => item.key !== entry.key) }))}><Trash2 size={16} aria-hidden="true" />Usuń</Button>
              </div>
            </div>
          })}
          {day.exercises.length === 0 && <p className="text-sm text-muted-foreground">Dodaj co najmniej jedno ćwiczenie do tego dnia.</p>}
          <FieldError id={`entries-${day.key}`} error={errors[`${field}.exercises`]} />
          <Button type="button" variant="outline" disabled={day.exercises.length >= 100} aria-expanded={picker === day.key} onClick={() => setPicker(picker === day.key ? null : day.key)}><Plus size={16} aria-hidden="true" />{picker === day.key ? 'Zamknij wybór ćwiczeń' : 'Dodaj ćwiczenie'}</Button>
          {picker === day.key && day.exercises.length < 100 && <ExercisePicker catalog={catalog} onAdd={(exercise) => changeDay(day.key, (current) => ({ ...current, exercises: [...current.exercises, newDraftEntry(exercise.id)] }))} />}
        </CardContent></Card></section>
      })}
      <Button type="button" variant="outline" disabled={draft.days.length >= 31} onClick={() => change((current) => ({ ...current, days: [...current.days, newDraftDay(current.days.length)] }))}><Plus size={16} aria-hidden="true" />Dodaj dzień</Button>
    </fieldset>
    <div className="flex flex-wrap gap-3 border-t pt-5"><Button type="submit" disabled={saving}><Save size={16} aria-hidden="true" />{saving ? 'Zapisywanie…' : plan ? 'Zapisz zmiany' : 'Zapisz plan'}</Button><Button type="button" variant="ghost" disabled={saving} onClick={cancel}>Anuluj</Button></div>
  </form>
}
