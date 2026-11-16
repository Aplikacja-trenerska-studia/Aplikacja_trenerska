import assert from 'node:assert/strict'
import test from 'node:test'
import { normalizeApiOrigin, resolveApiUrl } from '../src/lib/api-url.ts'

test('local requests retain the existing /api proxy and query string', () => {
  assert.equal(resolveApiUrl('/api/exercises?q=martwy%20ci%C4%85g&type=STRENGTH'),
    '/api/exercises?q=martwy%20ci%C4%85g&type=STRENGTH')
  assert.equal(normalizeApiOrigin('  '), '')
})

test('a deployed backend origin keeps API paths and query parameters', () => {
  assert.equal(resolveApiUrl('/api/plans/42', ' https://backend.vercel.app/ '),
    'https://backend.vercel.app/api/plans/42')
  assert.equal(resolveApiUrl('/api/exercises?muscleGroup=CORE&q=a%26b', 'https://backend.vercel.app'),
    'https://backend.vercel.app/api/exercises?muscleGroup=CORE&q=a%26b')
})

test('deployment origin excludes credentials, paths, fragments and query strings', () => {
  for (const value of [
    'backend.vercel.app', 'https://user:password@backend.vercel.app',
    'https://backend.vercel.app/api', 'https://backend.vercel.app?token=secret',
    'https://backend.vercel.app#fragment', 'https://backend.vercel.app/?',
    'javascript:alert(1)', 'ftp://backend.vercel.app', 'http://backend.vercel.app',
  ]) assert.throws(() => normalizeApiOrigin(value))
})

test('HTTP loopback backends work locally while Vercel requires HTTPS', () => {
  for (const value of ['http://localhost:8080', 'http://127.0.0.1:8080', 'http://[::1]:8080']) {
    assert.equal(normalizeApiOrigin(value), value)
    assert.throws(() => normalizeApiOrigin(value, true))
  }
  assert.equal(normalizeApiOrigin('https://backend.vercel.app', true), 'https://backend.vercel.app')
})

test('an API request cannot replace the configured origin or escape its namespace', () => {
  for (const path of ['https://unrelated.test/api/users/me', '//unrelated.test/api/users/me',
    '/login', '/api/../outside', '/api\\outside', '/api/users#fragment']) {
    assert.throws(() => resolveApiUrl(path, 'https://backend.vercel.app'))
  }
})
