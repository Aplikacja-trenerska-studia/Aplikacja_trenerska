import { Link, useSearchParams } from 'react-router-dom'
import { Plus, Trophy } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ResourceStatus } from '@/components/ui/resource-status'
import { WeeklyVolumeChart } from '@/components/stats/WeeklyVolumeChart'
import { useApiResource } from '@/hooks/use-api-resource'
import { numberFormat } from '@/lib/progress'
import { formatVolume, statRanges, summaryPath, weeksParam } from '@/lib/stats'
import type { ExerciseRecord, StatsSummary } from '@/types/stats'

const dateFormat = new Intl.DateTimeFormat('pl-PL', { day: 'numeric', month: 'short', year: 'numeric' })
const formatDate = (value: string | null) => value ? dateFormat.format(new Date(value)) : '—'

export function StatsPage() {
  const [params, setParams] = useSearchParams()
  const weeks = weeksParam(params.get('tygodnie'))
  const { data, loading, error, reload } = useApiResource<StatsSummary>(summaryPath(weeks))
  return <div className="space-y-6">
    <div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Twoje postępy</p><h1 className="text-3xl font-semibold tracking-tight">Statystyki</h1><p className="mt-2 text-sm text-muted-foreground">Podsumowanie zapisanych treningów, objętość w kolejnych tygodniach i rekordy w ćwiczeniach.</p></div>
    <div className="flex flex-wrap gap-2" role="group" aria-label="Zakres statystyk">{statRanges.map((range) => <Button key={range} type="button" size="sm" variant={range === weeks ? 'default' : 'outline'} aria-pressed={range === weeks} onClick={() => setParams({ tygodnie: String(range) })}>{range} tyg.</Button>)}</div>
    <ResourceStatus loading={loading} error={error} onRetry={reload} label="Wczytywanie statystyk…" />
    {!loading && !error && data && <>
      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {[
          { label: 'Treningi', value: numberFormat.format(data.summary.sessions) },
          { label: 'Średnio na tydzień', value: numberFormat.format(data.summary.averageSessionsPerWeek) },
          { label: 'Aktywne tygodnie', value: `${data.summary.activeWeeks} / ${data.weeks.length}` },
          { label: 'Serie', value: numberFormat.format(data.summary.sets) },
          { label: 'Powtórzenia', value: numberFormat.format(data.summary.reps) },
          { label: 'Objętość', value: formatVolume(data.summary.volume) },
        ].map((tile) => <div key={tile.label} className="min-w-0 rounded-xl border bg-card p-4 shadow-sm"><dt className="text-xs text-muted-foreground">{tile.label}</dt><dd className="mt-1 break-words text-2xl font-semibold tracking-tight">{tile.value}</dd></div>)}
      </dl>
      <Card><CardContent className="pt-6"><WeeklyVolumeChart weeks={data.weeks} /></CardContent></Card>
      <Card>
        <CardHeader><CardTitle className="flex items-center gap-2 text-lg"><Trophy size={18} className="text-primary" aria-hidden="true" />Rekordy w ćwiczeniach</CardTitle><p className="text-sm text-muted-foreground">Z całej historii, niezależnie od wybranego zakresu. Szacowane 1RM liczymy wzorem Epleya.</p></CardHeader>
        <CardContent>{data.records.length ? <ul className="divide-y">{data.records.map((record) => <RecordRow key={record.exerciseId} record={record} />)}</ul> : <div className="space-y-3 rounded-lg border border-dashed p-5 text-center text-sm text-muted-foreground"><p>Rekordy pojawią się po zapisaniu pierwszego treningu.</p><Button asChild size="sm"><Link to="/trening"><Plus size={15} aria-hidden="true" />Zapisz trening</Link></Button></div>}</CardContent>
      </Card>
      <p className="text-xs text-muted-foreground">Tygodnie od poniedziałku do niedzieli, strefa czasowa: {data.zone}. Objętość nie obejmuje ćwiczeń z ciężarem 0 kg.</p>
    </>}
  </div>
}

function RecordRow({ record }: { record: ExerciseRecord }) {
  return <li className="py-4 first:pt-0 last:pb-0">
    <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1"><h3 className="break-words font-medium">{record.exerciseName}</h3><span className="text-xs text-muted-foreground">serie: {record.totalSets} · ostatnio {formatDate(record.lastPerformedAt)}</span></div>
    <dl className="mt-3 grid grid-cols-3 gap-2 rounded-lg bg-muted p-3 text-sm">
      <div className="min-w-0"><dt className="text-xs text-muted-foreground">Największy ciężar</dt><dd className="mt-1 font-semibold">{record.maxWeight === null ? '—' : `${numberFormat.format(record.maxWeight)} kg × ${record.maxWeightReps}`}</dd><dd className="text-xs text-muted-foreground">{formatDate(record.maxWeightAt)}</dd></div>
      <div className="min-w-0"><dt className="text-xs text-muted-foreground">Szacowane 1RM</dt><dd className="mt-1 font-semibold">{record.estimatedOneRepMax === null ? '—' : `${numberFormat.format(record.estimatedOneRepMax)} kg`}</dd><dd className="text-xs text-muted-foreground">{formatDate(record.estimatedOneRepMaxAt)}</dd></div>
      <div className="min-w-0"><dt className="text-xs text-muted-foreground">Najwięcej powtórzeń</dt><dd className="mt-1 font-semibold">{record.maxReps}</dd><dd className="text-xs text-muted-foreground">{formatDate(record.maxRepsAt)}</dd></div>
    </dl>
  </li>
}
