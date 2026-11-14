import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'

// Run against the local Docker stack: node tests/smoke-accounts.mjs
// Uses only a fresh disposable account and Mailpit; never sends real email.
const app = new URL(process.env.APP_URL || 'http://localhost:3000')
const mailbox = new URL(process.env.MAILPIT_URL || 'http://localhost:8025')
for (const url of [app, mailbox]) {
  assert.ok(url.protocol === 'http:' && ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname),
    'This smoke test only supports local HTTP services')
}
const email = `smoke-${randomUUID()}@example.test`
const unknownEmail = `missing-${randomUUID()}@example.test`
const password = `Initial-${randomUUID()}!`
const newPassword = `Changed-${randomUUID()}!`
const seenMessages = new Set()
const steps = []
let stage = 'startup'

async function request(path, { method = 'GET', body, jwt, expected = 200, statusOnly = false } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (jwt) headers.Authorization = `Bearer ${jwt}`
  const response = await fetch(new URL(path, app), {
    method, headers, body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(15_000),
  })
  assert.ok(Array.isArray(expected) ? expected.includes(response.status) : response.status === expected,
    `${stage}: unexpected HTTP status ${response.status}`)
  if (statusOnly) {
    await response.arrayBuffer()
    return response.status
  }
  const type = response.headers.get('content-type') || ''
  return type.includes('application/json') ? response.json() : response.text()
}

const post = (path, body, expected = 200, jwt) => request(path, { method: 'POST', body, expected, jwt })
const login = (value, expected = 200) => post('/api/auth/login', { email, password: value }, expected)
const me = (jwt, expected = 200) => request('/api/users/me', { jwt, expected })
const register = () => post('/api/auth/register', { email, password, displayName: 'Docker Smoke Test' }, 201)
const confirm = (token) => post('/api/auth/confirm', { token })
const reset = (token, value, expected = 200) => post('/api/auth/reset-password', { token, password: value }, expected)
const deleteRequest = (jwt, value, expected = 200) => post('/api/users/me/deletion-request', { password: value }, expected, jwt)
const deleteConfirm = (token, expected = 200) => post('/api/auth/confirm-deletion', { token }, expected)

function check(name) {
  steps.push(name)
  console.log(`PASS ${name}`)
}

async function mailLink(page) {
  for (let attempt = 0; attempt < 40; attempt++) {
    const search = new URL('/api/v1/search', mailbox)
    search.searchParams.set('query', `to:${email}`)
    const result = await fetch(search, { signal: AbortSignal.timeout(5_000) })
    assert.equal(result.status, 200, `${stage}: Mailpit search failed`)
    const payload = await result.json()
    const messages = payload.messages || []
    for (const summary of messages) {
      const id = summary.ID
      if (!id || seenMessages.has(id)) continue
      const detailResponse = await fetch(new URL(`/api/v1/message/${encodeURIComponent(id)}`, mailbox), {
        signal: AbortSignal.timeout(5_000),
      })
      assert.equal(detailResponse.status, 200, `${stage}: Mailpit message read failed`)
      const message = await detailResponse.json()
      const recipients = message.To || summary.To || []
      assert.ok(recipients.some((recipient) => recipient.Address?.toLowerCase() === email),
        `${stage}: unexpected mail recipient`)
      const text = message.Text || ''
      const links = text.match(/https?:\/\/[^\s<>]+/g) || []
      for (const raw of links) {
        const link = new URL(raw)
        if (link.pathname !== `/${page}`) continue
        assert.equal(link.origin, app.origin, `${stage}: email link points to a different frontend`)
        assert.ok(/^[A-Za-z0-9_-]{43}$/.test(link.searchParams.get('token') || ''), `${stage}: token format`)
        seenMessages.add(id)
        return link
      }
    }
    await new Promise((resolve) => setTimeout(resolve, 250))
  }
  throw new Error(`${stage}: expected Mailpit message was not delivered`)
}

try {
  stage = 'protected routes'
  await me(undefined, 401)
  await deleteRequest(undefined, password, 401)
  check(stage)

  stage = 'registration and SMTP activation mail'
  const registered = await register()
  assert.ok(!('accessToken' in registered), 'Registration must not return an access token')
  await login(password, 403)
  const activation = await mailLink('confirm-email')
  const activationToken = activation.searchParams.get('token')
  const html = await request(activation.pathname + activation.search)
  assert.ok(typeof html === 'string' && html.includes('<div id="root">'), 'Email link must load the SPA')
  await login(password, 403)
  await reset(activationToken, newPassword, 400)
  await confirm(activationToken)
  await post('/api/auth/confirm', { token: activationToken }, 400)
  const original = await login(password)
  assert.ok(typeof original.accessToken === 'string', 'Login must return an access token')
  assert.equal((await me(original.accessToken)).email, email)
  check(stage)

  stage = 'password reset through SMTP and old-session revocation'
  const known = await post('/api/auth/forgot-password', { email })
  const unknown = await post('/api/auth/forgot-password', { email: unknownEmail })
  assert.ok(JSON.stringify(known) === JSON.stringify(unknown), 'Forgot-password must hide account existence')
  const resetLink = await mailLink('reset-password')
  const resetToken = resetLink.searchParams.get('token')
  await request(resetLink.pathname + resetLink.search)
  await me(original.accessToken)
  await reset(resetToken, 'short', 400)
  await deleteConfirm(resetToken, 400)
  const concurrentResets = await Promise.all([1, 2].map(() => request('/api/auth/reset-password', {
    method: 'POST', body: { token: resetToken, password: newPassword }, expected: [200, 400], statusOnly: true,
  })))
  assert.deepEqual(concurrentResets.sort(), [200, 400], 'Only one concurrent reset may consume the token')
  await reset(resetToken, password, 400)
  await me(original.accessToken, 401)
  await login(password, 401)
  const updated = await login(newPassword)
  await me(updated.accessToken)
  check(stage)

  stage = 'account deletion requires password and explicit email confirmation'
  await deleteRequest(updated.accessToken, 'WrongPassword123!', 400)
  await deleteRequest(updated.accessToken, newPassword)
  const deletion = await mailLink('confirm-deletion')
  const deletionToken = deletion.searchParams.get('token')
  await request(deletion.pathname + deletion.search)
  await me(updated.accessToken)
  await deleteConfirm(deletionToken)
  await deleteConfirm(deletionToken, 400)
  await me(updated.accessToken, 401)
  await login(newPassword, 401)
  check(stage)

  stage = 'email reuse cannot restore deleted sessions'
  await register()
  const secondActivation = await mailLink('confirm-email')
  await confirm(secondActivation.searchParams.get('token'))
  const recreated = await login(password)
  await me(recreated.accessToken)
  await me(original.accessToken, 401)
  await me(updated.accessToken, 401)
  check(stage)

  stage = 'disposable account cleanup'
  await deleteRequest(recreated.accessToken, password)
  const cleanup = await mailLink('confirm-deletion')
  await deleteConfirm(cleanup.searchParams.get('token'))
  await me(recreated.accessToken, 401)
  check(stage)

  console.log(JSON.stringify({ result: 'PASS', checks: steps.length, email, mailpitMessagesRead: seenMessages.size }))
} catch (error) {
  // Never print response bodies, credentials, JWTs, tokens, or mail contents.
  console.error(JSON.stringify({ result: 'FAIL', stage, email, message: error.message }))
  process.exitCode = 1
}
