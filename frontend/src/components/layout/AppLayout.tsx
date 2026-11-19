import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { useAuthSession } from '@/hooks/use-auth-session'
import { clearSession } from '@/lib/auth-storage'
import { ChartNoAxesCombined, ClipboardList, Dumbbell, Play, UserRound, UsersRound } from 'lucide-react'

export function AppLayout() {
  const navigate = useNavigate()
  const { loggedIn, user } = useAuthSession()
  const { pathname } = useLocation()
  const trainingPage = ['/cwiczenia', '/plany', '/trening', '/progres', '/wspolpraca', '/trener'].some((path) => pathname === path || pathname.startsWith(`${path}/`))

  return (
    <div className="mx-auto flex min-h-svh w-full max-w-3xl flex-col px-4 pb-6 pt-5 sm:px-6 sm:pt-8">
      <header className={`flex flex-wrap items-center justify-between gap-3 ${loggedIn ? 'mb-5' : 'mb-8 sm:mb-12'}`}>
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
      {loggedIn && <nav aria-label="Trening" className="mb-7 grid grid-cols-2 gap-2 sm:grid-cols-3">{[
        { to: '/cwiczenia', label: 'Ćwiczenia', Icon: Dumbbell },
        { to: '/plany', label: 'Moje plany', Icon: ClipboardList },
        { to: '/trening', label: 'Trening', Icon: Play },
        { to: '/progres', label: 'Progres', Icon: ChartNoAxesCombined },
        { to: '/wspolpraca', label: 'Współpraca', Icon: UsersRound },
        ...(user?.role === 'TRAINER' ? [{ to: '/trener', label: 'Panel trenera', Icon: UsersRound }] : []),
      ].map(({ to, label, Icon }) => <NavLink key={to} to={to} className={({ isActive }) => `flex min-h-11 items-center justify-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition-colors ${isActive ? 'border-primary/20 bg-primary/8 text-primary' : 'bg-card text-muted-foreground hover:bg-accent'}`}><Icon size={17} aria-hidden="true" />{label}</NavLink>)}</nav>}
      <main id="main-content" className={`mx-auto flex w-full flex-1 flex-col ${trainingPage ? 'max-w-3xl' : 'max-w-lg'}`}>
        <Outlet />
      </main>
      <footer className="mt-10 border-t pt-5 text-center text-xs text-muted-foreground">Twój trening. Twój progres.</footer>
    </div>
  )
}
