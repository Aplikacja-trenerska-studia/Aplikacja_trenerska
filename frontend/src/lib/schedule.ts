import type { ScheduledWorkout } from '../types/schedule'

export type MonthRef = { year: number; month: number }

const part = (value: number) => String(value).padStart(2, '0')

export function isoDate(date = new Date()): string {
  return `${date.getFullYear()}-${part(date.getMonth() + 1)}-${part(date.getDate())}`
}

export function parseIsoDate(value: string | null | undefined): Date | null {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return null
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  return isoDate(date) === value ? date : null
}

export function addDays(value: string, days: number): string {
  const date = parseIsoDate(value)
  if (!date) throw new Error(`Invalid date: ${value}`)
  date.setDate(date.getDate() + days)
  return isoDate(date)
}

export function monthParam(value: string | null | undefined, today = new Date()): MonthRef {
  const match = value?.match(/^(\d{4})-(\d{2})$/)
  if (match) {
    const year = Number(match[1]), month = Number(match[2])
    if (year >= 2000 && year <= 2100 && month >= 1 && month <= 12) return { year, month }
  }
  return { year: today.getFullYear(), month: today.getMonth() + 1 }
}

export function monthKey(ref: MonthRef): string {
  return `${ref.year}-${part(ref.month)}`
}

export function shiftMonth(ref: MonthRef, delta: number): MonthRef {
  const date = new Date(ref.year, ref.month - 1 + delta, 1)
  return { year: date.getFullYear(), month: date.getMonth() + 1 }
}

export function monthGrid(ref: MonthRef): string[] {
  const first = new Date(ref.year, ref.month - 1, 1)
  const last = new Date(ref.year, ref.month, 0)
  const start = new Date(first)
  start.setDate(first.getDate() - ((first.getDay() + 6) % 7))
  const end = new Date(last)
  end.setDate(last.getDate() + (6 - ((last.getDay() + 6) % 7)))
  const days: string[] = []
  for (const cursor = new Date(start); cursor <= end; cursor.setDate(cursor.getDate() + 1)) days.push(isoDate(cursor))
  return days
}

export function inMonth(value: string, ref: MonthRef): boolean {
  return value.startsWith(`${monthKey(ref)}-`)
}

export function groupByDate(entries: ScheduledWorkout[]): Map<string, ScheduledWorkout[]> {
  const groups = new Map<string, ScheduledWorkout[]>()
  for (const entry of entries) groups.set(entry.date, [...(groups.get(entry.date) || []), entry])
  return groups
}

export function workoutLink(entry: Pick<ScheduledWorkout, 'id' | 'planId' | 'planDayId'>): string | null {
  if (!entry.planId) return null
  const query = new URLSearchParams({ planId: String(entry.planId) })
  if (entry.planDayId) query.set('dayId', String(entry.planDayId))
  query.set('scheduleId', String(entry.id))
  return `/trening?${query}`
}

export function formatDay(value: string, options: Intl.DateTimeFormatOptions = { weekday: 'long', day: 'numeric', month: 'long' }): string {
  const date = parseIsoDate(value)
  return date ? new Intl.DateTimeFormat('pl-PL', options).format(date) : value
}

export function relativeDay(value: string, today = isoDate()): string {
  if (value === today) return 'Dziś'
  if (value === addDays(today, 1)) return 'Jutro'
  if (value === addDays(today, -1)) return 'Wczoraj'
  return formatDay(value, { weekday: 'short', day: 'numeric', month: 'short' })
}
