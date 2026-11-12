import { Link, Outlet, useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { useAuthSession } from '@/hooks/use-auth-session'
import { clearSession } from '@/lib/auth-storage'

export function AppLayout() {
  const navigate = useNavigate()
  const { loggedIn, user } = useAuthSession()

  return (
    <div className="mx-auto flex min-h-svh w-full max-w-lg flex-col px-4 pb-8 pt-4">
      <header className="mb-6 flex items-center justify-between gap-3">
        <Link to="/" className="text-lg font-semibold tracking-tight">
          Aplikacja trenerska
        </Link>
        {loggedIn ? (
          <div className="flex items-center gap-2">
            {user ? (
              <span className="hidden max-w-[9rem] truncate text-sm text-muted-foreground sm:inline">
                {user.displayName}
              </span>
            ) : null}
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => {
                clearSession()
                navigate('/logowanie', { replace: true })
              }}
            >
              Wyloguj
            </Button>
          </div>
        ) : (
          <div className="flex gap-2">
            <Button asChild variant="ghost" size="sm">
              <Link to="/logowanie">Logowanie</Link>
            </Button>
            <Button asChild size="sm">
              <Link to="/rejestracja">Rejestracja</Link>
            </Button>
          </div>
        )}
      </header>
      <main className="flex flex-1 flex-col">
        <Outlet />
      </main>
    </div>
  )
}
