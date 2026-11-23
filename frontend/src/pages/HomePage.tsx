import { Link } from 'react-router-dom'
import { ArrowRight, Calculator, CalendarDays, CalendarPlus, ChartColumn, Dumbbell, History, ListChecks, Play } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ResourceStatus } from '@/components/ui/resource-status'
import { ScheduleEntryCard } from '@/components/calendar/ScheduleEntryCard'
import { useApiResource } from '@/hooks/use-api-resource'
import { useAuthSession } from '@/hooks/use-auth-session'
import { formatMoment, numberFormat } from '@/lib/progress'
import { formatDay, isoDate } from '@/lib/schedule'
import { formatVolume, summaryPath, volumeChange } from '@/lib/stats'
import type { Agenda } from '@/types/schedule'
import type { SessionSummary, StatsSummary } from '@/types/stats'

export function HomePage() {
  const { loggedIn, user } = useAuthSession()
  if (loggedIn) return <Dashboard name={user?.displayName || 'sportowcu'} />
  return <Card className="overflow-hidden">
    <div className="flex h-40 items-center justify-center bg-primary/5"><Dumbbell size={64} strokeWidth={1.3} className="text-primary" aria-hidden="true" /></div>
    <CardContent className="space-y-5 pt-7">
      <p className="text-xs font-semibold uppercase tracking-widest text-primary">Aplikacja trenerska</p>
      <h1 className="text-3xl font-semibold leading-tight tracking-tight">Każdy trening to krok do przodu.</h1>
      <p className="text-sm leading-relaxed text-muted-foreground">Planuj treningi, zapisuj wyniki i śledź postępy. Ćwicz samodzielnie lub pod opieką trenera.</p>
      <Button asChild className="w-full"><Link to="/rejestracja">Utwórz konto<ArrowRight size={16} aria-hidden="true" /></Link></Button>
      <p className="text-center text-sm text-muted-foreground">Masz już konto? <Link className="text-primary underline-offset-4 hover:underline" to="/logowanie">Zaloguj się</Link></p>
    </CardContent>
  </Card>
}

function Dashboard({ name }: { name: string }) {
  const today = isoDate()
  const agenda = useApiResource<Agenda>(`/api/schedule/agenda?date=${today}`)
  const stats = useApiResource<StatsSummary>(summaryPath(4))
  const sessions = useApiResource<SessionSummary[]>('/api/stats/sessions?limit=3')
  return <div className="w-full space-y-6">
    <div>
      <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary first-letter:uppercase">{formatDay(today, { weekday: 'long', day: 'numeric', month: 'long' })}</p>
      <h1 className="text-3xl font-semibold leading-tight tracking-tight">Witaj, {name}!</h1>
      <p className="mt-2 text-sm text-muted-foreground">Twój plan na dziś, najbliższe treningi i podsumowanie tygodnia.</p>
    </div>
    <TodayCard agenda={agenda} />
    <WeekCard stats={stats} />
    <Card>
      <CardHeader className="flex-row items-center justify-between gap-2 space-y-0"><CardTitle className="flex items-center gap-2 text-lg"><History size={18} className="text-primary" aria-hidden="true" />Ostatnie treningi</CardTitle><Link to="/progres" className="text-sm font-medium text-primary underline-offset-4 hover:underline">Historia</Link></CardHeader>
      <CardContent>
        <ResourceStatus loading={sessions.loading} error={sessions.error} onRetry={sessions.reload} label="Wczytywanie ostatnich treningów…" />
        {!sessions.loading && !sessions.error && sessions.data && (sessions.data.length ? <ul className="divide-y">{sessions.data.map((session) => <li key={session.time} className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1 py-3 first:pt-0 last:pb-0">
          <div className="min-w-0"><p className="break-words font-medium">{session.dayName}</p><p className="break-words text-xs text-muted-foreground">{session.planName} · {formatMoment(session.time)}</p></div>
          <p className="text-sm text-muted-foreground">{session.exercises} ćw. · {session.sets} serii · <span className="font-medium text-foreground">{formatVolume(session.volume)}</span></p>
        </li>)}</ul> : <p className="text-sm text-muted-foreground">Nie masz jeszcze zapisanych treningów.</p>)}
      </CardContent>
    </Card>
    <nav aria-label="Szybkie akcje" className="grid grid-cols-2 gap-3">
      {[
        { to: '/trening', label: 'Zapisz trening', Icon: Play },
        { to: '/kalendarz', label: 'Kalendarz', Icon: CalendarDays },
        { to: '/statystyki', label: 'Statystyki', Icon: ChartColumn },
        { to: '/kalkulatory', label: 'Kalkulatory', Icon: Calculator },
      ].map(({ to, label, Icon }) => <Link key={to} to={to} className="flex min-h-14 items-center gap-3 rounded-xl border bg-card p-4 text-sm font-medium shadow-sm transition-colors hover:border-primary/40 hover:bg-accent/30"><Icon size={18} className="shrink-0 text-primary" aria-hidden="true" />{label}</Link>)}
    </nav>
  </div>
}

function TodayCard({ agenda }: { agenda: ReturnType<typeof useApiResource<Agenda>> }) {
  const data = agenda.data
  const next = data?.upcoming[0]
  return <Card>
    <CardHeader className="flex-row items-center justify-between gap-2 space-y-0"><CardTitle className="flex items-center gap-2 text-lg"><ListChecks size={18} className="text-primary" aria-hidden="true" />Dziś do zrobienia</CardTitle><Link to="/kalendarz" className="text-sm font-medium text-primary underline-offset-4 hover:underline">Kalendarz</Link></CardHeader>
    <CardContent className="space-y-4">
      <ResourceStatus loading={agenda.loading} error={agenda.error} onRetry={agenda.reload} label="Wczytywanie planu dnia…" />
      {!agenda.loading && !agenda.error && data && <>
        {data.overdue.length > 0 && <section className="space-y-2" aria-labelledby="overdue-heading"><h3 id="overdue-heading" className="text-sm font-semibold text-destructive">Zaległe z ostatnich dni ({data.overdue.length})</h3>{data.overdue.map((entry) => <ScheduleEntryCard key={entry.id} entry={entry} showDate onChanged={agenda.reload} />)}</section>}
        {data.today.length ? <div className="space-y-2">{data.today.map((entry) => <ScheduleEntryCard key={entry.id} entry={entry} onChanged={agenda.reload} />)}</div>
          : <div className="space-y-3 rounded-lg border border-dashed p-4 text-sm text-muted-foreground"><p>Na dziś nie masz zaplanowanego treningu.</p><Button asChild size="sm" variant="outline"><Link to={`/kalendarz?dzien=${data.date}`}><CalendarPlus size={15} aria-hidden="true" />Zaplanuj trening</Link></Button></div>}
        {next && <section className="space-y-2" aria-labelledby="next-heading"><h3 id="next-heading" className="text-sm font-semibold">Najbliższy trening{data.upcoming.length > 1 && <span className="font-normal text-muted-foreground"> · w ciągu 7 dni jeszcze {data.upcoming.length - 1}</span>}</h3><ScheduleEntryCard entry={next} showDate onChanged={agenda.reload} /></section>}
      </>}
    </CardContent>
  </Card>
}

function WeekCard({ stats }: { stats: ReturnType<typeof useApiResource<StatsSummary>> }) {
  const data = stats.data
  const current = data?.weeks[data.weeks.length - 1]
  const change = data ? volumeChange(data.weeks) : null
  return <Card>
    <CardHeader className="flex-row items-center justify-between gap-2 space-y-0"><CardTitle className="flex items-center gap-2 text-lg"><ChartColumn size={18} className="text-primary" aria-hidden="true" />Ten tydzień</CardTitle><Link to="/statystyki" className="text-sm font-medium text-primary underline-offset-4 hover:underline">Statystyki</Link></CardHeader>
    <CardContent>
      <ResourceStatus loading={stats.loading} error={stats.error} onRetry={stats.reload} label="Wczytywanie podsumowania…" />
      {!stats.loading && !stats.error && current && <dl className="grid grid-cols-3 gap-2 rounded-lg bg-muted p-3 text-sm">
        <div className="min-w-0"><dt className="text-xs text-muted-foreground">Treningi</dt><dd className="mt-1 text-xl font-semibold">{current.sessions}</dd></div>
        <div className="min-w-0"><dt className="text-xs text-muted-foreground">Serie</dt><dd className="mt-1 text-xl font-semibold">{numberFormat.format(current.sets)}</dd></div>
        <div className="min-w-0"><dt className="text-xs text-muted-foreground">Objętość</dt><dd className="mt-1 break-words text-lg font-semibold sm:text-xl">{formatVolume(current.volume)}</dd>{change !== null && <dd className={`text-xs ${change >= 0 ? 'text-primary' : 'text-muted-foreground'}`}>{change >= 0 ? '+' : ''}{change}% vs poprzedni</dd>}</div>
      </dl>}
    </CardContent>
  </Card>
}
