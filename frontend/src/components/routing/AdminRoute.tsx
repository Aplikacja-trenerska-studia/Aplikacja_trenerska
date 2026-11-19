import { Link, Outlet, useOutletContext } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import type { UserResponse } from '@/types/auth'

export function AdminRoute() {
  const user = useOutletContext<UserResponse>()
  if (user.role !== 'ADMIN') return <div className="space-y-4 rounded-xl border bg-card p-6"><h1 className="text-xl font-semibold">Panel jest dostępny dla administratorów</h1><p className="text-sm text-muted-foreground">Twoje konto nie ma uprawnień do zarządzania aplikacją.</p><Button asChild variant="outline"><Link to="/plany">Moje plany</Link></Button></div>
  return <Outlet context={user} />
}
