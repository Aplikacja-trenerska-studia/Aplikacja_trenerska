export type Sex = 'MALE' | 'FEMALE'
export type ActivityLevel = 'SEDENTARY' | 'LIGHT' | 'MODERATE' | 'ACTIVE' | 'VERY_ACTIVE'
export type Goal = 'LOSE' | 'MAINTAIN' | 'GAIN'

export type OneRepMaxResult = {
  weight: number
  reps: number
  epley: number
  brzycki: number
  lombardi: number
  percentages: { percent: number; weight: number; reps: number }[]
}

export type CalorieResult = {
  bmr: number
  tdee: number
  targetCalories: number
  calorieChange: number
  activityFactor: number
  macros: { proteinG: number; fatG: number; carbsG: number; proteinKcal: number; fatKcal: number; carbsKcal: number }
}

export const sexLabels: Record<Sex, string> = { MALE: 'Mężczyzna', FEMALE: 'Kobieta' }

export const activityLabels: Record<ActivityLevel, { label: string; hint: string }> = {
  SEDENTARY: { label: 'Bardzo niska', hint: 'praca siedząca, brak treningów' },
  LIGHT: { label: 'Niska', hint: '1–3 lekkie treningi w tygodniu' },
  MODERATE: { label: 'Umiarkowana', hint: '3–5 treningów w tygodniu' },
  ACTIVE: { label: 'Wysoka', hint: '6–7 treningów w tygodniu' },
  VERY_ACTIVE: { label: 'Bardzo wysoka', hint: 'praca fizyczna i codzienne treningi' },
}

export const goalLabels: Record<Goal, string> = { LOSE: 'Redukcja (−15%)', MAINTAIN: 'Utrzymanie wagi', GAIN: 'Budowa masy (+10%)' }
