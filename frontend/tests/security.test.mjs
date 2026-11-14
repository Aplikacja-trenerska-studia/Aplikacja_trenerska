import assert from 'node:assert/strict'
import test from 'node:test'
import { tokenExpiresAt } from '../src/lib/token-expiry.ts'
import { passwordError } from '../src/lib/password-validation.ts'

const jwt = (claims) => `header.${Buffer.from(JSON.stringify(claims)).toString('base64url')}.signature`

test('missing, malformed and non-numeric expiry cannot create a client session', () => {
  for (const token of ['', 'invalid', 'a.???', jwt({}), jwt({ exp: '9999999999' })]) {
    assert.equal(tokenExpiresAt(token), 0)
  }
})

test('JWT expiry uses milliseconds and distinguishes expired sessions', () => {
  const expires = Math.floor(Date.now() / 1000) + 60
  assert.equal(tokenExpiresAt(jwt({ exp: expires })), expires * 1000)
  assert.ok(tokenExpiresAt(jwt({ exp: expires - 120 })) < Date.now())
  assert.ok(tokenExpiresAt(jwt({ exp: expires })) > Date.now())
})

test('password limits reject short passwords and mismatched confirmation', () => {
  assert.ok(passwordError('short'))
  assert.ok(passwordError('Training123', 'Training124'))
  assert.equal(passwordError('Training123', 'Training123'), null)
})

test('BCrypt byte limit handles Polish characters and emoji', () => {
  assert.equal(passwordError('a'.repeat(72)), null)
  assert.ok(passwordError('a'.repeat(73)))
  assert.equal(passwordError('ą'.repeat(36)), null)
  assert.ok(passwordError('ą'.repeat(37)))
  assert.ok(passwordError('🏋️'.repeat(20)))
})
