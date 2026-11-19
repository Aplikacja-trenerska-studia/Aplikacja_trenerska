import type { BodyMetric, ChartPoint, ExerciseResult, MetricKey } from '../types/progress'
import type { WorkoutPlan } from '../types/workout'

export const numberFormat = new Intl.NumberFormat('pl-PL', { maximumFractionDigits: 2 })
export function formatMoment(value: string | number) {
  return new Intl.DateTimeFormat('pl-PL', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
export function localDateTime(date = new Date()): string {
  const part = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${part(date.getMonth() + 1)}-${part(date.getDate())}T${part(date.getHours())}:${part(date.getMinutes())}`
}
export function toInstant(value: string): string | null {
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) || localDateTime(date) !== value ? null : date.toISOString()
}
export function decimal(value: string, minimum: number, maximum: number): number | null {
  if (!/^\d+(?:[.,]\d{1,2})?$/.test(value.trim())) return null
  const parsed = Number(value.trim().replace(',', '.'))
  return Number.isFinite(parsed) && parsed >= minimum && parsed <= maximum ? parsed : null
}
export function historyQuery(exerciseId: string, from: string, to: string) {
  const query = new URLSearchParams()
  if (exerciseId) query.set('exerciseId', exerciseId)
  if (from) query.set('from', new Date(`${from}T00:00:00`).toISOString())
  if (to) query.set('to', new Date(`${to}T23:59:59.999`).toISOString())
  return query.size ? `?${query}` : ''
}
export function resultPoints(results: ExerciseResult[], exerciseId: number): ChartPoint[] {
  const days = new Map<string, ChartPoint>()
  for (const row of results) {
    if (row.exerciseId !== exerciseId) continue
    const date = new Date(row.time)
    const day = localDateTime(date).slice(0, 10)
    const existing = days.get(day)
    if (!existing || row.weight > existing.value) days.set(day, { time: new Date(`${day}T12:00:00`).getTime(), value: row.weight })
  }
  return [...days.values()].sort((a, b) => a.time - b.time)
}
export function metricPoints(metrics: BodyMetric[], key: MetricKey): ChartPoint[] {
  return metrics.filter((row) => row[key] !== null && row[key] !== undefined)
    .map((row) => ({ time: new Date(row.time).getTime(), value: row[key]! })).sort((a, b) => a.time - b.time)
}
export type SetDraft = { exerciseId: number; exerciseName: string; setNo: number; reps: string; weight: string; included: boolean }
export function sessionDraft(day: WorkoutPlan['days'][number]): SetDraft[] {
  const nextSet = new Map<number, number>()
  return day.exercises.flatMap((entry) => Array.from({ length: Math.min(entry.sets, 100 - (nextSet.get(entry.exercise.id) || 0)) }, () => {
    const setNo = (nextSet.get(entry.exercise.id) || 0) + 1
    nextSet.set(entry.exercise.id, setNo)
    return { exerciseId: entry.exercise.id, exerciseName: entry.exercise.name, setNo, reps: String(entry.reps), weight: String(entry.weight).replace('.', ','), included: true }
  })).slice(0, 500)
}
