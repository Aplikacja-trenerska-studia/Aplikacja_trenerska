import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
import { spawnSync } from 'node:child_process'
import { mkdir } from 'node:fs/promises'
import { createRequire } from 'node:module'
import { resolve } from 'node:path'

const { chromium } = createRequire(import.meta.url)('playwright')
const app = new URL(process.env.APP_URL || 'http://localhost:3000')
const mailbox = new URL(process.env.MAILPIT_URL || 'http://localhost:8025')
for (const url of [app, mailbox]) assert.ok(url.protocol === 'http:' && ['localhost', '127.0.0.1'].includes(url.hostname), 'Use local services only')
const project = process.env.COMPOSE_PROJECT_NAME || ''
assert.match(project, /^sprint3-[a-z0-9-]+$/, 'Use a dedicated sprint3-* Compose project, never an existing application database')
const output = resolve('backend/target/sprint3-ui')
await mkdir(output, { recursive: true })
const suffix = randomUUID()
const password = `Training-${randomUUID()}!`
const student = { email: `s3-user-${suffix}@example.test`, name: 'Podopieczny S3' }
const trainer = { email: `s3-trainer-${suffix}@example.test`, name: 'Trener S3' }
const accounts = []
const errors = []
let browser
let stage = 'registration'

async function api(path, { method = 'GET', body, token, expected = 200 } = {}) {
  const response = await fetch(new URL(path, app), {
    method, headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined, signal: AbortSignal.timeout(20_000),
  })
  assert.equal(response.status, expected, `${stage}: ${method} ${path}`)
  return response.status === 204 ? undefined : response.json()
}
async function mailToken(email, path) {
  for (let attempt = 0; attempt < 30; attempt++) {
    const search = new URL('/api/v1/search', mailbox)
    search.searchParams.set('query', `to:${email}`)
    const result = await (await fetch(search)).json()
    for (const summary of result.messages || []) {
      const mail = await (await fetch(new URL(`/api/v1/message/${summary.ID}`, mailbox))).json()
      assert.ok(mail.To.some((recipient) => recipient.Address === email))
      for (const raw of (mail.Text || '').match(/https?:\/\/[^\s<>]+/g) || []) {
        const link = new URL(raw)
        if (link.pathname === `/${path}`) { assert.equal(link.origin, app.origin); return link.searchParams.get('token') }
      }
    }
    await new Promise((done) => setTimeout(done, 200))
  }
  throw new Error(`Missing account email: ${path}`)
}
async function login(page, account) {
  await page.goto(new URL('/logowanie', app).href)
  await page.getByLabel('E-mail', { exact: true }).fill(account.email)
  await page.getByLabel('Hasło', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Zaloguj się', exact: true }).click()
  await page.waitForURL((url) => url.pathname === '/profil')
}
async function screenshot(page, name) {
  assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), `${stage}: horizontal overflow`)
  assert.doesNotMatch(await page.locator('body').innerText(), /tryb demonstracyjny|prowadząc.{0,4} prezentac|prezentację/i)
  await page.screenshot({ path: resolve(output, `${name}.png`), fullPage: true })
}
function promoteTrainer() {
  const discovery = spawnSync('docker', ['compose', '-p', project, 'ps', '-q', 'db'], { encoding: 'utf8' })
  assert.equal(discovery.status, 0)
  const container = discovery.stdout.trim()
  assert.match(container, /^[a-f0-9]{12,64}$/)
  const promotion = spawnSync('docker', ['exec', '-i', container, 'psql', '-X', '-U', 'aplikacja', '-d', 'aplikacja_trenerska', '-v', 'ON_ERROR_STOP=1', '-At'], {
    input: `UPDATE users SET role='TRAINER',token_version=token_version+1 WHERE email='${trainer.email}';`, encoding: 'utf8',
  })
  assert.equal(promotion.status, 0, 'Local operator promotion failed')
  assert.match(promotion.stdout, /UPDATE 1/)
}

try {
  browser = await chromium.launch({ headless: true, ...(process.env.UI_CHROME_PATH ? { executablePath: process.env.UI_CHROME_PATH } : {}) })
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } })
  page.setDefaultTimeout(15_000)
  page.on('pageerror', (error) => errors.push(error.message))
  await page.goto(new URL('/rejestracja', app).href)
  await page.getByLabel('Imię lub nazwa wyświetlana').fill(student.name)
  await page.getByLabel('E-mail', { exact: true }).fill(student.email)
  await page.getByLabel('Hasło (min. 8 znaków)', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Załóż konto', exact: true }).click()
  await page.getByRole('status').filter({ hasText: 'Konto utworzone' }).waitFor()
  await screenshot(page, 'registration-standard-mail')
  await page.goto(new URL(`/confirm-email?token=${await mailToken(student.email, 'confirm-email')}`, app).href)
  await page.getByRole('button', { name: 'Potwierdź adres e-mail', exact: true }).click()
  await page.getByRole('heading', { name: 'Adres e-mail potwierdzony' }).waitFor()
  student.token = (await api('/api/auth/login', { method: 'POST', body: { email: student.email, password } })).accessToken
  accounts.push(student)
  await login(page, student)

  stage = 'create plan through UI'
  await page.goto(new URL('/plany/nowy', app).href)
  await page.getByLabel('Nazwa planu', { exact: true }).fill('Plan własny S3')
  await page.getByLabel('Nazwa dnia').fill('Dzień A')
  await page.getByRole('button', { name: 'Dodaj ćwiczenie', exact: true }).click()
  await page.getByLabel('Szukaj ćwiczenia').fill('Pompki klasyczne')
  await page.getByRole('button', { name: 'Dodaj: Pompki klasyczne', exact: true }).click()
  await page.getByRole('button', { name: 'Zamknij wybór ćwiczeń', exact: true }).click()
  await page.getByRole('button', { name: 'Zapisz plan', exact: true }).click()
  await page.waitForURL((url) => /^\/plany\/\d+$/.test(url.pathname))
  const plan = await api(new URL(page.url()).pathname.replace('/plany/', '/api/plans/'), { token: student.token })
  const exerciseId = plan.days[0].exercises[0].exercise.id

  stage = 'save workout through UI, preserve draft on error'
  await page.getByRole('link', { name: 'Wykonaj trening', exact: true }).click()
  await page.locator('#weight-0').fill('12,50')
  await page.route('**/api/results', (route) => route.request().method() === 'POST' ? route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Test błędu zapisu' }) }) : route.continue())
  await page.getByRole('button', { name: 'Zapisz trening', exact: true }).click()
  await page.getByRole('alert').filter({ hasText: 'Test błędu zapisu' }).waitFor()
  assert.equal(await page.locator('#weight-0').inputValue(), '12,50')
  assert.equal((await api('/api/results', { token: student.token })).length, 0)
  await page.unroute('**/api/results')
  await screenshot(page, 'workout-desktop')
  await page.setViewportSize({ width: 375, height: 812 })
  await screenshot(page, 'workout-mobile')
  await page.getByRole('button', { name: 'Zapisz trening', exact: true }).click()
  await page.getByRole('heading', { name: 'Trening zapisany', exact: true }).waitFor()
  const results = await api('/api/results', { token: student.token })
  assert.ok(results.some((row) => row.weight === 12.5))
  const yesterday = new Date(Date.now() - 86400000).toISOString()
  await api('/api/results', { method: 'POST', token: student.token, expected: 201, body: { time: yesterday, planId: plan.id, planDayId: plan.days[0].id, sets: [{ exerciseId, setNo: 1, reps: 10, weight: 10 }] } })

  stage = 'body metrics and charts'
  await page.getByRole('link', { name: 'Zobacz progres', exact: true }).click()
  await page.getByRole('button', { name: 'Nowy pomiar', exact: true }).click()
  await page.locator('#metric-weightKg').fill('78,25')
  await page.locator('#metric-waistCm').fill('84,5')
  await page.getByRole('button', { name: 'Zapisz pomiar', exact: true }).click()
  await page.getByRole('status').filter({ hasText: 'Pomiar zapisany.' }).waitFor()
  await api('/api/body-metrics', { method: 'POST', token: student.token, expected: 201, body: { time: yesterday, weightKg: 79, waistCm: 85 } })
  await page.reload()
  await page.getByRole('img', { name: /Pompki klasyczne/ }).waitFor()
  await page.getByRole('img', { name: /^Waga/ }).waitFor()
  await page.locator('#chart-metric').selectOption('waistCm')
  await screenshot(page, 'progress-mobile')
  await page.setViewportSize({ width: 1280, height: 900 })
  await screenshot(page, 'progress-desktop')

  stage = 'trainer account and group invitation'
  await api('/api/auth/register', { method: 'POST', expected: 201, body: { email: trainer.email, password, displayName: trainer.name } })
  await api('/api/auth/confirm', { method: 'POST', body: { token: await mailToken(trainer.email, 'confirm-email') } })
  promoteTrainer()
  trainer.token = (await api('/api/auth/login', { method: 'POST', body: { email: trainer.email, password } })).accessToken
  accounts.push(trainer)
  const source = await api('/api/plans', { method: 'POST', expected: 201, token: trainer.token, body: { name: 'Plan od trenera S3', days: [{ name: 'Trening z trenerem', exercises: [{ exerciseId, sets: 2, reps: 8, weight: 20 }] }] } })
  const trainerPage = await browser.newPage({ viewport: { width: 1280, height: 900 } })
  trainerPage.setDefaultTimeout(15_000)
  trainerPage.on('pageerror', (error) => errors.push(error.message))
  await login(trainerPage, trainer)
  await trainerPage.goto(new URL('/trener', app).href)
  await trainerPage.locator('#group-name').fill('Grupa S3')
  await trainerPage.getByRole('button', { name: 'Utwórz grupę', exact: true }).click()
  await trainerPage.locator('#member-email').fill(student.email)
  await trainerPage.getByRole('button', { name: 'Zaproś do grupy', exact: true }).click()
  await trainerPage.getByText('Oczekuje na przyjęcie zaproszenia', { exact: true }).waitFor()
  const groupId = (await api('/api/groups', { token: trainer.token }))[0].id
  const studentId = (await api('/api/users/me', { token: student.token })).id
  await api(`/api/groups/${groupId}/members/${studentId}/results`, { token: trainer.token, expected: 404 })
  await screenshot(trainerPage, 'trainer-invitation')
  await page.goto(new URL('/wspolpraca', app).href)
  await page.getByRole('button', { name: 'Przyjmij zaproszenie', exact: true }).click()
  await page.getByText('Współpraca aktywna', { exact: true }).waitFor()
  await trainerPage.reload()
  await trainerPage.locator('#assignment-plan').selectOption(String(source.id))
  await trainerPage.getByRole('button', { name: 'Przypisz plan', exact: true }).click()
  await trainerPage.getByRole('status').filter({ hasText: 'Plan przypisany.' }).waitFor()
  await trainerPage.getByRole('button', { name: `Pokaż progres: ${student.name}`, exact: true }).click()
  await trainerPage.getByRole('img', { name: /Pompki klasyczne/ }).waitFor()
  await screenshot(trainerPage, 'trainer-progress-desktop')
  await trainerPage.setViewportSize({ width: 375, height: 812 })
  await screenshot(trainerPage, 'trainer-progress-mobile')

  stage = 'recipient executes assigned copy and revokes consent'
  await page.reload()
  await page.getByRole('heading', { name: 'Plan od trenera S3', exact: true }).waitFor()
  const assignments = await api('/api/my-assignments', { token: student.token })
  assert.equal(assignments.length, 1)
  assert.notEqual(assignments[0].planId, source.id)
  await screenshot(page, 'cooperation-desktop')
  await page.getByRole('link', { name: 'Wykonaj trening', exact: true }).click()
  await page.locator('#weight-0').fill('22,5')
  await page.getByRole('button', { name: 'Zapisz trening', exact: true }).click()
  await page.getByRole('heading', { name: 'Trening zapisany', exact: true }).waitFor()
  assert.ok((await api(`/api/groups/${groupId}/members/${studentId}/results`, { token: trainer.token })).some((row) => row.planName === source.name && row.weight === 22.5))
  await page.goto(new URL('/wspolpraca', app).href)
  await page.getByRole('button', { name: 'Zakończ współpracę', exact: true }).click()
  await page.getByRole('button', { name: 'Potwierdź', exact: true }).click()
  await page.getByRole('status').filter({ hasText: 'Współpraca została zakończona.' }).waitFor()
  await api(`/api/groups/${groupId}/members/${studentId}/results`, { token: trainer.token, expected: 404 })
  await api(`/api/plans/${assignments[0].planId}`, { token: student.token })
  await page.goto(new URL('/trener', app).href)
  await page.getByRole('heading', { name: 'Panel jest dostępny dla trenerów', exact: true }).waitFor()
  assert.deepEqual(errors, [], 'Browser JavaScript errors')
  console.log('PASS: registration → SMTP confirmation → UI plan → workout → metrics/charts → trainer invitation/acceptance → assigned copy → workout → consent revocation; desktop/mobile; no presentation copy or JS errors.')
} catch (error) {
  console.error(`FAIL: ${stage}: ${error.message}`)
  throw error
} finally {
  for (const account of accounts.reverse()) {
    try {
      await api('/api/users/me/deletion-request', { method: 'POST', token: account.token, body: { password } })
      await api('/api/auth/confirm-deletion', { method: 'POST', body: { token: await mailToken(account.email, 'confirm-deletion') } })
    } catch { console.error('Cleanup failed for a disposable test account; remove the dedicated Compose project after the test.') }
  }
  await browser?.close()
}
