import { Link, useLocation, useParams } from 'react-router-dom'
import { ArrowLeft, Pencil, Play } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Notice } from '@/components/ui/notice'
import { DeletePlanButton } from '@/components/workout/DeletePlanButton'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ResourceStatus } from '@/components/ui/resource-status'
import { useApiResource } from '@/hooks/use-api-resource'
import { exerciseTypeLabels, muscleGroupLabels, type WorkoutPlan } from '@/types/workout'

const weightFormat = new Intl.NumberFormat('pl-PL', { maximumFractionDigits: 2 })

export function PlanDetailsPage() {
  const { state } = useLocation()
  const { id } = useParams()
  const { data, loading, error, reload } = useApiResource<WorkoutPlan>(`/api/plans/${encodeURIComponent(id || '')}`)
  return <div className="space-y-6">
    <Link to="/plany" className="inline-flex items-center gap-2 text-sm font-medium text-primary"><ArrowLeft size={16} aria-hidden="true" />Moje plany</Link>
    <ResourceStatus loading={loading} error={error} onRetry={reload} label="Wczytywanie planu…" />
    {!loading && !error && data && <>
      {typeof state?.notice === 'string' && <Notice>{state.notice}</Notice>}
      <div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Plan treningowy</p><h1 className="break-words text-3xl font-semibold tracking-tight">{data.name}</h1>{data.description && <p className="mt-3 whitespace-pre-line break-words text-sm leading-relaxed text-muted-foreground">{data.description}</p>}</div>
      <div className="flex flex-wrap gap-2"><Button asChild><Link to={`/trening?planId=${data.id}`}><Play size={16} aria-hidden="true" />Wykonaj trening</Link></Button><Button asChild variant="outline"><Link to={`/plany/${data.id}/edytuj`}><Pencil size={16} aria-hidden="true" />Edytuj plan</Link></Button><DeletePlanButton id={data.id} name={data.name} /></div>
      {data.days.map((day) => <Card key={day.id}><CardHeader><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Dzień {day.dayIndex + 1}</p><CardTitle className="break-words text-xl leading-snug">{day.name}</CardTitle></CardHeader><CardContent><ol className="divide-y">{day.exercises.map((entry) => <li key={entry.id} className="py-5 first:pt-0 last:pb-0"><h3 className="break-words font-medium">{entry.orderIndex + 1}. {entry.exercise.name}</h3><p className="mt-1 text-xs text-muted-foreground">{muscleGroupLabels[entry.exercise.muscleGroup]} · {exerciseTypeLabels[entry.exercise.type]}</p><dl className="mt-4 grid grid-cols-3 gap-2 rounded-lg bg-muted p-3 text-sm"><div><dt className="text-xs text-muted-foreground">Serie</dt><dd className="mt-1 font-semibold">{entry.sets}</dd></div><div><dt className="text-xs text-muted-foreground">Powtórzenia</dt><dd className="mt-1 font-semibold">{entry.reps}</dd></div><div><dt className="text-xs text-muted-foreground">Ciężar</dt><dd className="mt-1 font-semibold">{weightFormat.format(entry.weight)} kg</dd></div></dl></li>)}</ol></CardContent></Card>)}
    </>}
  </div>
}
