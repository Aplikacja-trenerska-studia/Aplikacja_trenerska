import { Link, Outlet, useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { useAuthSession } from '@/hooks/use-auth-session'
import { clearSession } from '@/lib/auth-storage'
import { Dumbbell, UserRound } from 'lucide-react'

export function AppLayout() {
  const navigate = useNavigate()
  const { loggedIn, user } = useAuthSession()

  return (
    <div className="mx-auto flex min-h-svh w-full max-w-3xl flex-col px-4 pb-6 pt-5 sm:px-6 sm:pt-8">
      <header className="mb-8 flex flex-wrap items-center justify-between gap-3 sm:mb-12">
        <Link to="/" className="flex items-center gap-2 text-sm font-semibold tracking-tight sm:text-base">
          <span className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground"><Dumbbell size={20} aria-hidden="true" /></span>
          Aplikacja trenerska
        </Link>
        {loggedIn ? (
          <div className="flex items-center gap-2">
            <Button asChild variant="ghost" size="sm"><Link to="/profil"><UserRound size={16} aria-hidden="true" /><span className="max-w-28 truncate">{user?.displayName || 'Mój profil'}</span></Link></Button>
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
      <main id="main-content" className="mx-auto flex w-full max-w-lg flex-1 flex-col">
        <Outlet />
      </main>
      <footer className="mt-10 border-t pt-5 text-center text-xs text-muted-foreground">Twój trening. Twój progres.</footer>
    </div>
  )
}
