import { Notice } from '@/components/ui/notice'
import { isMailDemo } from '@/lib/mail-mode'

export function MailDemoNotice() {
  if (!isMailDemo) return null
  return <Notice variant="mail">Tryb demonstracyjny: e-maile nie są wysyłane. O link do aktywacji konta, resetu hasła lub potwierdzenia usunięcia poproś prowadzącego prezentację.</Notice>
}
