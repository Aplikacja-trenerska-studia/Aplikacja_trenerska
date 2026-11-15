export function exerciseSearchParams(filters: { q?: string; muscleGroup?: string; type?: string }) {
  const params = new URLSearchParams()
  const q = filters.q?.trim()
  if (q) params.set('q', q)
  if (filters.muscleGroup) params.set('muscleGroup', filters.muscleGroup)
  if (filters.type) params.set('type', filters.type)
  return params
}

export function exerciseListPath(filters: { q?: string; muscleGroup?: string; type?: string }) {
  const query = exerciseSearchParams(filters).toString()
  return `/api/exercises${query ? `?${query}` : ''}`
}
