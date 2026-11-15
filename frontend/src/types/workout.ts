export type MuscleGroup = 'CHEST' | 'BACK' | 'SHOULDERS' | 'BICEPS' | 'TRICEPS' | 'FOREARMS' | 'QUADRICEPS' | 'HAMSTRINGS' | 'GLUTES' | 'CALVES' | 'CORE' | 'FULL_BODY'
export type ExerciseType = 'STRENGTH' | 'HOME'

export type Exercise = {
  id: number
  name: string
  muscleGroup: MuscleGroup
  type: ExerciseType
  description: string | null
}

export type PlanSummary = {
  id: number
  name: string
  description: string | null
  createdAt: string
}

export type WorkoutPlanRequest = {
  name: string
  description: string | null
  days: { name: string; exercises: { exerciseId: number; sets: number; reps: number; weight: number }[] }[]
}

export type WorkoutPlan = PlanSummary & {
  days: {
    id: number
    name: string
    dayIndex: number
    exercises: {
      id: number
      orderIndex: number
      exercise: Exercise
      sets: number
      reps: number
      weight: number
    }[]
  }[]
}

export const muscleGroupLabels: Record<MuscleGroup, string> = {
  CHEST: 'Klatka piersiowa', BACK: 'Plecy', SHOULDERS: 'Barki', BICEPS: 'Biceps',
  TRICEPS: 'Triceps', FOREARMS: 'Przedramiona', QUADRICEPS: 'Przód uda',
  HAMSTRINGS: 'Tył uda', GLUTES: 'Pośladki', CALVES: 'Łydki', CORE: 'Brzuch i tułów', FULL_BODY: 'Całe ciało',
}

export const exerciseTypeLabels: Record<ExerciseType, string> = { STRENGTH: 'Siłowe', HOME: 'Domowe' }
