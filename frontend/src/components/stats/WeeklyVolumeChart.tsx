import { chartScale, formatVolume, weekLabel } from '@/lib/stats'
import type { WeekStats } from '@/types/stats'

export function WeeklyVolumeChart({ weeks }: { weeks: WeekStats[] }) {
  if (!weeks.some((week) => week.volume > 0)) return <div className="rounded-lg border border-dashed p-7 text-center text-sm text-muted-foreground">Brak objętości do pokazania w wybranym okresie. Zapisz trening z obciążeniem, aby zobaczyć wykres.</div>
  const top = chartScale(weeks.map((week) => week.volume))
  const labelEvery = Math.ceil(weeks.length / 6)
  const best = weeks.reduce((winner, week) => week.volume > winner.volume ? week : winner, weeks[0])
  return <figure className="space-y-3">
    <figcaption className="text-sm font-medium">Objętość tygodniowa <span className="font-normal text-muted-foreground">(kg, suma powtórzeń × ciężar)</span></figcaption>
    <div role="img" aria-label={`Wykres objętości dla ${weeks.length} tygodni. Najwyższa objętość: ${formatVolume(best.volume)} w tygodniu od ${weekLabel(best.weekStart)}. Dokładne wartości są w tabeli pod wykresem.`} className="rounded-lg bg-muted/40 p-3">
      <div className="relative flex h-44 items-end gap-[2px] border-b border-l border-border pl-1 sm:gap-1" aria-hidden="true">
        <span className="absolute -top-1 left-1 text-[11px] text-muted-foreground">{formatVolume(top)}</span>
        <span className="absolute inset-x-0 top-1/2 border-t border-dashed border-border" />
        {weeks.map((week) => <div key={week.weekStart} className="flex h-full min-w-0 flex-1 items-end" title={`${weekLabel(week.weekStart)}: ${formatVolume(week.volume)}, treningi: ${week.sessions}`}>
          <div className={`w-full rounded-t-sm ${week.volume > 0 ? 'bg-primary' : ''}`} style={{ height: `${Math.max(week.volume > 0 ? 2 : 0, week.volume / top * 100)}%` }} />
        </div>)}
      </div>
      <div className="mt-1 flex gap-[2px] pl-1 sm:gap-1" aria-hidden="true">{weeks.map((week, index) => <span key={week.weekStart} className="min-w-0 flex-1 overflow-visible whitespace-nowrap text-[11px] text-muted-foreground">{index % labelEvery === 0 ? weekLabel(week.weekStart) : ''}</span>)}</div>
    </div>
    <details className="text-xs text-muted-foreground"><summary className="cursor-pointer py-1">Pokaż dane wykresu</summary><div className="max-h-60 overflow-auto"><table className="mt-2 w-full text-left"><caption className="sr-only">Objętość tygodniowa — wartości</caption><thead><tr><th scope="col" className="py-2">Tydzień od</th><th scope="col">Treningi</th><th scope="col">Serie</th><th scope="col">Objętość</th></tr></thead><tbody>{[...weeks].reverse().map((week) => <tr key={week.weekStart} className="border-t"><td className="py-2">{weekLabel(week.weekStart)}</td><td>{week.sessions}</td><td>{week.sets}</td><td>{formatVolume(week.volume)}</td></tr>)}</tbody></table></div></details>
  </figure>
}
