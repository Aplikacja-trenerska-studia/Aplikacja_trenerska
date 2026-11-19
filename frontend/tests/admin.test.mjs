import test from 'node:test'
import assert from 'node:assert/strict'
import { accountStatus, adminSearchParams, adminUsersPath, canChangeRole, readAdminFilters } from '../src/lib/admin.ts'
import { loginReturnPath } from '../src/lib/return-path.ts'

const user = { id: 2, email: 'user@example.test', displayName: 'User', role: 'USER', enabled: true, blocked: false, createdAt: '2026-01-01T00:00:00Z' }

test('admin filter URL keeps literal search text and AND filters without extra parameters', () => {
  const filters = readAdminFilters(new URLSearchParams({ q: '  Żaneta %_+&? ', role: 'TRAINER', status: 'ACTIVE', page: '3', untrusted: 'ignored' }))
  const url = new URL(adminUsersPath(filters), 'http://localhost')
  assert.equal(url.pathname, '/api/admin/users')
  assert.equal(url.searchParams.get('q'), 'Żaneta %_+&?')
  assert.equal(url.searchParams.get('role'), 'TRAINER')
  assert.equal(url.searchParams.get('status'), 'ACTIVE')
  assert.equal(url.searchParams.get('page'), '3')
  assert.equal(url.searchParams.get('size'), '20')
  assert.equal(url.searchParams.has('untrusted'), false)
})

test('admin malformed filter values cannot turn into invalid or unbounded API paging', () => {
  for (const page of ['-1', 'abc', '1.5', 'Infinity', '107374183', '2147483647', '2147483648', '999999999999999999']) {
    const filters = readAdminFilters(new URLSearchParams({ page, role: 'SUPERADMIN', status: 'enabled', q: 'a'.repeat(300) }))
    assert.equal(filters.page, 0)
    assert.equal(filters.role, '')
    assert.equal(filters.status, '')
    assert.equal(filters.q.length, 255)
  }
  assert.equal(readAdminFilters(new URLSearchParams({ page: '107374182' })).page, 107374182)
})

test('applying a different admin filter can reset page while paging preserves the filters', () => {
  const filters = { q: 'user@example.test', role: 'USER', status: 'BLOCKED', page: 4 }
  const firstPage = adminSearchParams(filters, 0)
  assert.equal(firstPage.has('page'), false)
  assert.equal(firstPage.get('q'), filters.q)
  assert.equal(firstPage.get('role'), filters.role)
  assert.equal(firstPage.get('status'), filters.status)
  assert.equal(readAdminFilters(adminSearchParams(filters, 5)).page, 5)
})

test('administrative block remains distinct from email confirmation', () => {
  assert.equal(accountStatus({ enabled: true, blocked: false }), 'ACTIVE')
  assert.equal(accountStatus({ enabled: false, blocked: false }), 'PENDING')
  assert.equal(accountStatus({ enabled: false, blocked: true }), 'BLOCKED')
  assert.equal(accountStatus({ enabled: true, blocked: true }), 'BLOCKED')
})

test('admin role controls protect self, unconfirmed accounts and blocked admin promotion', () => {
  assert.equal(canChangeRole(user, 1, 'TRAINER'), true)
  assert.equal(canChangeRole(user, 1, 'ADMIN'), true)
  assert.equal(canChangeRole(user, 2, 'ADMIN'), false)
  assert.equal(canChangeRole(user, 1, 'USER'), false)
  assert.equal(canChangeRole({ ...user, enabled: false }, 1, 'TRAINER'), false)
  assert.equal(canChangeRole({ ...user, blocked: true }, 1, 'ADMIN'), false)
  assert.equal(canChangeRole({ ...user, blocked: true }, 1, 'TRAINER'), true)
})

test('admin login return path preserves local filters and rejects external URLs', () => {
  assert.equal(loginReturnPath('/admin?q=Żaneta&role=USER&page=2'), '/admin?q=%C5%BBaneta&role=USER&page=2')
  assert.equal(loginReturnPath('https://example.com/admin'), '/profil')
  assert.equal(loginReturnPath('//example.com/admin'), '/profil')
})
