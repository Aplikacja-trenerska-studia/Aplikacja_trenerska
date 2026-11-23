import { decimal } from './progress.ts'
import type { ActivityLevel, Goal, Sex } from '../types/calc'

export type QueryResult = { query: string | null; error: string | null }
export type CalorieForm = { sex: Sex; age: string; heightCm: string; weightKg: string; activity: ActivityLevel; goal: Goal }

function integer(value: string, minimum: number, maximum: number): number | null {
  if (!/^\d{1,3}$/.test(value.trim())) return null
  const parsed = Number(value.trim())
  return parsed >= minimum && parsed <= maximum ? parsed : null
}

export function oneRepMaxQuery(weight: string, reps: string): QueryResult {
  const lifted = decimal(weight, 0, 2000)
  if (lifted === null || lifted <= 0) return { query: null, error: 'Wpisz ciężar większy od 0 i nie większy niż 2000 kg, maksymalnie z dwoma miejscami po przecinku.' }
  const repetitions = integer(reps, 1, 20)
  if (repetitions === null) return { query: null, error: 'Wpisz liczbę powtórzeń od 1 do 20. Przy większej liczbie wynik byłby mało wiarygodny.' }
  return { query: new URLSearchParams({ weight: String(lifted), reps: String(repetitions) }).toString(), error: null }
}

export function caloriesQuery(form: CalorieForm): QueryResult {
  const age = integer(form.age, 15, 100)
  if (age === null) return { query: null, error: 'Wpisz wiek od 15 do 100 lat.' }
  const height = integer(form.heightCm, 100, 250)
  if (height === null) return { query: null, error: 'Wpisz wzrost od 100 do 250 cm.' }
  const weight = decimal(form.weightKg, 30, 300)
  if (weight === null) return { query: null, error: 'Wpisz masę ciała od 30 do 300 kg, maksymalnie z dwoma miejscami po przecinku.' }
  return {
    query: new URLSearchParams({ sex: form.sex, age: String(age), heightCm: String(height), weightKg: String(weight), activity: form.activity, goal: form.goal }).toString(),
    error: null,
  }
}

export function macroShares(macros: { proteinKcal: number; fatKcal: number; carbsKcal: number }) {
  const total = macros.proteinKcal + macros.fatKcal + macros.carbsKcal
  if (total <= 0) return { protein: 0, fat: 0, carbs: 0 }
  const protein = Math.round(macros.proteinKcal / total * 100)
  const fat = Math.round(macros.fatKcal / total * 100)
  return { protein, fat, carbs: Math.max(0, 100 - protein - fat) }
}
