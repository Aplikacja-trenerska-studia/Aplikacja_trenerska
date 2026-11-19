export type ExerciseResult = {
  id: string
  time: string
  exerciseId: number
  exerciseName: string
  setNo: number
  reps: number
  weight: number
  planName: string
  dayName: string
}

export const metricLabels = {
  weightKg: 'Waga', chestCm: 'Klatka piersiowa', waistCm: 'Talia',
  hipsCm: 'Biodra', armCm: 'Ramię', thighCm: 'Udo',
} as const
export type MetricKey = keyof typeof metricLabels
export type BodyMetric = { id: string; time: string } & Record<MetricKey, number | null>
export type ChartPoint = { time: number; value: number }
