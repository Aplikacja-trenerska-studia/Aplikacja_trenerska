import { Navigate, Outlet } from 'react-router-dom'
import { useAuthSession } from '@/hooks/use-auth-session'

export function GuestRoute() {
  const { loggedIn } = useAuthSession()
  return loggedIn ? <Navigate to="/profil" replace /> : <Outlet />
}
