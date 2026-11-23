export type ScheduledWorkout = {
  id: number
  date: string
  planId: number | null
  planDayId: number | null
  planName: string
  dayName: string
  note: string | null
  completed: boolean
  completedAt: string | null
}

export type Agenda = {
  date: string
  today: ScheduledWorkout[]
  overdue: ScheduledWorkout[]
  upcoming: ScheduledWorkout[]
}

export type ScheduleRequest = { date: string; planId: number; planDayId: number; note: string | null }
