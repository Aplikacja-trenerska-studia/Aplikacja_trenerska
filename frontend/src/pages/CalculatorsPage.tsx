import { useState, type FormEvent } from 'react'
import { Calculator, Flame } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Notice } from '@/components/ui/notice'
import { apiFetch } from '@/lib/api'
import { caloriesQuery, macroShares, oneRepMaxQuery, type CalorieForm } from '@/lib/calculators'
import { numberFormat } from '@/lib/progress'
import type { ApiError } from '@/types/auth'
import { activityLabels, goalLabels, sexLabels, type ActivityLevel, type CalorieResult, type Goal, type OneRepMaxResult, type Sex } from '@/types/calc'

const selectClass = 'h-10 w-full min-w-0 rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'
const kcal = new Intl.NumberFormat('pl-PL', { maximumFractionDigits: 0 })

export function CalculatorsPage() {
  return <div className="space-y-6">
    <div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Narzędzia</p><h1 className="text-3xl font-semibold tracking-tight">Kalkulatory</h1><p className="mt-2 text-sm text-muted-foreground">Oszacuj ciężar maksymalny i dzienne zapotrzebowanie kaloryczne. Wyniki są orientacyjne.</p></div>
    <OneRepMaxCard />
    <CaloriesCard />
  </div>
}

function OneRepMaxCard() {
  const [weight, setWeight] = useState('')
  const [reps, setReps] = useState('')
  const [result, setResult] = useState<OneRepMaxResult | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    const { query, error: invalid } = oneRepMaxQuery(weight, reps)
    if (!query) { setError(invalid || ''); return }
    setBusy(true); setError('')
    try { setResult(await apiFetch<OneRepMaxResult>(`/api/calc/1rm?${query}`)) }
    catch (failure) { setError((failure as ApiError).message || 'Nie udało się obliczyć wyniku.') }
    finally { setBusy(false) }
  }

  return <Card>
    <CardHeader>
      <CardTitle className="flex items-center gap-2 text-lg"><Calculator size={18} className="text-primary" aria-hidden="true" />Ciężar maksymalny (1RM)</CardTitle>
      <p className="text-sm leading-relaxed text-muted-foreground">Wpisz ciężar i liczbę powtórzeń wykonanych w jednej serii. Wynik główny liczymy wzorem Epleya, pozostałe wzory pokazujemy dla porównania.</p>
    </CardHeader>
    <CardContent className="space-y-5">
      <form onSubmit={submit} noValidate>
        <fieldset disabled={busy} className="grid gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
          <div className="min-w-0 space-y-2"><Label htmlFor="orm-weight">Ciężar (kg)</Label><Input id="orm-weight" inputMode="decimal" placeholder="np. 80" value={weight} onChange={(event) => setWeight(event.target.value)} /></div>
          <div className="min-w-0 space-y-2"><Label htmlFor="orm-reps">Powtórzenia (1–20)</Label><Input id="orm-reps" inputMode="numeric" placeholder="np. 5" value={reps} onChange={(event) => setReps(event.target.value)} /></div>
          <Button type="submit">{busy ? 'Liczenie…' : 'Oblicz'}</Button>
        </fieldset>
      </form>
      {error && <Notice variant="error">{error}</Notice>}
      {result && <div className="space-y-4" aria-live="polite">
        <div className="rounded-xl bg-primary/5 p-5 text-center">
          <p className="text-xs font-semibold uppercase tracking-widest text-primary">Szacowane 1RM</p>
          <p className="mt-1 text-4xl font-semibold tracking-tight">{numberFormat.format(result.epley)} kg</p>
          <p className="mt-2 text-xs text-muted-foreground">{numberFormat.format(result.weight)} kg × {result.reps} powt. · Brzycki: {numberFormat.format(result.brzycki)} kg · Lombardi: {numberFormat.format(result.lombardi)} kg</p>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <caption className="mb-2 text-left text-xs text-muted-foreground">Ciężary treningowe jako procent 1RM, zaokrąglone do 0,5 kg, z orientacyjną liczbą powtórzeń.</caption>
            <thead><tr className="text-xs text-muted-foreground"><th scope="col" className="py-2 pr-3 font-medium">% 1RM</th><th scope="col" className="py-2 pr-3 font-medium">Ciężar</th><th scope="col" className="py-2 font-medium">Powtórzenia</th></tr></thead>
            <tbody>{result.percentages.map((row) => <tr key={row.percent} className="border-t"><td className="py-2 pr-3 font-medium">{row.percent}%</td><td className="py-2 pr-3">{numberFormat.format(row.weight)} kg</td><td className="py-2">≈ {row.reps}</td></tr>)}</tbody>
          </table>
        </div>
      </div>}
    </CardContent>
  </Card>
}

function CaloriesCard() {
  const [form, setForm] = useState<CalorieForm>({ sex: 'MALE', age: '', heightCm: '', weightKg: '', activity: 'MODERATE', goal: 'MAINTAIN' })
  const [result, setResult] = useState<CalorieResult | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const update = (change: Partial<CalorieForm>) => setForm((current) => ({ ...current, ...change }))

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    const { query, error: invalid } = caloriesQuery(form)
    if (!query) { setError(invalid || ''); return }
    setBusy(true); setError('')
    try { setResult(await apiFetch<CalorieResult>(`/api/calc/calories?${query}`)) }
    catch (failure) { setError((failure as ApiError).message || 'Nie udało się obliczyć zapotrzebowania.') }
    finally { setBusy(false) }
  }

  const shares = result ? macroShares(result.macros) : null
  return <Card>
    <CardHeader>
      <CardTitle className="flex items-center gap-2 text-lg"><Flame size={18} className="text-primary" aria-hidden="true" />Kalorie i makroskładniki</CardTitle>
      <p className="text-sm leading-relaxed text-muted-foreground">Podstawową przemianę materii liczymy wzorem Mifflina-St Jeora, a następnie mnożymy przez współczynnik aktywności. Białko zależy od masy ciała i celu, tłuszcze to 25% kalorii, resztę stanowią węglowodany.</p>
    </CardHeader>
    <CardContent className="space-y-5">
      <form onSubmit={submit} noValidate>
        <fieldset disabled={busy} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="min-w-0 space-y-2"><Label htmlFor="cal-sex">Płeć</Label><select id="cal-sex" className={selectClass} value={form.sex} onChange={(event) => update({ sex: event.target.value as Sex })}>{(Object.keys(sexLabels) as Sex[]).map((key) => <option key={key} value={key}>{sexLabels[key]}</option>)}</select></div>
            <div className="min-w-0 space-y-2"><Label htmlFor="cal-age">Wiek (lata)</Label><Input id="cal-age" inputMode="numeric" value={form.age} onChange={(event) => update({ age: event.target.value })} /></div>
            <div className="min-w-0 space-y-2"><Label htmlFor="cal-height">Wzrost (cm)</Label><Input id="cal-height" inputMode="numeric" value={form.heightCm} onChange={(event) => update({ heightCm: event.target.value })} /></div>
            <div className="min-w-0 space-y-2"><Label htmlFor="cal-weight">Masa ciała (kg)</Label><Input id="cal-weight" inputMode="decimal" value={form.weightKg} onChange={(event) => update({ weightKg: event.target.value })} /></div>
          </div>
          <div className="space-y-2"><Label htmlFor="cal-activity">Aktywność</Label><select id="cal-activity" className={selectClass} value={form.activity} onChange={(event) => update({ activity: event.target.value as ActivityLevel })}>{(Object.keys(activityLabels) as ActivityLevel[]).map((key) => <option key={key} value={key}>{activityLabels[key].label} — {activityLabels[key].hint}</option>)}</select></div>
          <div className="space-y-2"><Label htmlFor="cal-goal">Cel</Label><select id="cal-goal" className={selectClass} value={form.goal} onChange={(event) => update({ goal: event.target.value as Goal })}>{(Object.keys(goalLabels) as Goal[]).map((key) => <option key={key} value={key}>{goalLabels[key]}</option>)}</select></div>
          <Button type="submit">{busy ? 'Liczenie…' : 'Oblicz zapotrzebowanie'}</Button>
        </fieldset>
      </form>
      {error && <Notice variant="error">{error}</Notice>}
      {result && shares && <div className="space-y-4" aria-live="polite">
        <div className="rounded-xl bg-primary/5 p-5 text-center">
          <p className="text-xs font-semibold uppercase tracking-widest text-primary">Dzienny cel</p>
          <p className="mt-1 text-4xl font-semibold tracking-tight">{kcal.format(result.targetCalories)} kcal</p>
          <p className="mt-2 text-xs text-muted-foreground">PPM: {kcal.format(result.bmr)} kcal · CPM: {kcal.format(result.tdee)} kcal (×{numberFormat.format(result.activityFactor)}){result.calorieChange !== 0 && ` · ${result.calorieChange > 0 ? '+' : '−'}${kcal.format(Math.abs(result.calorieChange))} kcal względem CPM`}</p>
        </div>
        <div className="flex h-3 overflow-hidden rounded-full bg-muted" aria-hidden="true">
          <div className="bg-primary" style={{ width: `${shares.protein}%` }} />
          <div className="bg-primary/55" style={{ width: `${shares.fat}%` }} />
          <div className="bg-primary/25" style={{ width: `${shares.carbs}%` }} />
        </div>
        <dl className="grid grid-cols-3 gap-2 text-sm">
          {[
            { label: 'Białko', grams: result.macros.proteinG, energy: result.macros.proteinKcal, share: shares.protein, swatch: 'bg-primary' },
            { label: 'Tłuszcze', grams: result.macros.fatG, energy: result.macros.fatKcal, share: shares.fat, swatch: 'bg-primary/55' },
            { label: 'Węglowodany', grams: result.macros.carbsG, energy: result.macros.carbsKcal, share: shares.carbs, swatch: 'bg-primary/25' },
          ].map((macro) => <div key={macro.label} className="min-w-0 rounded-lg bg-muted px-2.5 py-3 sm:p-3">
            <dt className="flex min-w-0 flex-col items-start gap-1 text-[11px] text-muted-foreground sm:flex-row sm:items-center sm:gap-1.5 sm:text-xs"><span className={`size-2 shrink-0 rounded-full sm:size-2.5 ${macro.swatch}`} aria-hidden="true" /><span className="min-w-0">{macro.label}</span></dt>
            <dd className="mt-1 text-lg font-semibold">{macro.grams} g</dd>
            <dd className="text-xs text-muted-foreground">{kcal.format(macro.energy)} kcal · {macro.share}%</dd>
          </div>)}
        </dl>
        <p className="text-xs leading-relaxed text-muted-foreground">PPM — podstawowa przemiana materii, CPM — całkowita przemiana materii. Kalkulator nie zastępuje porady dietetyka ani lekarza.</p>
      </div>}
    </CardContent>
  </Card>
}
