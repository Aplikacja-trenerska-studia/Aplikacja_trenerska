import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
import { mkdir } from 'node:fs/promises'
import { createRequire } from 'node:module'
import { resolve } from 'node:path'

const { chromium } = createRequire(import.meta.url)('playwright')
const app = new URL(process.env.APP_URL || 'http://localhost:3000')
const mailbox = new URL(process.env.MAILPIT_URL || 'http://localhost:8025')
for (const url of [app, mailbox]) assert.ok(url.protocol === 'http:' && ['localhost', '127.0.0.1'].includes(url.hostname), 'Use local HTTP services only')
assert.match(process.env.COMPOSE_PROJECT_NAME || '', /^admin-[a-z0-9-]+$/, 'Use a dedicated admin-* Compose project')
const output = resolve('backend/target/admin-ui')
await mkdir(output, { recursive: true })
const admin = { email: 'dev@example.test', password: 'devdevdev' }
const account = { email: `admin-ui-${randomUUID()}@example.test`, displayName: 'Użytkownik panelu administracji', password: `Admin-ui-${randomUUID()}!` }
let browser
let adminPage
let userPage
let adminToken
let created = false
let activated = false
let stage = 'admin login'
const errors = []

async function api(path, { method = 'GET', body, token, expected = 200 } = {}) {
  const response = await fetch(new URL(path, app), {
    method, headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined, signal: AbortSignal.timeout(20_000),
  })
  assert.equal(response.status, expected, `${stage}: ${method} ${path}`)
  return response.status === 204 ? undefined : response.json()
}

async function mailToken(path) {
  for (let attempt = 0; attempt < 30; attempt++) {
    const search = new URL('/api/v1/search', mailbox)
    search.searchParams.set('query', `to:${account.email}`)
    const result = await (await fetch(search)).json()
    for (const summary of result.messages || []) {
      const message = await (await fetch(new URL(`/api/v1/message/${summary.ID}`, mailbox))).json()
      assert.ok(message.To.some((recipient) => recipient.Address === account.email))
      for (const raw of (message.Text || '').match(/https?:\/\/[^\s<>]+/g) || []) {
        const link = new URL(raw)
        if (link.pathname === `/${path}`) { assert.equal(link.origin, app.origin); return link.searchParams.get('token') }
      }
    }
    await new Promise((done) => setTimeout(done, 200))
  }
  throw new Error(`Missing local account email: ${path}`)
}

async function loginApi(credentials, expected = 200) {
  return api('/api/auth/login', { method: 'POST', body: { email: credentials.email, password: credentials.password }, expected })
}

async function loginUi(page, credentials) {
  await page.goto(new URL('/logowanie', app).href)
  await page.getByLabel('E-mail', { exact: true }).fill(credentials.email)
  await page.getByLabel('Hasło', { exact: true }).fill(credentials.password)
  await page.getByRole('button', { name: 'Zaloguj się', exact: true }).click()
  await page.waitForURL((url) => url.pathname === '/profil')
}

function watch(page) {
  page.setDefaultTimeout(15_000)
  page.on('pageerror', (error) => errors.push(error.message))
}

async function screenshot(page, name) {
  assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), `${stage}: horizontal overflow`)
  await page.screenshot({ path: resolve(output, `${name}.png`), fullPage: true })
}

function userCard(email = account.email) { return adminPage.getByRole('article', { name: `Konto: ${email}`, exact: true }) }
function dialog() { return adminPage.getByRole('dialog') }
function stat(label) { return adminPage.locator('dl').first().getByText(label, { exact: true }).locator('..').locator('dd') }

async function findUser(status = '', role = '') {
  await adminPage.locator('#admin-search').fill(account.email)
  await adminPage.locator('#admin-filter-role').selectOption(role)
  await adminPage.locator('#admin-filter-status').selectOption(status)
  await adminPage.getByRole('button', { name: 'Szukaj użytkowników', exact: true }).click()
  await userCard().waitFor()
}

async function confirmAction() {
  await dialog().getByRole('button', { name: 'Potwierdź zmianę', exact: true }).click()
  await dialog().waitFor({ state: 'hidden' })
}

async function setRole(role) {
  await userCard().getByLabel(`Nowa rola: ${account.email}`, { exact: true }).selectOption(role)
  await userCard().getByRole('button', { name: `Zmień rolę: ${account.email}`, exact: true }).click()
  await adminPage.getByRole('dialog', { name: 'Zmiana roli konta', exact: true }).waitFor()
  await confirmAction()
}

async function freshUserUi() {
  await userPage.goto(new URL('/profil', app).href)
  await userPage.waitForURL((url) => url.pathname === '/logowanie')
  await loginUi(userPage, account)
}

try {
  const authenticated = await loginApi(admin)
  assert.equal(authenticated.user.role, 'ADMIN', 'Bootstrap the dedicated local admin account before running this test')
  adminToken = authenticated.accessToken
  const initialOverview = await api('/api/admin/overview', { token: adminToken })
  browser = await chromium.launch({ headless: true, ...(process.env.UI_CHROME_PATH ? { executablePath: process.env.UI_CHROME_PATH } : {}) })
  adminPage = await browser.newPage({ viewport: { width: 1280, height: 900 } })
  watch(adminPage)
  await loginUi(adminPage, admin)
  await adminPage.getByRole('navigation', { name: 'Trening', exact: true }).getByRole('link', { name: 'Administracja', exact: true }).click()
  await adminPage.getByRole('heading', { name: 'Panel administratora', exact: true }).waitFor()
  await userCard(admin.email).waitFor()
  assert.equal(await stat('Administratorzy').innerText(), new Intl.NumberFormat('pl-PL').format(initialOverview.admins))
  assert.ok(await userCard(admin.email).getByLabel(`Nowa rola: ${admin.email}`, { exact: true }).isDisabled())
  assert.ok(await userCard(admin.email).getByRole('button', { name: `Zablokuj konto: ${admin.email}`, exact: true }).isDisabled())

  stage = 'register and activate a disposable user through real SMTP'
  await api('/api/auth/register', { method: 'POST', expected: 201, body: { email: account.email, password: account.password, displayName: account.displayName } })
  created = true
  await api('/api/auth/confirm', { method: 'POST', body: { token: await mailToken('confirm-email') } })
  activated = true
  const originalUser = await loginApi(account)
  const userId = originalUser.user.id
  assert.equal(originalUser.user.role, 'USER')
  userPage = await browser.newPage({ viewport: { width: 1280, height: 900 } })
  watch(userPage)
  await loginUi(userPage, account)
  await userPage.goto(new URL('/admin', app).href)
  await userPage.getByRole('heading', { name: 'Panel jest dostępny dla administratorów', exact: true }).waitFor()
  await api('/api/admin/overview', { token: originalUser.accessToken, expected: 403 })

  stage = 'filter users and cancel a role change'
  await findUser('ACTIVE', 'USER')
  assert.equal(new URL(adminPage.url()).searchParams.get('q'), account.email)
  assert.equal(new URL(adminPage.url()).searchParams.get('status'), 'ACTIVE')
  assert.equal((await api(`/api/admin/users?q=${encodeURIComponent(account.email)}`, { token: adminToken })).totalElements, 1)
  let roleWrites = 0
  const rolePath = `/api/admin/users/${userId}/role`
  adminPage.on('request', (request) => { if (request.method() === 'PATCH' && new URL(request.url()).pathname === rolePath) roleWrites++ })
  await userCard().getByLabel(`Nowa rola: ${account.email}`, { exact: true }).selectOption('TRAINER')
  await userCard().getByRole('button', { name: `Zmień rolę: ${account.email}`, exact: true }).click()
  await dialog().getByRole('button', { name: 'Anuluj', exact: true }).click()
  await dialog().waitFor({ state: 'hidden' })
  assert.equal(roleWrites, 0)
  assert.equal((await api('/api/users/me', { token: originalUser.accessToken })).role, 'USER')

  stage = 'role change error preserves confirmation and retry revokes old JWT'
  await findUser()
  await userCard().getByLabel(`Nowa rola: ${account.email}`, { exact: true }).selectOption('TRAINER')
  const rolePattern = `**${rolePath}`
  await adminPage.route(rolePattern, (route) => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Testowy błąd zapisu roli' }) }))
  await userCard().getByRole('button', { name: `Zmień rolę: ${account.email}`, exact: true }).click()
  await dialog().getByRole('button', { name: 'Potwierdź zmianę', exact: true }).click()
  await dialog().getByRole('alert').filter({ hasText: 'Testowy błąd zapisu roli' }).waitFor()
  assert.match(await dialog().innerText(), /Trener/)
  assert.equal(roleWrites, 1)
  assert.equal((await api('/api/users/me', { token: originalUser.accessToken })).role, 'USER')
  await adminPage.unroute(rolePattern)
  let release
  const gate = new Promise((done) => { release = done })
  await adminPage.route(rolePattern, async (route) => { assert.equal(route.request().postDataJSON().role, 'TRAINER'); await gate; await route.continue() })
  await dialog().getByRole('button', { name: 'Potwierdź zmianę', exact: true }).click()
  await dialog().getByRole('button', { name: 'Zapisywanie…', exact: true }).waitFor()
  assert.ok(await dialog().getByRole('button', { name: 'Zapisywanie…', exact: true }).isDisabled())
  release()
  await dialog().waitFor({ state: 'hidden' })
  await adminPage.unroute(rolePattern)
  assert.equal(roleWrites, 2)
  await api('/api/users/me', { token: originalUser.accessToken, expected: 401 })
  const trainer = await loginApi(account)
  assert.equal(trainer.user.role, 'TRAINER')
  await freshUserUi()
  await userPage.getByRole('navigation', { name: 'Trening', exact: true }).getByRole('link', { name: 'Panel trenera', exact: true }).click()
  await userPage.getByRole('heading', { name: 'Panel trenera', exact: true }).waitFor()

  stage = 'block through UI, reject old JWT and login'
  await userCard().getByRole('button', { name: `Zablokuj konto: ${account.email}`, exact: true }).click()
  await adminPage.getByRole('dialog', { name: 'Blokada konta', exact: true }).waitFor()
  await adminPage.setViewportSize({ width: 375, height: 812 })
  await screenshot(adminPage, 'dialog-mobile')
  await confirmAction()
  await api('/api/users/me', { token: trainer.accessToken, expected: 401 })
  await loginApi(account, 403)
  await findUser('BLOCKED')
  await userCard().getByText('Zablokowane', { exact: true }).waitFor()
  await screenshot(adminPage, 'admin-blocked-mobile')

  stage = 'unblock preserves revocation and enables fresh login'
  await userCard().getByRole('button', { name: `Odblokuj konto: ${account.email}`, exact: true }).click()
  await adminPage.getByRole('dialog', { name: 'Odblokowanie konta', exact: true }).waitFor()
  await confirmAction()
  await api('/api/users/me', { token: trainer.accessToken, expected: 401 })
  const unblockedTrainer = await loginApi(account)
  assert.equal(unblockedTrainer.user.role, 'TRAINER')
  await findUser()

  stage = 'grant another administrator and revoke access again'
  await setRole('ADMIN')
  await api('/api/users/me', { token: unblockedTrainer.accessToken, expected: 401 })
  const secondAdmin = await loginApi(account)
  assert.equal(secondAdmin.user.role, 'ADMIN')
  await api('/api/admin/overview', { token: secondAdmin.accessToken })
  await adminPage.waitForFunction((expected) => {
    const label = [...document.querySelectorAll('dl:first-of-type dt')].find((entry) => entry.textContent === 'Administratorzy')
    return label?.nextElementSibling?.textContent === String(expected)
  }, initialOverview.admins + 1)
  await setRole('USER')
  await api('/api/admin/overview', { token: secondAdmin.accessToken, expected: 401 })
  const ordinaryUser = await loginApi(account)
  assert.equal(ordinaryUser.user.role, 'USER')
  await api('/api/admin/overview', { token: ordinaryUser.accessToken, expected: 403 })
  await adminPage.waitForFunction((expected) => {
    const label = [...document.querySelectorAll('dl:first-of-type dt')].find((entry) => entry.textContent === 'Administratorzy')
    return label?.nextElementSibling?.textContent === String(expected)
  }, initialOverview.admins)
  await freshUserUi()
  await userPage.goto(new URL('/admin', app).href)
  await userPage.getByRole('heading', { name: 'Panel jest dostępny dla administratorów', exact: true }).waitFor()
  await screenshot(adminPage, 'admin-mobile')
  await adminPage.setViewportSize({ width: 1280, height: 900 })
  await screenshot(adminPage, 'admin-desktop')
  assert.deepEqual(errors, [], 'Browser JavaScript errors')
  console.log('PASS: admin dashboard/search/filters; USER guard; role cancel + failed-save retry; USER→TRAINER→ADMIN→USER; block/unblock; JWT revocation; native dialogs; desktop/mobile without overflow or JS errors.')
} catch (error) {
  console.error(`FAIL: ${stage}: ${error.message}`)
  throw error
} finally {
  if (created && adminToken) {
    try {
      const page = await api(`/api/admin/users?q=${encodeURIComponent(account.email)}`, { token: adminToken })
      const target = page.content.find((entry) => entry.email === account.email)
      if (target) {
        await api(`/api/admin/users/${target.id}/block`, { method: 'PATCH', token: adminToken, body: { blocked: false } })
        await api(`/api/admin/users/${target.id}/role`, { method: 'PATCH', token: adminToken, body: { role: 'USER' } })
        if (!activated) await api('/api/auth/confirm', { method: 'POST', body: { token: await mailToken('confirm-email') } })
        const login = await loginApi(account)
        await api('/api/users/me/deletion-request', { method: 'POST', token: login.accessToken, body: { password: account.password } })
        await api('/api/auth/confirm-deletion', { method: 'POST', body: { token: await mailToken('confirm-deletion') } })
      }
    } catch { console.error('Cleanup failed for the disposable test account; remove only the dedicated test project after diagnostics.') }
  }
  await browser?.close()
}
