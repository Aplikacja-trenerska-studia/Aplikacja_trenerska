import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Check, Pencil, Play, Trash2, Undo2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Notice } from '@/components/ui/notice'
import { apiFetch } from '@/lib/api'
import { isoDate, relativeDay, workoutLink } from '@/lib/schedule'
import type { ApiError } from '@/types/auth'
import type { ScheduledWorkout } from '@/types/schedule'

export function ScheduleEntryCard({ entry, onChanged, onEdit, showDate = false }: {
  entry: ScheduledWorkout; onChanged: () => void; onEdit?: () => void; showDate?: boolean
}) {
  const [busy, setBusy] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [error, setError] = useState('')
  const today = isoDate()
  const overdue = !entry.completed && entry.date < today
  const link = workoutLink(entry)

  async function run(path: string, method: 'POST' | 'DELETE', failure: string) {
    if (busy) return
    setBusy(true); setError('')
    try {
      await apiFetch(path, { method })
      setConfirming(false)
      onChanged()
    } catch (problem) { setError((problem as ApiError).message || failure) }
    finally { setBusy(false) }
  }

  return <article className={`space-y-3 rounded-xl border p-4 ${entry.completed ? 'bg-muted/50' : 'bg-card'}`}>
    <div className="flex items-start justify-between gap-3">
      <div className="min-w-0">
        {showDate && <p className="text-xs font-semibold uppercase tracking-wide text-primary">{relativeDay(entry.date, today)}</p>}
        <h3 className={`break-words font-semibold ${entry.completed ? 'text-muted-foreground line-through decoration-1' : ''}`}>{entry.dayName}</h3>
        <p className="break-words text-sm text-muted-foreground">{entry.planName}{!entry.planId && ' · plan usunięty'}</p>
        {entry.note && <p className="mt-2 whitespace-pre-line break-words text-sm">{entry.note}</p>}
      </div>
      <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${entry.completed ? 'bg-primary/10 text-primary' : overdue ? 'bg-destructive/10 text-destructive' : 'bg-muted text-muted-foreground'}`}>
        {entry.completed ? 'Wykonany' : overdue ? 'Zaległy' : 'Zaplanowany'}
      </span>
    </div>
    {error && <Notice variant="error">{error}</Notice>}
    {confirming ? <div className="space-y-3 rounded-lg border p-3">
      <p className="text-sm">Usunąć ten trening z kalendarza? Zapisane wyniki pozostaną w historii.</p>
      <div className="flex flex-wrap gap-2">
        <Button type="button" size="sm" disabled={busy} className="bg-destructive hover:bg-destructive/90" onClick={() => run(`/api/schedule/${entry.id}`, 'DELETE', 'Nie udało się usunąć wpisu.')}>{busy ? 'Usuwanie…' : 'Usuń'}</Button>
        <Button type="button" size="sm" variant="ghost" disabled={busy} onClick={() => setConfirming(false)}>Anuluj</Button>
      </div>
    </div> : <div className="flex flex-wrap gap-2">
      {link && !entry.completed && <Button asChild size="sm"><Link to={link}><Play size={15} aria-hidden="true" />Rozpocznij</Link></Button>}
      {entry.completed
        ? <Button type="button" size="sm" variant="outline" disabled={busy} onClick={() => run(`/api/schedule/${entry.id}/complete`, 'DELETE', 'Nie udało się cofnąć oznaczenia.')}><Undo2 size={15} aria-hidden="true" />Cofnij wykonanie</Button>
        : <Button type="button" size="sm" variant="outline" disabled={busy} onClick={() => run(`/api/schedule/${entry.id}/complete`, 'POST', 'Nie udało się oznaczyć treningu.')}><Check size={15} aria-hidden="true" />Wykonany</Button>}
      {onEdit && <Button type="button" size="sm" variant="ghost" disabled={busy} onClick={onEdit}><Pencil size={15} aria-hidden="true" />Edytuj</Button>}
      {onEdit && <Button type="button" size="sm" variant="ghost" className="text-destructive" disabled={busy} onClick={() => setConfirming(true)} aria-label={`Usuń trening ${entry.dayName} z kalendarza`}><Trash2 size={15} aria-hidden="true" /></Button>}
    </div>}
  </article>
}
