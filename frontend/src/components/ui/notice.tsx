import { CheckCircle2, CircleAlert, Mail } from 'lucide-react'
import type { ReactNode } from 'react'

export function Notice({ children, variant = 'success' }: { children: ReactNode; variant?: 'success' | 'error' | 'mail' }) {
  const Icon = variant === 'error' ? CircleAlert : variant === 'mail' ? Mail : CheckCircle2
  return <div role={variant === 'error' ? 'alert' : 'status'} className={`flex items-start gap-3 rounded-lg border p-4 text-sm leading-relaxed ${variant === 'error' ? 'border-destructive/25 bg-destructive/5 text-destructive' : 'border-primary/15 bg-primary/5 text-foreground'}`}>
    <Icon size={19} className="mt-0.5 shrink-0" aria-hidden="true" /><div>{children}</div>
  </div>
}
