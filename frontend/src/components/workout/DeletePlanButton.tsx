import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Notice } from '@/components/ui/notice'
import { apiFetch } from '@/lib/api'
import type { ApiError } from '@/types/auth'

export function DeletePlanButton({ id, name }: { id: number; name: string }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function remove() {
    if (busy) return
    setBusy(true); setError('')
    try {
      await apiFetch<void>(`/api/plans/${id}`, { method: 'DELETE' })
      dialog.current?.close()
      navigate('/plany', { replace: true, state: { notice: 'Plan został usunięty.' } })
    } catch (failure) { setError((failure as ApiError).message || 'Nie udało się usunąć planu. Spróbuj ponownie.') }
    finally { setBusy(false) }
  }
  return <><Button type="button" variant="outline" className="text-destructive" onClick={() => { setError(''); dialog.current?.showModal() }}><Trash2 size={16} aria-hidden="true" />Usuń plan</Button><dialog ref={dialog} aria-labelledby="delete-plan-title" aria-describedby="delete-plan-description" className="m-auto w-[calc(100%_-_2rem)] max-w-md rounded-xl border bg-card p-6 text-foreground shadow-xl backdrop:bg-foreground/40" onCancel={(event) => { if (busy) event.preventDefault() }}>
    <h2 id="delete-plan-title" className="text-xl font-semibold">Usunąć plan?</h2><p id="delete-plan-description" className="mt-3 break-words text-sm leading-relaxed text-muted-foreground">Plan „{name}” oraz jego dni i ćwiczenia zostaną trwale usunięte. Tej operacji nie można cofnąć.</p>
    {error && <div className="mt-4"><Notice variant="error">{error}</Notice></div>}
    <div className="mt-6 flex flex-wrap justify-end gap-2"><Button type="button" variant="outline" disabled={busy} onClick={() => dialog.current?.close()}>Anuluj</Button><Button type="button" disabled={busy} className="bg-destructive hover:bg-destructive/90" onClick={remove}>{busy ? 'Usuwanie…' : 'Potwierdź usunięcie'}</Button></div>
  </dialog></>
}
