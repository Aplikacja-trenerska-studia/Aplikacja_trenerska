import { PlanEditor } from '@/components/workout/PlanEditor'
import { ResourceStatus } from '@/components/ui/resource-status'
import { useApiResource } from '@/hooks/use-api-resource'
import type { Exercise } from '@/types/workout'

export function PlanCreatePage() {
  const { data, loading, error, reload } = useApiResource<Exercise[]>('/api/exercises')
  return <><ResourceStatus loading={loading} error={error} onRetry={reload} label="Wczytywanie kreatora…" />{!loading && !error && data && <PlanEditor catalog={data} />}</>
}
