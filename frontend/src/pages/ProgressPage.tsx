import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Notice } from '@/components/ui/notice'
import { ProgressHistory } from '@/components/progress/ProgressHistory'
import { apiFetch } from '@/lib/api'
import { decimal, localDateTime, toInstant } from '@/lib/progress'
import { metricLabels, type MetricKey } from '@/types/progress'
import type { ApiError } from '@/types/auth'

const keys = Object.keys(metricLabels) as MetricKey[]
const bounds: Record<MetricKey, [number, number]> = { weightKg: [10, 500], chestCm: [10, 300], waistCm: [10, 300], hipsCm: [10, 300], armCm: [5, 150], thighCm: [5, 200] }
const empty = () => Object.fromEntries(keys.map((key) => [key, ''])) as Record<MetricKey, string>

export function ProgressPage() {
  const [refresh, setRefresh] = useState(0)
  return <div className="space-y-6"><div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Twoje postępy</p><h1 className="text-3xl font-semibold tracking-tight">Progres</h1><p className="mt-2 text-sm text-muted-foreground">Historia wykonanych treningów, waga i obwody w jednym miejscu.</p></div><Button asChild><Link to="/trening"><Plus size={16} aria-hidden="true" />Zapisz trening</Link></Button><MetricForm onSaved={() => setRefresh((value) => value + 1)} /><ProgressHistory refreshKey={refresh} /></div>
}

function MetricForm({ onSaved }: { onSaved: () => void }) {
  const [open, setOpen] = useState(false)
  const [values, setValues] = useState(empty)
  const [time, setTime] = useState(localDateTime())
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (saving) return
    setError(''); setSaved(false)
    const instant = toInstant(time)
    if (!instant) { setError('Wpisz prawidłową datę i godzinę pomiaru.'); return }
    const payload: Record<string, number | string | null> = { time: instant }
    let filled = false
    for (const key of keys) {
      if (!values[key].trim()) { payload[key] = null; continue }
      const [min, max] = bounds[key]
      const parsed = decimal(values[key], min, max)
      if (parsed === null) { setError(`${metricLabels[key]}: wpisz wartość od ${min} do ${max} ${key === 'weightKg' ? 'kg' : 'cm'}, do dwóch miejsc po przecinku.`); return }
      payload[key] = parsed; filled = true
    }
    if (!filled) { setError('Uzupełnij przynajmniej jeden pomiar. Pozostałe pola mogą być puste.'); return }
    setSaving(true)
    try { await apiFetch('/api/body-metrics', { method: 'POST', body: JSON.stringify(payload) }); setValues(empty()); setSaved(true); setOpen(false); onSaved() }
    catch (err) { setError((err as ApiError).message || 'Nie udało się zapisać pomiaru.') }
    finally { setSaving(false) }
  }
  return <Card><CardHeader><CardTitle className="text-lg">Dodaj pomiar ciała</CardTitle><p className="text-sm text-muted-foreground">Zapisz samą wagę lub dowolne obwody. Nie musisz wypełniać wszystkich pól.</p></CardHeader><CardContent className="space-y-4">{saved && <Notice>Pomiar zapisany.</Notice>}{!open ? <Button variant="outline" onClick={() => { setOpen(true); setSaved(false); setTime(localDateTime()) }}>Nowy pomiar</Button> : <form onSubmit={submit}><fieldset disabled={saving} className="space-y-4"><div className="space-y-2"><Label htmlFor="metric-time">Data i godzina pomiaru</Label><Input id="metric-time" type="datetime-local" required value={time} onChange={(event) => setTime(event.target.value)} /></div><div className="grid grid-cols-2 gap-4">{keys.map((key) => <div className="min-w-0 space-y-2" key={key}><Label htmlFor={`metric-${key}`}>{metricLabels[key]} ({key === 'weightKg' ? 'kg' : 'cm'})</Label><Input id={`metric-${key}`} inputMode="decimal" value={values[key]} onChange={(event) => setValues((current) => ({ ...current, [key]: event.target.value }))} /></div>)}</div>{error && <Notice variant="error">{error}</Notice>}<div className="flex flex-wrap gap-2"><Button type="submit" disabled={saving}>{saving ? 'Zapisywanie…' : 'Zapisz pomiar'}</Button><Button variant="ghost" type="button" onClick={() => { setOpen(false); setError('') }}>Anuluj</Button></div></fieldset></form>}</CardContent></Card>
}
