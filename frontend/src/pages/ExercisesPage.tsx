import type { FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Dumbbell, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ResourceStatus } from '@/components/ui/resource-status'
import { useApiResource } from '@/hooks/use-api-resource'
import { exerciseListPath, exerciseSearchParams } from '@/lib/workout-filters'
import { exerciseTypeLabels, muscleGroupLabels, type Exercise } from '@/types/workout'

const selectClass = 'h-10 w-full min-w-0 rounded-md border bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

export function ExercisesPage() {
  const [params, setParams] = useSearchParams()
  const filters = { q: params.get('q') || '', muscleGroup: params.get('muscleGroup') || '', type: params.get('type') || '' }
  const { data, loading, error, reload } = useApiResource<Exercise[]>(exerciseListPath(filters))
  const filtered = Boolean(filters.q.trim() || filters.muscleGroup || filters.type)

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setParams(exerciseSearchParams({ q: String(form.get('q') || ''), muscleGroup: String(form.get('muscleGroup') || ''), type: String(form.get('type') || '') }))
  }

  return <div className="space-y-6">
    <div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Twój trening</p><h1 className="text-3xl font-semibold tracking-tight">Baza ćwiczeń</h1><p className="mt-2 text-sm leading-relaxed text-muted-foreground">Znajdź ćwiczenia na siłownię i do domu. Wybierz partię mięśniową lub wyszukaj nazwę.</p></div>
    <Card><CardContent className="pt-6">
      <form key={params.toString()} onSubmit={submit} className="space-y-4" aria-label="Filtry ćwiczeń">
        <div className="space-y-2"><Label htmlFor="exercise-search">Nazwa ćwiczenia</Label><Input id="exercise-search" name="q" type="search" maxLength={160} defaultValue={filters.q} placeholder="Np. pompki, przysiad…" /></div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2"><Label htmlFor="exercise-muscle">Partia mięśniowa</Label><select id="exercise-muscle" name="muscleGroup" defaultValue={filters.muscleGroup} className={selectClass}><option value="">Wszystkie partie</option>{Object.entries(muscleGroupLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
          <div className="space-y-2"><Label htmlFor="exercise-type">Typ ćwiczenia</Label><select id="exercise-type" name="type" defaultValue={filters.type} className={selectClass}><option value="">Wszystkie typy</option>{Object.entries(exerciseTypeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
        </div>
        <div className="flex flex-wrap gap-2"><Button type="submit"><Search size={16} aria-hidden="true" />Filtruj</Button>{filtered && <Button type="button" variant="ghost" onClick={() => setParams({})}>Wyczyść filtry</Button>}</div>
      </form>
    </CardContent></Card>
    <ResourceStatus loading={loading} error={error} onRetry={reload} label="Wczytywanie ćwiczeń…" />
    {!loading && !error && data && <>
      <p role="status" className="text-sm text-muted-foreground">Liczba wyników: <span className="font-semibold text-foreground">{data.length}</span></p>
      {data.length === 0 ? <Card><CardContent className="space-y-3 pt-6 text-center"><Search className="mx-auto text-muted-foreground" size={30} aria-hidden="true" /><h2 className="font-semibold">Brak pasujących ćwiczeń</h2><p className="text-sm text-muted-foreground">Zmień nazwę lub wybierz inne filtry.</p>{filtered && <Button variant="outline" onClick={() => setParams({})}>Pokaż wszystkie ćwiczenia</Button>}</CardContent></Card> :
        <ul className="grid gap-4 sm:grid-cols-2">{data.map((exercise) => <li key={exercise.id} className="min-w-0"><Card className="h-full"><CardHeader className="space-y-3"><div className="flex flex-wrap gap-2 text-xs"><span className="rounded-full bg-primary/8 px-2.5 py-1 font-medium text-primary">{muscleGroupLabels[exercise.muscleGroup]}</span><span className="rounded-full bg-muted px-2.5 py-1 text-muted-foreground">{exerciseTypeLabels[exercise.type]}</span></div><CardTitle className="flex items-start gap-2 break-words text-base leading-snug"><Dumbbell size={18} className="mt-0.5 shrink-0 text-primary" aria-hidden="true" />{exercise.name}</CardTitle></CardHeader><CardContent><p className="break-words text-sm leading-relaxed text-muted-foreground">{exercise.description || 'Opis niedostępny.'}</p></CardContent></Card></li>)}</ul>}
    </>}
  </div>
}
