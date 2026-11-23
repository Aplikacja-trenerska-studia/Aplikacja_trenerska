import type { WeekStats } from '../types/stats'

export const statRanges = [4, 12, 26, 52] as const

export function currentTimeZone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Warsaw'
  } catch {
    return 'Europe/Warsaw'
  }
}

export function weeksParam(value: string | null): number {
  const parsed = Number(value)
  return statRanges.includes(parsed as (typeof statRanges)[number]) ? parsed : 12
}

export function summaryPath(weeks: number, zone = currentTimeZone()): string {
  return `/api/stats/summary?${new URLSearchParams({ weeks: String(weeks), zone })}`
}

export function weekLabel(weekStart: string): string {
  const [year, month, day] = weekStart.split('-').map(Number)
  return new Intl.DateTimeFormat('pl-PL', { day: 'numeric', month: 'short' }).format(new Date(year, month - 1, day))
}

export function formatVolume(value: number): string {
  return `${new Intl.NumberFormat('pl-PL', { maximumFractionDigits: value >= 1000 ? 0 : 1 }).format(value)} kg`
}

export function volumeChange(weeks: WeekStats[]): number | null {
  if (weeks.length < 2) return null
  const current = weeks[weeks.length - 1].volume, previous = weeks[weeks.length - 2].volume
  if (previous <= 0) return null
  return Math.round((current - previous) / previous * 100)
}

export function chartScale(values: number[]): number {
  const max = Math.max(0, ...values)
  if (max <= 0) return 1
  const magnitude = 10 ** Math.floor(Math.log10(max))
  const step = [1, 2, 2.5, 5, 10].find((candidate) => candidate * magnitude >= max) || 10
  return step * magnitude
}
