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
      <p className="text-sm leading-relaxed text-muted-foreground">{loggedIn ? 'Przeglądaj bazę ćwiczeń i sprawdzaj swoje plany treningowe. Wszystkie dni i ćwiczenia masz w jednym miejscu.' : 'Jedno miejsce dla Twoich treningów. Poznaj ćwiczenia na siłownię i do domu, a swoje plany miej zawsze pod ręką.'}</p>
      <Button asChild className="w-full"><Link to={loggedIn ? '/plany' : '/rejestracja'}>{loggedIn ? 'Przejdź do moich planów' : 'Utwórz konto'}<ArrowRight size={16} aria-hidden="true" /></Link></Button>
      {loggedIn && <Button asChild variant="outline" className="w-full"><Link to="/cwiczenia">Przejrzyj ćwiczenia<Dumbbell size={16} aria-hidden="true" /></Link></Button>}
      {!loggedIn && <p className="text-center text-sm text-muted-foreground">Masz już konto? <Link className="text-primary underline-offset-4 hover:underline" to="/logowanie">Zaloguj się</Link></p>}
    </CardContent>
  </Card>
}
