import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ClipboardList, Copy, Share2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Notice } from '@/components/ui/notice'
import { ResourceStatus } from '@/components/ui/resource-status'
import { useApiResource } from '@/hooks/use-api-resource'
import { apiFetch } from '@/lib/api'
import { isShareToken } from '@/lib/sharing'
import type { ApiError } from '@/types/auth'
import { exerciseTypeLabels, muscleGroupLabels, type SharedPlan, type WorkoutPlan } from '@/types/workout'

const weightFormat = new Intl.NumberFormat('pl-PL', { maximumFractionDigits: 2 })

export function SharedPlanPage() {
  const { token } = useParams()
  if (!isShareToken(token)) {
    return <div className="space-y-4"><Notice variant="error">Link do planu jest nieprawidłowy. Poproś autora o nowy link.</Notice><Button asChild variant="outline"><Link to="/plany">Moje plany</Link></Button></div>
  }
  return <SharedPlanView token={token} />
}

function SharedPlanView({ token }: { token: string }) {
  const navigate = useNavigate()
  const { data, loading, error, reload } = useApiResource<SharedPlan>(`/api/shared-plans/${token}`)
  const [busy, setBusy] = useState(false)
  const [copyError, setCopyError] = useState('')

  async function copy() {
    if (busy) return
    setBusy(true); setCopyError('')
    try {
      const plan = await apiFetch<WorkoutPlan>(`/api/shared-plans/${token}/copy`, { method: 'POST' })
      navigate(`/plany/${plan.id}`, { state: { notice: 'Plan został skopiowany do Twoich planów. Możesz go dowolnie edytować.' } })
    } catch (failure) { setCopyError((failure as ApiError).message || 'Nie udało się skopiować planu. Spróbuj ponownie.') }
    finally { setBusy(false) }
  }

  return <div className="space-y-6">
    <ResourceStatus loading={loading} error={error} onRetry={reload} label="Wczytywanie udostępnionego planu…" />
    {!loading && !error && data && <>
      <div>
        <p className="mb-2 flex items-center gap-2 text-xs font-semibold uppercase tracking-widest text-primary"><Share2 size={14} aria-hidden="true" />Udostępniony plan</p>
        <h1 className="break-words text-3xl font-semibold tracking-tight">{data.name}</h1>
        <p className="mt-2 text-sm text-muted-foreground">Autor: <span className="font-medium text-foreground">{data.ownerName}</span> · dni treningowe: {data.days.length}</p>
        {data.description && <p className="mt-3 whitespace-pre-line break-words text-sm leading-relaxed text-muted-foreground">{data.description}</p>}
      </div>
      {data.own ? <Notice>To Twój plan. Tak widzą go osoby, którym wyślesz link.</Notice> : <Card>
        <CardContent className="space-y-3 pt-6">
          <p className="text-sm leading-relaxed text-muted-foreground">Zapisz kopię planu na swoim koncie. Kopia jest niezależna: możesz ją edytować, a późniejsze zmiany autora jej nie zmienią.</p>
          {copyError && <Notice variant="error">{copyError}</Notice>}
          <Button type="button" disabled={busy} onClick={copy}><Copy size={16} aria-hidden="true" />{busy ? 'Kopiowanie…' : 'Skopiuj do moich planów'}</Button>
        </CardContent>
      </Card>}
      {data.days.map((day) => <Card key={day.id}>
        <CardHeader><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Dzień {day.dayIndex + 1}</p><CardTitle className="break-words text-xl leading-snug">{day.name}</CardTitle></CardHeader>
        <CardContent><ol className="divide-y">{day.exercises.map((entry) => <li key={entry.id} className="py-5 first:pt-0 last:pb-0">
          <h3 className="break-words font-medium">{entry.orderIndex + 1}. {entry.exercise.name}</h3>
          <p className="mt-1 text-xs text-muted-foreground">{muscleGroupLabels[entry.exercise.muscleGroup]} · {exerciseTypeLabels[entry.exercise.type]}</p>
          <dl className="mt-4 grid grid-cols-3 gap-2 rounded-lg bg-muted p-3 text-sm"><div><dt className="text-xs text-muted-foreground">Serie</dt><dd className="mt-1 font-semibold">{entry.sets}</dd></div><div><dt className="text-xs text-muted-foreground">Powtórzenia</dt><dd className="mt-1 font-semibold">{entry.reps}</dd></div><div><dt className="text-xs text-muted-foreground">Ciężar</dt><dd className="mt-1 font-semibold">{weightFormat.format(entry.weight)} kg</dd></div></dl>
        </li>)}</ol></CardContent>
      </Card>)}
      <Button asChild variant="outline"><Link to="/plany"><ClipboardList size={16} aria-hidden="true" />Moje plany</Link></Button>
    </>}
  </div>
}
