import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { CalendarPlus, ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Notice } from '@/components/ui/notice'
import { ResourceStatus } from '@/components/ui/resource-status'
import { ScheduleEntryCard } from '@/components/calendar/ScheduleEntryCard'
import { ScheduleForm } from '@/components/calendar/ScheduleForm'
import { useApiResource } from '@/hooks/use-api-resource'
import { formatDay, groupByDate, inMonth, isoDate, monthGrid, monthKey, monthParam, parseIsoDate, shiftMonth, type MonthRef } from '@/lib/schedule'
import type { ScheduledWorkout } from '@/types/schedule'
import type { PlanSummary } from '@/types/workout'

const weekdays = ['Pn', 'Wt', 'Śr', 'Cz', 'Pt', 'So', 'Nd']

export function CalendarPage() {
  const [params, setParams] = useSearchParams()
  const month = monthParam(params.get('miesiac'))
  const today = isoDate()
  const requestedDay = params.get('dzien')
  const selected = requestedDay && parseIsoDate(requestedDay) ? requestedDay : inMonth(today, month) ? today : `${monthKey(month)}-01`
  const preselectedPlan = Number(params.get('planId')) || null
  const grid = monthGrid(month)
  const schedule = useApiResource<ScheduledWorkout[]>(`/api/schedule?from=${grid[0]}&to=${grid[grid.length - 1]}`)
  const plans = useApiResource<PlanSummary[]>('/api/plans')
  const [editing, setEditing] = useState<number | null>(null)
  const [notice, setNotice] = useState('')
  const [formVersion, setFormVersion] = useState(0)
  const byDate = groupByDate(schedule.data || [])
  const dayEntries = byDate.get(selected) || []

  function navigate(next: MonthRef, day?: string) {
    const query = new URLSearchParams({ miesiac: monthKey(next) })
    if (day) query.set('dzien', day)
    if (preselectedPlan) query.set('planId', String(preselectedPlan))
    setParams(query)
    setEditing(null)
  }

  function select(day: string) {
    const date = parseIsoDate(day)!
    navigate({ year: date.getFullYear(), month: date.getMonth() + 1 }, day)
    setNotice('')
  }

  function saved(entry: ScheduledWorkout, message: string) {
    schedule.reload()
    if (entry.date !== selected) select(entry.date)
    setFormVersion((value) => value + 1)
    setNotice(message)
  }

  const monthTitle = new Intl.DateTimeFormat('pl-PL', { month: 'long', year: 'numeric' }).format(new Date(month.year, month.month - 1, 1))
  return <div className="space-y-6">
    <div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Planowanie</p><h1 className="text-3xl font-semibold tracking-tight">Kalendarz treningów</h1><p className="mt-2 text-sm text-muted-foreground">Zaplanuj dni treningowe z własnych planów. Zaplanowane i zaległe treningi zobaczysz także na stronie głównej.</p></div>
    <Card>
      <CardHeader className="flex-row items-center justify-between gap-2 space-y-0">
        <Button type="button" variant="ghost" size="sm" onClick={() => navigate(shiftMonth(month, -1))} aria-label="Poprzedni miesiąc"><ChevronLeft size={18} aria-hidden="true" /></Button>
        <div className="text-center"><CardTitle className="text-lg capitalize">{monthTitle}</CardTitle>{!inMonth(today, month) && <button type="button" className="mt-1 text-xs font-medium text-primary underline-offset-4 hover:underline" onClick={() => select(today)}>Wróć do dzisiaj</button>}</div>
        <Button type="button" variant="ghost" size="sm" onClick={() => navigate(shiftMonth(month, 1))} aria-label="Następny miesiąc"><ChevronRight size={18} aria-hidden="true" /></Button>
      </CardHeader>
      <CardContent className="space-y-3 px-3 sm:px-6">
        <div className="grid grid-cols-7 gap-1 text-center text-xs font-medium text-muted-foreground" aria-hidden="true">{weekdays.map((day) => <span key={day}>{day}</span>)}</div>
        <div className="grid grid-cols-7 gap-1" role="group" aria-label={`Dni miesiąca: ${monthTitle}`}>
          {grid.map((day) => {
            const entries = byDate.get(day) || []
            const done = entries.filter((entry) => entry.completed).length
            const isSelected = day === selected
            return <button key={day} type="button" aria-pressed={isSelected} aria-label={`${formatDay(day)}${entries.length ? `, treningi: ${entries.length}, wykonane: ${done}` : ''}`} onClick={() => select(day)}
              className={`flex min-h-12 flex-col items-center justify-start gap-1 rounded-lg border px-0.5 py-1.5 text-sm transition-colors sm:min-h-16 ${isSelected ? 'border-primary bg-primary text-primary-foreground' : day === today ? 'border-primary/50 bg-primary/5' : 'border-transparent hover:bg-accent'} ${inMonth(day, month) ? '' : 'opacity-45'}`}>
              <span className={day === today && !isSelected ? 'font-semibold text-primary' : 'font-medium'}>{Number(day.slice(8))}</span>
              {entries.length > 0 && <span className="flex flex-wrap justify-center gap-0.5" aria-hidden="true">{entries.slice(0, 3).map((entry) => <span key={entry.id} className={`size-1.5 rounded-full ${isSelected ? 'bg-primary-foreground' : entry.completed ? 'bg-primary/35' : 'bg-primary'}`} />)}{entries.length > 3 && <span className="text-[10px] leading-none">+</span>}</span>}
            </button>
          })}
        </div>
        <ResourceStatus loading={schedule.loading} error={schedule.error} onRetry={schedule.reload} label="Wczytywanie kalendarza…" />
      </CardContent>
    </Card>
    <section className="space-y-4" aria-labelledby="selected-day-heading">
      <h2 id="selected-day-heading" className="text-xl font-semibold first-letter:uppercase">{formatDay(selected, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}</h2>
      {notice && <Notice>{notice}</Notice>}
      {!schedule.loading && !schedule.error && (dayEntries.length ? <div className="space-y-3">{dayEntries.map((entry) => editing === entry.id && plans.data
        ? <Card key={entry.id}><CardContent className="pt-6"><ScheduleForm plans={plans.data} entryId={entry.id} initial={{ date: entry.date, planId: entry.planId, planDayId: entry.planDayId, note: entry.note || '' }} onCancel={() => setEditing(null)} onSaved={(next) => { setEditing(null); saved(next, 'Zmiany zapisane.') }} /></CardContent></Card>
        : <ScheduleEntryCard key={entry.id} entry={entry} onChanged={() => { setNotice(''); schedule.reload() }} onEdit={() => setEditing(entry.id)} />)}</div>
        : <p className="rounded-xl border border-dashed p-5 text-sm text-muted-foreground">Brak treningów zaplanowanych na ten dzień.</p>)}
      <Card>
        <CardHeader><CardTitle className="flex items-center gap-2 text-lg"><CalendarPlus size={18} className="text-primary" aria-hidden="true" />Zaplanuj trening</CardTitle></CardHeader>
        <CardContent>
          <ResourceStatus loading={plans.loading} error={plans.error} onRetry={plans.reload} label="Wczytywanie planów…" />
          {!plans.loading && !plans.error && plans.data && <ScheduleForm key={`${selected}-${formVersion}`} plans={plans.data} initial={{ date: selected, planId: preselectedPlan, planDayId: null, note: '' }} onSaved={(entry) => saved(entry, `Zaplanowano: ${entry.dayName}, ${formatDay(entry.date)}.`)} />}
        </CardContent>
      </Card>
    </section>
  </div>
}
