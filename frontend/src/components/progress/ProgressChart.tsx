import { useId, useLayoutEffect, useRef, useState } from 'react'
import type { ChartPoint } from '@/types/progress'
import { numberFormat } from '@/lib/progress'

export function ProgressChart({ title, unit, points }: { title: string; unit: string; points: ChartPoint[] }) {
  const id = useId()
  const figure = useRef<HTMLElement>(null)
  const [width, setWidth] = useState(620)
  const hasPoints = points.length > 0
  useLayoutEffect(() => {
    const element = figure.current
    if (!element) return
    function measure() {
      const measured = element!.getBoundingClientRect().width
      if (measured > 0) setWidth(Math.max(200, Math.round(measured)))
    }
    measure()
    const observer = new ResizeObserver(measure)
    observer.observe(element)
    return () => observer.disconnect()
  }, [hasPoints])
  if (!points.length) return <div className="rounded-lg border border-dashed p-7 text-center text-sm text-muted-foreground">Brak danych do wykresu „{title}”.</div>
  const values = points.map((point) => point.value)
  const min = Math.min(...values), max = Math.max(...values)
  const padding = Math.max((max - min) * .15, max * .03, 1)
  const low = Math.max(0, min - padding), high = max + padding
  const start = points[0].time, end = points[points.length - 1].time
  const left = 60, right = width - 24
  const x = (time: number) => start === end ? (left + right) / 2 : left + (time - start) / (end - start) * (right - left)
  const y = (value: number) => 175 - (value - low) / (high - low) * 145
  const date = (time: number) => new Intl.DateTimeFormat('pl-PL', { day: 'numeric', month: 'short' }).format(time)
  return <figure ref={figure} className="space-y-3">
    <figcaption className="text-sm font-medium">{title} <span className="font-normal text-muted-foreground">({unit})</span></figcaption>
    <svg viewBox={`0 0 ${width} 220`} role="img" aria-labelledby={`${id}-title ${id}-description`} className="h-[220px] w-full rounded-lg bg-muted/40">
      <title id={`${id}-title`}>{title}</title><desc id={`${id}-description`}>Liczba punktów: {points.length}. Dokładne wartości są w tabeli pod wykresem. Oś pionowa od {numberFormat.format(low)} do {numberFormat.format(high)} {unit}.</desc>
      {[0, .5, 1].map((fraction) => { const value = low + (high - low) * fraction; return <g key={fraction}><line x1={left} x2={right} y1={y(value)} y2={y(value)} stroke="currentColor" className="text-border" /><text x={left - 10} y={y(value) + 4} textAnchor="end" fontSize="12" fill="currentColor" className="text-muted-foreground">{numberFormat.format(value)}</text></g> })}
      <polyline points={points.map((point) => `${x(point.time)},${y(point.value)}`).join(' ')} fill="none" stroke="currentColor" strokeWidth="3" className="text-primary" />
      {points.map((point, index) => <circle key={`${point.time}-${index}`} cx={x(point.time)} cy={y(point.value)} r="4" fill="currentColor" className="text-primary"><title>{date(point.time)}: {numberFormat.format(point.value)} {unit}</title></circle>)}
      <text x={left} y="205" fontSize="12" fill="currentColor" className="text-muted-foreground">{date(start)}</text>
      {start !== end && <text x={right} y="205" textAnchor="end" fontSize="12" fill="currentColor" className="text-muted-foreground">{date(end)}</text>}
    </svg>
    <details className="text-xs text-muted-foreground"><summary className="cursor-pointer py-1">Pokaż dane wykresu</summary><div className="max-h-52 overflow-auto"><table className="mt-2 w-full text-left"><caption className="sr-only">{title} — wartości</caption><thead><tr><th scope="col" className="py-2">Data</th><th scope="col">Wartość ({unit})</th></tr></thead><tbody>{points.map((point, index) => <tr key={`${point.time}-${index}`} className="border-t"><td className="py-2">{new Intl.DateTimeFormat('pl-PL', { dateStyle: 'medium' }).format(point.time)}</td><td>{numberFormat.format(point.value)}</td></tr>)}</tbody></table></div></details>
  </figure>
}
