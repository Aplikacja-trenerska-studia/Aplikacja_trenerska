import test from 'node:test'
import assert from 'node:assert/strict'
import { exerciseListPath, exerciseSearchParams } from '../src/lib/workout-filters.ts'
import { loginReturnPath } from '../src/lib/return-path.ts'

test('empty filters request the complete catalog', () => {
  assert.equal(exerciseListPath({}), '/api/exercises')
  assert.equal(exerciseListPath({ q: '   ', muscleGroup: '', type: '' }), '/api/exercises')
})

test('combined filters preserve Polish text and literal URL metacharacters', () => {
  const path = exerciseListPath({ q: '  Ściąganie %_+&?  ', muscleGroup: 'BACK', type: 'HOME' })
  const url = new URL(path, 'http://localhost')
  assert.equal(url.pathname, '/api/exercises')
  assert.equal(url.searchParams.get('q'), 'Ściąganie %_+&?')
  assert.equal(url.searchParams.get('muscleGroup'), 'BACK')
  assert.equal(url.searchParams.get('type'), 'HOME')
  assert.equal([...url.searchParams].length, 3)
})

test('filter URLs can be restored without changing their meaning', () => {
  const params = exerciseSearchParams({ q: 'Pompki', type: 'HOME' })
  const restored = Object.fromEntries(new URLSearchParams(params.toString()))
  assert.equal(exerciseListPath(restored), '/api/exercises?q=Pompki&type=HOME')
})

test('login returns to protected lists and plan details including filters', () => {
  for (const path of ['/profil', '/plany', '/plany/123', '/plany/nowy', '/plany/123/edytuj', '/cwiczenia?type=HOME&q=Pompki']) {
    assert.equal(loginReturnPath(path), path)
  }
})

test('login return path rejects external and unrelated URLs', () => {
  for (const value of [undefined, '', '//example.com', 'https://example.com/plany', '/\\example.com/plany', '/logowanie', '/plany/nieznana-trasa']) {
    assert.equal(loginReturnPath(value), '/profil')
  }
})
