import { RefreshCw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Notice } from '@/components/ui/notice'
import type { ApiError } from '@/types/auth'

export function ResourceStatus({ loading, error, onRetry, label }: {
  loading: boolean; error: ApiError | null; onRetry: () => void; label: string
}) {
  if (loading) return <div role="status" className="rounded-xl border bg-card p-8 text-center text-sm text-muted-foreground">{label}</div>
  if (error) return <div className="space-y-3">
    <Notice variant="error">{error.message}</Notice>
    {error.status !== 404 && <Button variant="outline" onClick={onRetry}><RefreshCw size={16} aria-hidden="true" />Spróbuj ponownie</Button>}
  </div>
  return null
}
