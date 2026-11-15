import { useId, useState } from 'react'
import { Plus, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { exerciseTypeLabels, muscleGroupLabels, type Exercise } from '@/types/workout'

export function ExercisePicker({ catalog, onAdd }: { catalog: Exercise[]; onAdd: (exercise: Exercise) => void }) {
  const id = useId()
  const [query, setQuery] = useState('')
  const [group, setGroup] = useState('')
  const [type, setType] = useState('')
  const [message, setMessage] = useState('')
  const results = catalog.filter((exercise) => exercise.name.toLocaleLowerCase('pl').includes(query.trim().toLocaleLowerCase('pl')) && (!group || exercise.muscleGroup === group) && (!type || exercise.type === type))
  const selectClass = 'h-10 w-full min-w-0 rounded-md border bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'
  return <div className="space-y-4 rounded-lg border bg-muted/40 p-4">
    <h3 className="flex items-center gap-2 font-semibold"><Search size={17} aria-hidden="true" />Wybierz ćwiczenie</h3>
    <div className="space-y-2"><Label htmlFor={`${id}-search`}>Szukaj ćwiczenia</Label><Input id={`${id}-search`} type="search" maxLength={160} value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Np. pompki…" /></div>
    <div className="grid gap-3 sm:grid-cols-2">
      <div className="space-y-2"><Label htmlFor={`${id}-group`}>Partia mięśniowa</Label><select id={`${id}-group`} className={selectClass} value={group} onChange={(event) => setGroup(event.target.value)}><option value="">Wszystkie partie</option>{Object.entries(muscleGroupLabels).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></div>
      <div className="space-y-2"><Label htmlFor={`${id}-type`}>Typ ćwiczenia</Label><select id={`${id}-type`} className={selectClass} value={type} onChange={(event) => setType(event.target.value)}><option value="">Wszystkie typy</option>{Object.entries(exerciseTypeLabels).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></div>
    </div>
    <p role="status" className="text-xs text-muted-foreground">{message || `Liczba ćwiczeń: ${results.length}`}</p>
    <ul className="max-h-72 space-y-1 overflow-y-auto">{results.map((exercise) => <li key={exercise.id}><Button type="button" variant="ghost" className="h-auto min-h-11 w-full justify-between gap-3 whitespace-normal p-3 text-left" aria-label={`Dodaj: ${exercise.name}`} onClick={() => { onAdd(exercise); setMessage(`Dodano: ${exercise.name}`) }}><span className="min-w-0"><span className="block break-words">{exercise.name}</span><span className="mt-1 block text-xs font-normal text-muted-foreground">{muscleGroupLabels[exercise.muscleGroup]} · {exerciseTypeLabels[exercise.type]}</span></span><Plus size={18} className="shrink-0" aria-hidden="true" /></Button></li>)}</ul>
    {results.length === 0 && <p className="text-sm text-muted-foreground">Brak pasujących ćwiczeń. Zmień nazwę lub filtry.</p>}
  </div>
}
