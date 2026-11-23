export type WeekStats = { weekStart: string; sessions: number; sets: number; reps: number; volume: number }

export type ExerciseRecord = {
  exerciseId: number
  exerciseName: string
  maxWeight: number | null
  maxWeightReps: number | null
  maxWeightAt: string | null
  estimatedOneRepMax: number | null
  estimatedOneRepMaxAt: string | null
  maxReps: number
  maxRepsAt: string | null
  totalSets: number
  lastPerformedAt: string | null
}

export type StatsSummary = {
  from: string
  to: string
  zone: string
  summary: { sessions: number; sets: number; reps: number; volume: number; activeWeeks: number; averageSessionsPerWeek: number }
  weeks: WeekStats[]
  records: ExerciseRecord[]
}

export type SessionSummary = {
  time: string
  planName: string
  dayName: string
  exercises: number
  sets: number
  reps: number
  volume: number
}
