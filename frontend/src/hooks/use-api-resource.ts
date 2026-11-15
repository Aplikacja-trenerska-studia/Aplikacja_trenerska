import { useEffect, useState } from 'react'
import { apiFetch } from '@/lib/api'
import type { ApiError } from '@/types/auth'

type Resource<T> = { path: string; data?: T; loading: boolean; error: ApiError | null }

export function useApiResource<T>(path: string) {
  const [resource, setResource] = useState<Resource<T>>({ path, loading: true, error: null })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let active = true
    const controller = new AbortController()
    apiFetch<T>(path, { signal: controller.signal }).then((data) => {
      if (active) setResource({ path, data, loading: false, error: null })
    }).catch((error: ApiError) => {
      if (active) setResource({ path, loading: false, error: { ...error, message: error.message || 'Nie udało się wczytać danych.' } })
    })
    return () => { active = false; controller.abort() }
  }, [path, attempt])

  function reload() {
    setResource({ path, loading: true, error: null })
    setAttempt((value) => value + 1)
  }

  // Never display results from an earlier URL while a new filter or plan is loading.
  return { ...(resource.path === path ? resource : { path, loading: true, error: null }), reload }
}
