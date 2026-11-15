import type { Exercise, WorkoutPlan, WorkoutPlanRequest } from '../types/workout'

export type DraftEntry = { key: string; exerciseId: number; sets: string; reps: string; weight: string }
export type DraftDay = { key: string; name: string; exercises: DraftEntry[] }
export type PlanDraft = { name: string; description: string; days: DraftDay[] }

export function newDraftDay(index: number): DraftDay {
  return { key: crypto.randomUUID(), name: `Dzień ${index + 1}`, exercises: [] }
}

export function newDraftEntry(exerciseId: number): DraftEntry {
  return { key: crypto.randomUUID(), exerciseId, sets: '3', reps: '10', weight: '0' }
}

export function planToDraft(plan: WorkoutPlan): PlanDraft {
  return { name: plan.name, description: plan.description || '', days: plan.days.map((day) => ({
    key: crypto.randomUUID(), name: day.name, exercises: day.exercises.map((entry) => ({
      key: crypto.randomUUID(), exerciseId: entry.exercise.id, sets: String(entry.sets), reps: String(entry.reps), weight: String(entry.weight).replace('.', ','),
    })),
  })) }
}

export function moveItem<T>(items: T[], from: number, to: number): T[] {
  if (from < 0 || to < 0 || from >= items.length || to >= items.length || from === to) return items
  const next = [...items]
  const [item] = next.splice(from, 1)
  next.splice(to, 0, item)
  return next
}

export function validatePlanDraft(draft: PlanDraft, catalog: Pick<Exercise, 'id'>[]): {
  payload: WorkoutPlanRequest | null; errors: Record<string, string>
} {
  const errors: Record<string, string> = {}
  const ids = new Set(catalog.map((exercise) => exercise.id))
  function name(value: string, field: string) {
    if (!value.trim()) errors[field] = 'Wpisz nazwę.'
    else if (value.trim().length > 120) errors[field] = 'Nazwa może mieć maksymalnie 120 znaków.'
    return value.trim()
  }
  function integer(value: string, field: string) {
    const parsed = Number(value.trim())
    if (!/^\d+$/.test(value.trim()) || !Number.isSafeInteger(parsed) || parsed < 1 || parsed > 2_147_483_647) errors[field] = 'Wpisz dodatnią liczbę całkowitą.'
    return parsed
  }
  const payload: WorkoutPlanRequest = {
    name: name(draft.name, 'name'), description: draft.description.trim() || null,
    days: draft.days.map((day, dayIndex) => {
      const field = `days[${dayIndex}]`
      const dayName = name(day.name, `${field}.name`)
      if (day.exercises.length < 1 || day.exercises.length > 100) errors[`${field}.exercises`] = 'Dzień musi zawierać od 1 do 100 ćwiczeń.'
      return { name: dayName, exercises: day.exercises.map((entry, index) => {
        const prefix = `${field}.exercises[${index}]`
        if (!ids.has(entry.exerciseId)) errors[`${prefix}.exerciseId`] = 'Wybierz ćwiczenie dostępne w katalogu.'
        const weight = Number(entry.weight.trim().replace(',', '.'))
        if (!/^\d+(?:[.,]\d{1,2})?$/.test(entry.weight.trim()) || !Number.isFinite(weight) || weight < 0 || weight > 99999.99) {
          errors[`${prefix}.weight`] = 'Wpisz ciężar od 0 do 99999,99 kg, do dwóch miejsc po przecinku.'
        }
        return { exerciseId: entry.exerciseId, sets: integer(entry.sets, `${prefix}.sets`), reps: integer(entry.reps, `${prefix}.reps`), weight }
      }) }
    }),
  }
  if (draft.description.trim().length > 2000) errors.description = 'Opis może mieć maksymalnie 2000 znaków.'
  if (draft.days.length < 1 || draft.days.length > 31) errors.days = 'Plan musi zawierać od 1 do 31 dni.'
  return { payload: Object.keys(errors).length ? null : payload, errors }
}
