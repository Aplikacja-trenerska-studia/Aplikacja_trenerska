import { Navigate, Outlet } from 'react-router-dom'

import { useAuthSession } from '@/hooks/use-auth-session'

export function ProtectedRoute() {
  const { loggedIn } = useAuthSession()

  if (!loggedIn) {
    return <Navigate to="/logowanie" replace />
  }
  return <Outlet />
}
