import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useOutletContext, useSearchParams } from 'react-router-dom'
import { ArrowLeft, ArrowRight, Search, ShieldCheck, UsersRound } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Notice } from '@/components/ui/notice'
import { ResourceStatus } from '@/components/ui/resource-status'
import { useApiResource } from '@/hooks/use-api-resource'
import { apiFetch } from '@/lib/api'
import { accountStatus, adminSearchParams, adminUsersPath, canChangeRole, readAdminFilters, type AdminFilters } from '@/lib/admin'
import { accountStatusLabels, adminRoleLabels, type AdminOverview, type AdminUser, type AdminUserPage } from '@/types/admin'
import type { ApiError, UserResponse, UserRole } from '@/types/auth'

const selectClass = 'h-10 w-full min-w-0 rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'
type PendingAction = { user: AdminUser; kind: 'role'; role: UserRole } | { user: AdminUser; kind: 'block'; blocked: boolean }
const roleOptions = Object.entries(adminRoleLabels) as [UserRole, string][]

export function AdminPage() {
  const actor = useOutletContext<UserResponse>()
  const [params, setParams] = useSearchParams()
  const filters = readAdminFilters(params)
  const overview = useApiResource<AdminOverview>('/api/admin/overview')
  const users = useApiResource<AdminUserPage>(adminUsersPath(filters))
  const [pending, setPending] = useState<PendingAction | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  function propose(action: PendingAction) {
    if (busy || pending) return
    setError(''); setNotice(''); setPending(action)
  }
  async function confirm(event: FormEvent) {
    event.preventDefault()
    if (!pending || busy) return
    setBusy(true); setError('')
    try {
      const action = pending.kind === 'role' ? { path: 'role', body: { role: pending.role } } : { path: 'block', body: { blocked: pending.blocked } }
      await apiFetch<AdminUser>(`/api/admin/users/${pending.user.id}/${action.path}`, { method: 'PATCH', body: JSON.stringify(action.body) })
      setNotice(pending.kind === 'role' ? `Rola konta ${pending.user.email} została zmieniona. Użytkownik musi zalogować się ponownie.` : pending.blocked ? `Konto ${pending.user.email} zostało zablokowane. Jego aktywne sesje utraciły dostęp.` : `Konto ${pending.user.email} zostało odblokowane.${pending.user.enabled ? ' Użytkownik może zalogować się ponownie.' : ' Nadal wymaga potwierdzenia adresu e-mail.'}`)
      setPending(null); users.reload(); overview.reload()
    } catch (err) { setError((err as ApiError).message || 'Nie udało się zapisać zmiany. Możesz spróbować ponownie.') }
    finally { setBusy(false) }
  }

  return <div className="space-y-6">
    <div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-primary">Zarządzanie aplikacją</p><h1 className="text-3xl font-semibold tracking-tight">Panel administratora</h1><p className="mt-2 text-sm leading-relaxed text-muted-foreground">Zarządzaj dostępem do kont i uprawnieniami użytkowników.</p></div>
    <section className="space-y-3" aria-labelledby="overview-heading"><h2 id="overview-heading" className="text-xl font-semibold">Przegląd aplikacji</h2><ResourceStatus loading={overview.loading} error={overview.error} onRetry={overview.reload} label="Wczytywanie podsumowania…" />{!overview.loading && !overview.error && overview.data && <>
      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-3">{([
        ['Użytkownicy', overview.data.users], ['Aktywne konta', overview.data.activeUsers], ['Trenerzy', overview.data.trainers], ['Administratorzy', overview.data.admins], ['Plany', overview.data.plans], ['Zapisane serie', overview.data.results], ['Grupy', overview.data.groups],
      ] as [string, number][]).map(([label, value]) => <div key={label} className="rounded-xl border bg-card p-4 shadow-sm"><dt className="text-xs text-muted-foreground">{label}</dt><dd className="mt-2 text-2xl font-semibold tabular-nums">{new Intl.NumberFormat('pl-PL').format(value)}</dd></div>)}</dl>
      <p className="text-xs leading-relaxed text-muted-foreground">Aktywne konta mają potwierdzony e-mail i nie są zablokowane. Liczby trenerów i administratorów obejmują wszystkie konta z daną rolą. Podsumowanie pokazuje wyłącznie liczby, bez prywatnych planów i wyników.</p>
    </>}</section>

    <section className="space-y-4" aria-labelledby="admin-users-heading">
      <div className="flex items-center gap-2"><UsersRound size={22} className="text-primary" aria-hidden="true" /><h2 id="admin-users-heading" className="text-xl font-semibold">Użytkownicy</h2></div>
      <AdminFilterForm key={params.toString()} filters={filters} onApply={(next) => { setNotice(''); setParams(adminSearchParams(next, 0)) }} onClear={() => { setNotice(''); setParams({}) }} />
      {notice && <Notice>{notice}</Notice>}
      <ResourceStatus loading={users.loading} error={users.error} onRetry={users.reload} label="Wczytywanie użytkowników…" />
      {!users.loading && !users.error && users.data && <>
        <p className="text-sm text-muted-foreground" role="status">Liczba użytkowników: <strong>{users.data.totalElements}</strong></p>
        {users.data.content.length ? <ul className="space-y-4">{users.data.content.map((user) => <li key={`${user.id}-${user.role}-${user.blocked}-${user.enabled}`}><UserCard user={user} actorId={actor.id} disabled={busy || pending !== null} onAction={propose} /></li>)}</ul> : <Card><CardContent className="space-y-3 py-8 text-center"><Search className="mx-auto text-muted-foreground" aria-hidden="true" /><h3 className="font-semibold">Brak użytkowników na tej stronie</h3><p className="text-sm text-muted-foreground">Zmień filtry lub wróć na początek listy.</p>{filters.page > 0 && <Button variant="outline" onClick={() => setParams(adminSearchParams(filters, 0))}>Wróć na pierwszą stronę</Button>}</CardContent></Card>}
        {(users.data.totalPages > 1 || filters.page > 0) && <nav aria-label="Strony użytkowników" className="flex flex-wrap items-center justify-between gap-3 rounded-xl border bg-card p-4"><Button variant="outline" disabled={filters.page === 0 || busy} onClick={() => setParams(adminSearchParams(filters, filters.page - 1))}><ArrowLeft size={16} aria-hidden="true" />Poprzednia</Button><span className="text-xs text-muted-foreground">Strona {filters.page + 1} · liczba stron: {Math.max(1, users.data.totalPages)}</span><Button variant="outline" disabled={filters.page + 1 >= users.data.totalPages || busy} onClick={() => setParams(adminSearchParams(filters, filters.page + 1))}>Następna<ArrowRight size={16} aria-hidden="true" /></Button></nav>}
      </>}
    </section>
    {pending && <ActionConfirmation action={pending} busy={busy} error={error} onCancel={() => { if (!busy) { setPending(null); setError('') } }} onSubmit={confirm} />}
  </div>
}

function AdminFilterForm({ filters, onApply, onClear }: { filters: AdminFilters; onApply: (filters: AdminFilters) => void; onClear: () => void }) {
  const [q, setQ] = useState(filters.q)
  const [role, setRole] = useState(filters.role)
  const [status, setStatus] = useState(filters.status)
  function submit(event: FormEvent) { event.preventDefault(); onApply({ q, role, status, page: 0 }) }
  return <Card><CardContent className="pt-6"><form onSubmit={submit} className="space-y-4"><div className="space-y-2"><Label htmlFor="admin-search">Nazwa lub e-mail użytkownika</Label><Input id="admin-search" maxLength={255} autoComplete="off" value={q} onChange={(event) => setQ(event.target.value)} placeholder="Szukaj konta…" /></div><div className="grid gap-4 sm:grid-cols-2"><div className="space-y-2"><Label htmlFor="admin-filter-role">Rola</Label><select id="admin-filter-role" className={selectClass} value={role} onChange={(event) => setRole(event.target.value as AdminFilters['role'])}><option value="">Wszystkie role</option>{roleOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div><div className="space-y-2"><Label htmlFor="admin-filter-status">Status konta</Label><select id="admin-filter-status" className={selectClass} value={status} onChange={(event) => setStatus(event.target.value as AdminFilters['status'])}><option value="">Wszystkie statusy</option>{Object.entries(accountStatusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div></div><div className="flex flex-wrap gap-2"><Button type="submit"><Search size={16} aria-hidden="true" />Szukaj użytkowników</Button><Button variant="outline" type="button" onClick={onClear}>Wyczyść filtry</Button></div></form></CardContent></Card>
}

function UserCard({ user, actorId, disabled, onAction }: { user: AdminUser; actorId: number; disabled: boolean; onAction: (action: PendingAction) => void }) {
  const [role, setRole] = useState<UserRole>(user.role)
  const self = user.id === actorId
  const status = accountStatus(user)
  return <article aria-label={`Konto: ${user.email}`} className="rounded-xl border bg-card p-5 shadow-sm">
    <div className="flex flex-wrap items-start justify-between gap-3"><div className="min-w-0"><h3 className="break-words text-lg font-semibold">{user.displayName}{self && <span className="ml-2 inline-block text-xs font-medium text-primary">Twoje konto</span>}</h3><p className="mt-1 break-all text-sm text-muted-foreground">{user.email}</p></div><span className={`rounded-full px-3 py-1 text-xs font-medium ${status === 'BLOCKED' ? 'bg-destructive/10 text-destructive' : status === 'ACTIVE' ? 'bg-primary/10 text-primary' : 'bg-muted text-muted-foreground'}`}>{accountStatusLabels[status]}</span></div>
    <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-3"><div><dt className="text-xs text-muted-foreground">Obecna rola</dt><dd className="mt-1 font-medium">{adminRoleLabels[user.role]}</dd></div><div><dt className="text-xs text-muted-foreground">E-mail potwierdzony</dt><dd className="mt-1">{user.enabled ? 'Tak' : 'Nie'}</dd></div><div><dt className="text-xs text-muted-foreground">Konto utworzone</dt><dd className="mt-1">{new Intl.DateTimeFormat('pl-PL', { dateStyle: 'medium' }).format(new Date(user.createdAt))}</dd></div></dl>
    <div className="mt-5 space-y-4 border-t pt-4"><div className="grid gap-3 sm:grid-cols-[1fr_auto] sm:items-end"><div className="min-w-0 space-y-2"><Label htmlFor={`admin-role-${user.id}`}>Nowa rola</Label><select id={`admin-role-${user.id}`} aria-label={`Nowa rola: ${user.email}`} className={selectClass} disabled={disabled || self || !user.enabled} value={role} onChange={(event) => setRole(event.target.value as UserRole)}>{roleOptions.map(([value, label]) => <option key={value} value={value} disabled={value === 'ADMIN' && user.blocked}>{label}</option>)}</select></div><Button variant="outline" disabled={disabled || !canChangeRole(user, actorId, role)} aria-label={`Zmień rolę: ${user.email}`} onClick={() => onAction({ user, kind: 'role', role })}>Zmień rolę</Button></div>
      <div className="flex flex-wrap items-center justify-between gap-3"><p className="max-w-sm text-xs leading-relaxed text-muted-foreground">{self ? 'Własnej roli i blokady nie zmienisz z tego panelu.' : !user.enabled ? 'Zmiana roli wymaga potwierdzenia e-maila. Odblokowanie nie aktywuje niepotwierdzonego konta.' : 'Zmiana roli lub blokady unieważnia aktywne sesje tego konta.'}</p><Button variant="outline" className={user.blocked ? '' : 'border-destructive/30 text-destructive hover:bg-destructive/5 hover:text-destructive'} disabled={disabled || self} aria-label={`${user.blocked ? 'Odblokuj' : 'Zablokuj'} konto: ${user.email}`} onClick={() => onAction({ user, kind: 'block', blocked: !user.blocked })}>{user.blocked ? 'Odblokuj konto' : 'Zablokuj konto'}</Button></div>
    </div>
  </article>
}

function ActionConfirmation({ action, busy, error, onCancel, onSubmit }: { action: PendingAction; busy: boolean; error: string; onCancel: () => void; onSubmit: (event: FormEvent) => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const cancel = useRef<HTMLButtonElement>(null)
  const title = action.kind === 'role' ? 'Zmiana roli konta' : action.blocked ? 'Blokada konta' : 'Odblokowanie konta'
  useEffect(() => {
    const element = dialog.current
    element?.showModal()
    cancel.current?.focus()
    return () => { if (element?.open) element.close() }
  }, [])
  return <dialog ref={dialog} aria-labelledby="admin-confirm-heading" aria-describedby="admin-confirm-description" onCancel={(event) => { event.preventDefault(); if (!busy) onCancel() }} className="m-auto max-h-[90svh] w-[calc(100%-2rem)] max-w-lg overflow-auto rounded-xl border bg-card p-6 text-foreground shadow-xl backdrop:bg-black/45">
    <form onSubmit={onSubmit} className="space-y-5"><div className="space-y-3"><ShieldCheck className="text-primary" size={25} aria-hidden="true" /><h2 id="admin-confirm-heading" className="text-xl font-semibold">{title}</h2><div className="min-w-0 rounded-lg bg-muted p-3"><p className="break-words font-medium">{action.user.displayName}</p><p className="mt-1 break-all text-sm text-muted-foreground">{action.user.email}</p></div><p id="admin-confirm-description" className="text-sm leading-relaxed">{action.kind === 'role' ? <>Zmienisz rolę z <strong>{adminRoleLabels[action.user.role]}</strong> na <strong>{adminRoleLabels[action.role]}</strong>. {action.role === 'ADMIN' ? 'To konto otrzyma dostęp do zarządzania użytkownikami i uprawnieniami.' : action.user.role === 'ADMIN' ? 'To konto utraci dostęp do panelu administratora.' : action.role === 'TRAINER' ? 'To konto otrzyma dostęp do grup i przypisywania planów.' : 'To konto utraci uprawnienia trenera.'} Użytkownik będzie musiał zalogować się ponownie.</> : action.blocked ? 'Zablokujesz logowanie i dostęp z aktywnych sesji tego konta. Jego plany, treningi i pomiary pozostaną zachowane.' : <>Przywrócisz możliwość korzystania z konta. {action.user.enabled ? 'Użytkownik będzie mógł ponownie się zalogować.' : 'Adres e-mail nadal będzie wymagał potwierdzenia.'}</>}</p></div>{error && <Notice variant="error">{error}</Notice>}<div className="flex flex-wrap justify-end gap-2"><Button ref={cancel} variant="outline" type="button" disabled={busy} onClick={onCancel}>Anuluj</Button><Button type="submit" disabled={busy}>{busy ? 'Zapisywanie…' : 'Potwierdź zmianę'}</Button></div></form>
  </dialog>
}
