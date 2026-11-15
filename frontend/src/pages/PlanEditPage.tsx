import { Link, useParams } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { PlanEditor } from '@/components/workout/PlanEditor'
import { ResourceStatus } from '@/components/ui/resource-status'
import { useApiResource } from '@/hooks/use-api-resource'
import type { Exercise, WorkoutPlan } from '@/types/workout'

export function PlanEditPage() {
  const { id } = useParams()
  const catalog = useApiResource<Exercise[]>('/api/exercises')
  const plan = useApiResource<WorkoutPlan>(`/api/plans/${encodeURIComponent(id || '')}`)
  const loading = plan.loading || catalog.loading
  const error = plan.error || catalog.error
  return <div className="space-y-5"><Link to="/plany" className="inline-flex items-center gap-2 text-sm font-medium text-primary"><ArrowLeft size={16} aria-hidden="true" />Moje plany</Link><ResourceStatus loading={loading} error={error} onRetry={() => { plan.reload(); catalog.reload() }} label="Wczytywanie planu do edycji…" />{!loading && !error && plan.data && catalog.data && <PlanEditor key={plan.data.id} plan={plan.data} catalog={catalog.data} />}</div>
}
