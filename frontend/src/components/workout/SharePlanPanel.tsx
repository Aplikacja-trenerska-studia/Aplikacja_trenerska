import { useState } from 'react'
import { Copy, Link2, Share2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Notice } from '@/components/ui/notice'
import { apiFetch } from '@/lib/api'
import { shareUrl } from '@/lib/sharing'
import type { ApiError } from '@/types/auth'
import type { PlanShare } from '@/types/workout'

export function SharePlanPanel({ planId, initial }: { planId: number; initial: PlanShare }) {
  const [share, setShare] = useState<PlanShare>(initial)
  const [busy, setBusy] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const link = share.shared && share.shareToken ? shareUrl(share.shareToken, window.location.origin) : ''

  async function enable() {
    if (busy) return
    setBusy(true); setError(''); setNotice('')
    try {
      const result = await apiFetch<PlanShare>(`/api/plans/${planId}/share`, { method: 'POST' })
      setShare(result)
      setNotice('Link jest gotowy. Każdy zalogowany użytkownik, który go otrzyma, zobaczy plan i może skopiować go do siebie.')
    } catch (failure) { setError((failure as ApiError).message || 'Nie udało się udostępnić planu.') }
    finally { setBusy(false) }
  }

  async function disable() {
    if (busy) return
    setBusy(true); setError(''); setNotice('')
    try {
      await apiFetch<void>(`/api/plans/${planId}/share`, { method: 'DELETE' })
      setShare({ shared: false, shareToken: null })
      setConfirming(false)
      setNotice('Udostępnianie wyłączone. Dotychczasowy link przestał działać. Kopie zrobione wcześniej przez innych pozostają u nich.')
    } catch (failure) { setError((failure as ApiError).message || 'Nie udało się wyłączyć udostępniania.') }
    finally { setBusy(false) }
  }

  async function copy() {
    setError('')
    try {
      await navigator.clipboard.writeText(link)
      setNotice('Skopiowano link do schowka.')
    } catch {
      const field = document.getElementById(`share-link-${planId}`) as HTMLInputElement | null
      field?.select()
      setNotice('Zaznaczono link. Skopiuj go skrótem Ctrl+C lub z menu telefonu.')
    }
  }

  return <Card>
    <CardHeader>
      <CardTitle className="flex items-center gap-2 text-lg"><Share2 size={18} className="text-primary" aria-hidden="true" />Udostępnianie planu</CardTitle>
      <p className="text-sm leading-relaxed text-muted-foreground">Wyślij link znajomemu. Po zalogowaniu zobaczy plan i może zapisać jego niezależną kopię na swoim koncie. Twoje dalsze zmiany nie zmienią już zapisanych kopii.</p>
    </CardHeader>
    <CardContent className="space-y-4">
      {error && <Notice variant="error">{error}</Notice>}
      {notice && <Notice>{notice}</Notice>}
      {!share.shared || !link ? <Button type="button" variant="outline" disabled={busy} onClick={enable}><Link2 size={16} aria-hidden="true" />{busy ? 'Tworzenie linku…' : 'Udostępnij link'}</Button> : <>
        <div className="space-y-2">
          <Label htmlFor={`share-link-${planId}`}>Link do planu</Label>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Input id={`share-link-${planId}`} readOnly value={link} onFocus={(event) => event.currentTarget.select()} className="min-w-0 font-mono text-xs" />
            <Button type="button" onClick={copy}><Copy size={16} aria-hidden="true" />Kopiuj link</Button>
          </div>
        </div>
        {confirming ? <div className="space-y-3 rounded-lg border p-4">
          <p className="text-sm">Wyłączyć udostępnianie? Obecny link przestanie działać. Ponowne udostępnienie utworzy nowy link.</p>
          <div className="flex flex-wrap gap-2"><Button type="button" disabled={busy} className="bg-destructive hover:bg-destructive/90" onClick={disable}>{busy ? 'Wyłączanie…' : 'Wyłącz udostępnianie'}</Button><Button type="button" variant="ghost" disabled={busy} onClick={() => setConfirming(false)}>Anuluj</Button></div>
        </div> : <Button type="button" variant="ghost" className="text-destructive" onClick={() => { setConfirming(true); setNotice('') }}>Wyłącz udostępnianie</Button>}
      </>}
    </CardContent>
  </Card>
}
