import { useState, type FormEvent } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Notice } from '@/components/ui/notice'
import { ResourceStatus } from '@/components/ui/resource-status'
import { useApiResource } from '@/hooks/use-api-resource'
import { apiFetch } from '@/lib/api'
import { formatMoment, historyQuery, metricPoints, numberFormat, resultPoints } from '@/lib/progress'
import { metricLabels, type BodyMetric, type ExerciseResult, type MetricKey } from '@/types/progress'
import type { Exercise } from '@/types/workout'
import type { ApiError } from '@/types/auth'
import { ProgressChart } from './ProgressChart'

const selectClass = 'h-10 w-full min-w-0 rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'
const metricKeys = Object.keys(metricLabels) as MetricKey[]

export function ProgressHistory({ basePath = '/api', canDelete = true, refreshKey = 0 }: { basePath?: string; canDelete?: boolean; refreshKey?: number }) {
  const catalog = useApiResource<Exercise[]>('/api/exercises')
  const [exerciseId, setExerciseId] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [filters, setFilters] = useState({ exerciseId: '', from: '', to: '' })
  const [filterError, setFilterError] = useState('')
  const [metric, setMetric] = useState<MetricKey>('weightKg')
  const query = historyQuery(filters.exerciseId, filters.from, filters.to)
  const dateQuery = historyQuery('', filters.from, filters.to)
  // A changed key remounts the data component after a successful measurement save.
  function apply(event: FormEvent) {
    event.preventDefault()
    if (from && to && from > to) { setFilterError('Data początkowa nie może być późniejsza niż końcowa.'); return }
    setFilterError(''); setFilters({ exerciseId, from, to })
  }
  return <div className="space-y-6">
    <Card><CardHeader><CardTitle className="text-lg">Historia i wykresy</CardTitle></CardHeader><CardContent><form onSubmit={apply} className="space-y-4"><div className="grid gap-4 sm:grid-cols-2"><div className="space-y-2 sm:col-span-2"><Label htmlFor="progress-exercise">Ćwiczenie</Label><select id="progress-exercise" className={selectClass} value={exerciseId} onChange={(event) => setExerciseId(event.target.value)}><option value="">Wszystkie ćwiczenia</option>{catalog.data?.map((exercise) => <option key={exercise.id} value={exercise.id}>{exercise.name}</option>)}</select>{catalog.error && <p className="text-xs text-destructive">{catalog.error.message} <button type="button" className="underline" onClick={catalog.reload}>Ponów wczytanie ćwiczeń</button></p>}</div><div className="space-y-2"><Label htmlFor="progress-from">Od dnia</Label><Input type="date" id="progress-from" value={from} onChange={(event) => setFrom(event.target.value)} /></div><div className="space-y-2"><Label htmlFor="progress-to">Do dnia</Label><Input type="date" id="progress-to" value={to} onChange={(event) => setTo(event.target.value)} /></div></div>{filterError && <Notice variant="error">{filterError}</Notice>}<div className="flex gap-2"><Button type="submit">Pokaż historię</Button><Button type="button" variant="outline" onClick={() => { setExerciseId(''); setFrom(''); setTo(''); setFilters({ exerciseId: '', from: '', to: '' }); setFilterError('') }}>Wyczyść filtry</Button></div></form></CardContent></Card>
    <HistoryData key={`${basePath}-${query}-${dateQuery}-${refreshKey}`} basePath={basePath} query={query} dateQuery={dateQuery} selectedExerciseId={Number(filters.exerciseId) || null} canDelete={canDelete} metric={metric} setMetric={setMetric} />
  </div>
}

function HistoryData({ basePath, query, dateQuery, selectedExerciseId, canDelete, metric, setMetric }: { basePath: string; query: string; dateQuery: string; selectedExerciseId: number | null; canDelete: boolean; metric: MetricKey; setMetric: (metric: MetricKey) => void }) {
  const results = useApiResource<ExerciseResult[]>(`${basePath}/results${query}`)
  const metrics = useApiResource<BodyMetric[]>(`${basePath}/body-metrics${dateQuery}`)
  const [pending, setPending] = useState<{ kind: 'results' | 'body-metrics'; id: string; time: string } | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState('')
  const chartExerciseId = selectedExerciseId || results.data?.[0]?.exerciseId || 0
  const [chartChoice, setChartChoice] = useState<number | null>(null)
  const chosenId = chartChoice || chartExerciseId
  const choices = Array.from(new Map(results.data?.map((row) => [row.exerciseId, row.exerciseName])).entries())
  async function remove() {
    if (!pending || deleting) return
    setDeleting(true); setError('')
    try { await apiFetch(`/api/${pending.kind}/${pending.id}?${new URLSearchParams({ time: pending.time })}`, { method: 'DELETE' }); setPending(null); results.reload(); metrics.reload() }
    catch (err) { setError((err as ApiError).message || 'Nie udało się usunąć wpisu.') }
    finally { setDeleting(false) }
  }
  return <>
    {pending && <div className="space-y-3 rounded-xl border border-destructive/30 bg-card p-5" role="alert"><p className="text-sm">Czy trwale usunąć wybrany wpis z historii?</p>{error && <Notice variant="error">{error}</Notice>}<div className="flex flex-wrap gap-2"><Button disabled={deleting} onClick={remove} className="bg-destructive hover:bg-destructive/90">{deleting ? 'Usuwanie…' : 'Tak, usuń wpis'}</Button><Button disabled={deleting} variant="outline" onClick={() => { setPending(null); setError('') }}>Anuluj usuwanie</Button></div></div>}
    <Card><CardHeader><CardTitle className="text-lg">Ciężar w czasie</CardTitle><p className="text-sm text-muted-foreground">Największy zapisany ciężar z serii danego ćwiczenia w każdym dniu. Wykres nie przedstawia objętości treningu.</p></CardHeader><CardContent className="space-y-4"><ResourceStatus loading={results.loading} error={results.error} onRetry={results.reload} label="Wczytywanie wyników…" />{!results.loading && !results.error && results.data && <>{choices.length > 1 && <div className="space-y-2"><Label htmlFor="chart-exercise">Ćwiczenie na wykresie</Label><select id="chart-exercise" className={selectClass} value={chosenId} onChange={(event) => setChartChoice(Number(event.target.value))}>{choices.map(([id, name]) => <option key={id} value={id}>{name}</option>)}</select></div>}<ProgressChart title={choices.find(([id]) => id === chosenId)?.[1] || 'Ciężar ćwiczenia'} unit="kg" points={resultPoints(results.data, chosenId)} /></>}</CardContent></Card>
    <Card><CardHeader><CardTitle className="text-lg">Waga i obwody</CardTitle></CardHeader><CardContent className="space-y-4"><div className="space-y-2"><Label htmlFor="chart-metric">Pomiar na wykresie</Label><select id="chart-metric" className={selectClass} value={metric} onChange={(event) => setMetric(event.target.value as MetricKey)}>{metricKeys.map((key) => <option key={key} value={key}>{metricLabels[key]} ({key === 'weightKg' ? 'kg' : 'cm'})</option>)}</select></div><ResourceStatus loading={metrics.loading} error={metrics.error} onRetry={metrics.reload} label="Wczytywanie pomiarów…" />{!metrics.loading && !metrics.error && metrics.data && <ProgressChart title={metricLabels[metric]} unit={metric === 'weightKg' ? 'kg' : 'cm'} points={metricPoints(metrics.data, metric)} />}</CardContent></Card>
    <section aria-labelledby="result-history-heading" className="space-y-3"><h2 id="result-history-heading" className="text-xl font-semibold">Historia serii</h2>{!results.loading && !results.error && results.data && (results.data.length ? <><p className="text-xs text-muted-foreground">Od najnowszych. Liczba serii: {results.data.length}. Maksymalnie 2000 najnowszych wpisów dla wybranych filtrów.</p><ul className="divide-y rounded-xl border bg-card px-4 sm:px-5">{results.data.map((row) => <li key={`${row.id}-${row.time}`} className="space-y-2 py-4"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><p className="break-words font-medium">{row.exerciseName}</p><p className="mt-1 text-xs text-muted-foreground">{formatMoment(row.time)}</p></div>{canDelete && <Button variant="ghost" size="sm" disabled={deleting} onClick={() => { setPending({ kind: 'results', id: row.id, time: row.time }); setError('') }} aria-label={`Usuń serię ${row.setNo}: ${row.exerciseName}`}>Usuń</Button>}</div><p className="text-sm">Seria {row.setNo} · <strong>{row.reps}</strong> powtórzeń · <strong>{numberFormat.format(row.weight)} kg</strong></p><p className="break-words text-xs text-muted-foreground">{row.planName} · {row.dayName}</p></li>)}</ul></> : <p className="rounded-xl border border-dashed p-6 text-sm text-muted-foreground">Brak zapisanych serii dla wybranych filtrów.</p>)}</section>
    <section aria-labelledby="metric-history-heading" className="space-y-3"><h2 id="metric-history-heading" className="text-xl font-semibold">Historia pomiarów</h2>{!metrics.loading && !metrics.error && metrics.data && (metrics.data.length ? <><p className="text-xs text-muted-foreground">Maksymalnie 500 najnowszych pomiarów dla wybranego zakresu dat.</p><ul className="divide-y rounded-xl border bg-card px-4 sm:px-5">{metrics.data.map((row) => <li key={`${row.id}-${row.time}`} className="py-4"><div className="mb-3 flex items-center justify-between gap-2"><p className="text-sm font-medium">{formatMoment(row.time)}</p>{canDelete && <Button variant="ghost" size="sm" disabled={deleting} onClick={() => { setPending({ kind: 'body-metrics', id: row.id, time: row.time }); setError('') }} aria-label={`Usuń pomiar z ${formatMoment(row.time)}`}>Usuń</Button>}</div><dl className="grid grid-cols-2 gap-3 sm:grid-cols-3">{metricKeys.filter((key) => row[key] !== null && row[key] !== undefined).map((key) => <div key={key}><dt className="text-xs text-muted-foreground">{metricLabels[key]}</dt><dd className="mt-1 text-sm font-semibold">{numberFormat.format(row[key]!)} {key === 'weightKg' ? 'kg' : 'cm'}</dd></div>)}</dl></li>)}</ul></> : <p className="rounded-xl border border-dashed p-6 text-sm text-muted-foreground">Brak zapisanych pomiarów dla wybranego zakresu dat.</p>)}</section>
  </>
}
