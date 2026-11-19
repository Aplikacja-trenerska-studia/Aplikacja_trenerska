import { Link, Outlet, useOutletContext } from 'react-router-dom'
import type { UserResponse } from '@/types/auth'
import { Button } from '@/components/ui/button'

export function TrainerRoute() {
  const user = useOutletContext<UserResponse>()
  if (user.role !== 'TRAINER') return <div className="space-y-4 rounded-xl border bg-card p-6"><h1 className="text-xl font-semibold">Panel jest dostępny dla trenerów</h1><p className="text-sm text-muted-foreground">Rolę trenera nadaje administrator projektu.</p><Button asChild variant="outline"><Link to="/plany">Moje plany</Link></Button></div>
  return <Outlet context={user} />
}
