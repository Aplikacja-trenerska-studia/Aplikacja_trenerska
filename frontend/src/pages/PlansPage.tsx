import { Link, useLocation } from 'react-router-dom'
import { ArrowRight, ClipboardList, Dumbbell, Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { ResourceStatus } from '@/components/ui/resource-status'
import { useApiResource } from '@/hooks/use-api-resource'
import type { PlanSummary } from '@/types/workout'
import { Notice } from '@/components/ui/notice'

export function PlansPage() {
  const { state } = useLocation()
  const { data, loading, error, reload } = useApiResource<PlanSummary[]>('/api/plans')
  return <div className="space-y-6">
    <div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Twój trening</p><h1 className="text-3xl font-semibold tracking-tight">Moje plany</h1><p className="mt-2 text-sm text-muted-foreground">Twoje plany treningowe, od najnowszego. Wybierz plan, aby zobaczyć dni i ćwiczenia.</p></div>
    <Button asChild><Link to="/plany/nowy"><Plus size={16} aria-hidden="true" />Nowy plan</Link></Button>
    {typeof state?.notice === 'string' && <Notice>{state.notice}</Notice>}
    <ResourceStatus loading={loading} error={error} onRetry={reload} label="Wczytywanie planów…" />
    {!loading && !error && data && (data.length === 0 ?
      <Card><CardContent className="space-y-4 py-10 text-center"><ClipboardList size={40} className="mx-auto text-primary/60" aria-hidden="true" /><h2 className="text-lg font-semibold">Nie masz jeszcze planów</h2><p className="text-sm leading-relaxed text-muted-foreground">Ułóż pierwszy plan i dodaj do niego ćwiczenia na siłownię lub do domu.</p><div className="flex flex-wrap justify-center gap-2"><Button asChild><Link to="/plany/nowy">Utwórz pierwszy plan</Link></Button><Button asChild variant="outline"><Link to="/cwiczenia"><Dumbbell size={16} aria-hidden="true" />Przejrzyj ćwiczenia</Link></Button></div></CardContent></Card> :
      <><p role="status" className="text-sm text-muted-foreground">Liczba planów: <span className="font-semibold text-foreground">{data.length}</span></p><ul className="space-y-3">{data.map((plan) => <li key={plan.id}><Link to={`/plany/${plan.id}`} className="block rounded-xl border bg-card p-5 shadow-sm transition-colors hover:border-primary/40 hover:bg-accent/30 sm:p-6"><div className="flex items-start gap-3"><ClipboardList size={22} className="mt-0.5 shrink-0 text-primary" aria-hidden="true" /><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h2 className="break-words text-lg font-semibold">{plan.name}</h2>{plan.shared && <span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">Udostępniony</span>}</div>{plan.description && <p className="mt-2 break-words text-sm leading-relaxed text-muted-foreground">{plan.description}</p>}<p className="mt-3 text-xs text-muted-foreground">Dodano: {formatPlanDate(plan.createdAt)}</p><span className="mt-4 flex items-center gap-2 text-sm font-medium text-primary">Zobacz plan<ArrowRight size={16} aria-hidden="true" /></span></div></div></Link></li>)}</ul></>)}
  </div>
}

function formatPlanDate(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : new Intl.DateTimeFormat('pl-PL', { day: 'numeric', month: 'long', year: 'numeric' }).format(date)
}
