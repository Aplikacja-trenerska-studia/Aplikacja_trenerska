import type { ReactNode } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

export function AuthCard({ icon, title, description, children }: { icon: ReactNode; title: string; description: string; children: ReactNode }) {
  return <Card>
    <CardHeader className="space-y-3 pb-5">
      <div className="mb-1 flex size-12 items-center justify-center rounded-2xl bg-primary/8 text-primary" aria-hidden="true">{icon}</div>
      <CardTitle>{title}</CardTitle>
      <CardDescription className="leading-relaxed">{description}</CardDescription>
    </CardHeader>
    <CardContent>{children}</CardContent>
  </Card>
}
