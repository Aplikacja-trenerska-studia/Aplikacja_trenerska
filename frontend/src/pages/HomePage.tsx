import { Link } from 'react-router-dom'
import { ArrowRight, Dumbbell } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { useAuthSession } from '@/hooks/use-auth-session'

export function HomePage() {
  const { loggedIn, user } = useAuthSession()
  return <Card className="overflow-hidden">
    <div className="flex h-40 items-center justify-center bg-primary/5"><Dumbbell size={64} strokeWidth={1.3} className="text-primary" aria-hidden="true" /></div>
    <CardContent className="space-y-5 pt-7">
      <p className="text-xs font-semibold uppercase tracking-widest text-primary">Aplikacja trenerska</p>
      <h1 className="text-3xl font-semibold leading-tight tracking-tight">{loggedIn ? `Witaj, ${user?.displayName || 'sportowcu'}!` : 'Każdy trening to krok do przodu.'}</h1>
      <p className="text-sm leading-relaxed text-muted-foreground">Jedno miejsce dla Twoich treningów i postępów. Już teraz możesz zarządzać kontem; plany treningowe i śledzenie progresu pojawią się w kolejnych etapach.</p>
      <Button asChild className="w-full"><Link to={loggedIn ? '/profil' : '/rejestracja'}>{loggedIn ? 'Przejdź do profilu' : 'Utwórz konto'}<ArrowRight size={16} aria-hidden="true" /></Link></Button>
      {!loggedIn && <p className="text-center text-sm text-muted-foreground">Masz już konto? <Link className="text-primary underline-offset-4 hover:underline" to="/logowanie">Zaloguj się</Link></p>}
    </CardContent>
  </Card>
}
