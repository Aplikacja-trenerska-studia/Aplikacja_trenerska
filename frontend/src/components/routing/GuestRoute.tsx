import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthSession } from '@/hooks/use-auth-session'
import { loginReturnPath } from '@/lib/return-path'

export function GuestRoute() {
  const { loggedIn } = useAuthSession()
  const { state } = useLocation()
  return loggedIn ? <Navigate to={loginReturnPath(state?.from)} replace /> : <Outlet />
}
