import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
import { mkdir } from 'node:fs/promises'
import { createRequire } from 'node:module'
import { resolve } from 'node:path'

// Requires Playwright (installed locally or exposed through NODE_PATH) and the local Docker stack.
const { chromium } = createRequire(import.meta.url)('playwright')
const app = new URL(process.env.APP_URL || 'http://localhost:3000')
const mailbox = new URL(process.env.MAILPIT_URL || 'http://localhost:8025')
for (const url of [app, mailbox]) assert.ok(url.protocol === 'http:' && ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname), 'Only local HTTP services are supported')
const output = resolve('backend/target/ui-screenshots')
await mkdir(output, { recursive: true })
const email = `workout-ui-${randomUUID()}@example.test`
const password = `Ui-${randomUUID()}!`
let jwt
let browser
let page
const errors = []
let stage = 'registration'

async function api(path, { method = 'GET', body, authenticated = true, expected = 200 } = {}) {
  const response = await fetch(new URL(path, app), {
    method,
    headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(authenticated && jwt ? { Authorization: `Bearer ${jwt}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(15_000),
  })
  assert.equal(response.status, expected, `${stage}: ${method} ${path}`)
  return response.status === 204 ? undefined : response.json()
}

async function mailToken(page) {
  for (let attempt = 0; attempt < 40; attempt++) {
    const search = new URL('/api/v1/search', mailbox)
    search.searchParams.set('query', `to:${email}`)
    const results = await (await fetch(search)).json()
    for (const summary of results.messages || []) {
      const message = await (await fetch(new URL(`/api/v1/message/${encodeURIComponent(summary.ID)}`, mailbox))).json()
      for (const raw of (message.Text || '').match(/https?:\/\/[^\s<>]+/g) || []) {
        const link = new URL(raw)
        if (link.pathname === `/${page}`) {
          assert.equal(link.origin, app.origin)
          return link.searchParams.get('token')
        }
      }
    }
    await new Promise((done) => setTimeout(done, 250))
  }
  throw new Error(`Missing Mailpit message for ${page}`)
}

try {
  browser = await chromium.launch({ headless: true, ...(process.env.UI_CHROME_PATH ? { executablePath: process.env.UI_CHROME_PATH } : {}) })
  page = await browser.newPage({ viewport: { width: 1280, height: 900 } })
  page.on('pageerror', (error) => errors.push(error.message))
  await page.goto(new URL('/rejestracja', app).href)
  await page.getByLabel('Imię lub nazwa wyświetlana').fill('Test kreatora planów')
  await page.getByLabel('E-mail', { exact: true }).fill(email)
  await page.getByLabel('Hasło (min. 8 znaków)', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Załóż konto', exact: true }).click()
  await page.getByRole('status').filter({ hasText: 'Konto utworzone' }).waitFor()
  await page.goto(new URL(`/confirm-email?token=${await mailToken('confirm-email')}`, app).href)
  await page.getByRole('button', { name: 'Potwierdź adres e-mail', exact: true }).click()
  await page.getByRole('heading', { name: 'Adres e-mail potwierdzony' }).waitFor()
  jwt = (await api('/api/auth/login', { method: 'POST', body: { email, password }, authenticated: false })).accessToken
  const trainingNav = () => page.getByRole('navigation', { name: 'Trening' })
  const results = (count) => page.getByText(`Liczba wyników: ${count}`, { exact: true }).waitFor()
  async function screenshot(name) {
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), `${stage}: horizontal overflow`)
    await page.screenshot({ path: resolve(output, `${name}.png`), fullPage: true })
  }

  stage = 'login redirect with preserved filters'
  await page.goto(new URL('/cwiczenia?type=HOME&q=Pompki', app).href)
  await page.waitForURL('**/logowanie')
  await page.getByLabel('E-mail', { exact: true }).fill(email)
  await page.getByLabel('Hasło', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Zaloguj się', exact: true }).click()
  await page.waitForURL((url) => url.pathname === '/cwiczenia' && url.searchParams.get('q') === 'Pompki')
  await page.getByRole('heading', { name: 'Baza ćwiczeń' }).waitFor()
  await page.getByText(/^Liczba wyników:/).waitFor()

  stage = 'combined exercise filters and navigation history'
  await page.getByLabel('Partia mięśniowa').selectOption('CHEST')
  await page.getByRole('button', { name: 'Filtruj', exact: true }).click()
  await results(2)
  await screenshot('exercises-desktop')
  await page.goBack()
  await page.waitForURL((url) => url.pathname === '/cwiczenia' && !url.searchParams.has('muscleGroup'))
  assert.equal(await page.getByLabel('Partia mięśniowa').inputValue(), '')
  assert.equal(await page.getByLabel('Nazwa ćwiczenia').inputValue(), 'Pompki')
  await page.getByRole('button', { name: 'Wyczyść filtry', exact: true }).click()
  await results(48)

  stage = 'empty exercise results'
  await page.getByLabel('Nazwa ćwiczenia').fill('zzzniewystepujace')
  await page.getByRole('button', { name: 'Filtruj', exact: true }).click()
  await page.getByRole('heading', { name: 'Brak pasujących ćwiczeń' }).waitFor()
  await page.getByRole('button', { name: 'Pokaż wszystkie ćwiczenia' }).click()
  await results(48)

  stage = 'empty plans list'
  await trainingNav().getByRole('link', { name: 'Moje plany' }).click()
  await page.getByRole('heading', { name: 'Nie masz jeszcze planów' }).waitFor()
  await screenshot('plans-empty-desktop')
  const catalog = await api('/api/exercises?muscleGroup=CHEST&type=HOME')
  stage = 'create plan with client validation, ordered days and exercises'
  await page.getByRole('link', { name: 'Nowy plan', exact: true }).click()
  await page.getByRole('heading', { name: 'Nowy plan', exact: true }).waitFor()
  await page.getByRole('button', { name: 'Zapisz plan', exact: true }).click()
  await page.getByRole('alert').filter({ hasText: 'Sprawdź zaznaczone pola' }).waitFor()
  assert.equal((await api('/api/plans')).length, 0)
  await page.getByLabel('Nazwa planu', { exact: true }).fill('Plan testowy S2-08')
  await page.getByLabel('Opis (opcjonalnie)').fill('Trening domowy — polskie znaki i opis planu.')
  const firstDay = () => page.getByRole('region', { name: 'Dzień 1', exact: true })
  const secondDay = () => page.getByRole('region', { name: 'Dzień 2', exact: true })
  await firstDay().getByLabel('Nazwa dnia').fill('Dzień A — trening klatki piersiowej')
  await firstDay().getByRole('button', { name: 'Dodaj ćwiczenie', exact: true }).click()
  await firstDay().getByLabel('Szukaj ćwiczenia').fill('Pompki')
  await firstDay().getByLabel('Partia mięśniowa').selectOption('CHEST')
  await firstDay().getByLabel('Typ ćwiczenia').selectOption('HOME')
  await firstDay().getByRole('button', { name: 'Dodaj: Pompki klasyczne', exact: true }).click()
  await firstDay().getByRole('button', { name: 'Dodaj: Pompki przy ścianie', exact: true }).click()
  await firstDay().getByRole('button', { name: 'Dodaj: Pompki klasyczne', exact: true }).click()
  await firstDay().getByRole('button', { name: 'Usuń ćwiczenie 3', exact: true }).click()
  await firstDay().getByRole('group', { name: /^Ćwiczenie 1:/ }).getByLabel('Ciężar (kg)').fill('1,001')
  await page.getByRole('button', { name: 'Zapisz plan', exact: true }).click()
  assert.equal((await api('/api/plans')).length, 0)
  await firstDay().getByRole('group', { name: /^Ćwiczenie 1:/ }).getByLabel('Ciężar (kg)').fill('42,75')
  await firstDay().getByRole('button', { name: 'Ćwiczenie 2: w górę', exact: true }).click()
  assert.equal(await firstDay().getByRole('group', { name: /^Ćwiczenie 2:/ }).getByLabel('Ciężar (kg)').inputValue(), '42,75')
  await firstDay().getByRole('button', { name: 'Ćwiczenie 2: w górę', exact: true }).click()
  await page.getByRole('button', { name: 'Dodaj dzień', exact: true }).click()
  await secondDay().getByLabel('Nazwa dnia').fill('Dzień B')
  await secondDay().getByRole('button', { name: 'Dodaj ćwiczenie', exact: true }).click()
  await secondDay().getByLabel('Szukaj ćwiczenia').fill('Pompki klasyczne')
  await secondDay().getByRole('button', { name: 'Dodaj: Pompki klasyczne', exact: true }).click()
  await secondDay().getByRole('button', { name: 'Zamknij wybór ćwiczeń', exact: true }).click()
  await secondDay().getByRole('button', { name: 'Dzień 2: w górę', exact: true }).click()
  assert.equal(await firstDay().getByLabel('Nazwa dnia').inputValue(), 'Dzień B')
  await firstDay().getByRole('button', { name: 'Dzień 1: w dół', exact: true }).click()
  await screenshot('plan-editor-desktop')
  await page.setViewportSize({ width: 375, height: 812 })
  await screenshot('plan-editor-create-mobile')

  stage = 'failed save preserves the complete draft and successful retry creates one plan'
  await page.route('**/api/plans', (route) => route.request().method() === 'POST' ? route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Testowy błąd zapisu' }) }) : route.continue())
  await page.getByRole('button', { name: 'Zapisz plan', exact: true }).click()
  await page.getByRole('alert').filter({ hasText: 'Testowy błąd zapisu' }).waitFor()
  assert.equal(await page.getByLabel('Nazwa planu', { exact: true }).inputValue(), 'Plan testowy S2-08')
  assert.equal(await firstDay().getByRole('group', { name: /^Ćwiczenie 1:/ }).getByLabel('Ciężar (kg)').inputValue(), '42,75')
  await page.unroute('**/api/plans')
  let releaseSave
  let saves = 0
  const saveGate = new Promise((done) => { releaseSave = done })
  await page.route('**/api/plans', async (route) => { if (route.request().method() === 'POST') { saves++; await saveGate }; await route.continue() })
  await page.getByRole('button', { name: 'Zapisz plan', exact: true }).click()
  await page.getByRole('button', { name: 'Zapisywanie…', exact: true }).waitFor()
  assert.ok(await page.getByRole('button', { name: 'Zapisywanie…', exact: true }).isDisabled())
  releaseSave()
  await page.waitForURL((url) => /^\/plany\/\d+$/.test(url.pathname))
  await page.unroute('**/api/plans')
  assert.equal(saves, 1)
  const plan = await api(`/api/plans/${new URL(page.url()).pathname.split('/').at(-1)}`)
  assert.equal(plan.days.length, 2)
  assert.deepEqual(plan.days[0].exercises.map((entry) => entry.exercise.id), catalog.map((exercise) => exercise.id))
  assert.equal(plan.days[0].exercises[0].weight, 42.75)
  await page.setViewportSize({ width: 1280, height: 900 })
  await trainingNav().getByRole('link', { name: 'Moje plany' }).click()

  stage = 'own plans and actual plan details'
  await page.reload()
  await page.getByText('Liczba planów: 1', { exact: true }).waitFor()
  await screenshot('plans-desktop')
  await page.getByRole('link').filter({ hasText: 'Plan testowy S2-08' }).click()
  await page.waitForURL(`**/plany/${plan.id}`)
  await page.getByRole('heading', { name: 'Plan testowy S2-08' }).waitFor()
  await page.getByText('42,75 kg', { exact: true }).waitFor()
  await page.getByText('0 kg', { exact: true }).first().waitFor()
  await screenshot('plan-details-desktop')

  stage = 'mobile plan and exercise layout'
  await page.setViewportSize({ width: 375, height: 812 })
  await screenshot('plan-details-mobile')
  await trainingNav().getByRole('link', { name: 'Moje plany' }).click()
  await page.getByText('Liczba planów: 1', { exact: true }).waitFor()
  await screenshot('plans-mobile')
  await page.goto(new URL('/cwiczenia?muscleGroup=CORE&type=HOME', app).href)
  await results(2)
  await screenshot('exercises-mobile')

  stage = 'cancel edit preserves the saved plan'
  await page.goto(new URL(`/plany/${plan.id}/edytuj`, app).href)
  await page.getByRole('heading', { name: 'Edytuj plan', exact: true }).waitFor()
  assert.equal(await firstDay().getByRole('group', { name: /^Ćwiczenie 1:/ }).getByLabel('Ciężar (kg)').inputValue(), '42,75')
  await page.getByLabel('Nazwa planu', { exact: true }).fill('Zmiana do odrzucenia')
  page.once('dialog', (dialog) => dialog.dismiss())
  await page.getByRole('button', { name: 'Anuluj', exact: true }).click()
  assert.equal(await page.getByLabel('Nazwa planu', { exact: true }).inputValue(), 'Zmiana do odrzucenia')
  page.once('dialog', (dialog) => dialog.accept())
  await page.getByRole('button', { name: 'Anuluj', exact: true }).click()
  await page.getByRole('heading', { name: 'Plan testowy S2-08', exact: true }).waitFor()
  assert.equal((await api(`/api/plans/${plan.id}`)).name, 'Plan testowy S2-08')

  stage = 'edit existing plan and retain draft after failed PUT'
  await page.getByRole('link', { name: 'Edytuj plan', exact: true }).click()
  await page.getByRole('heading', { name: 'Edytuj plan', exact: true }).waitFor()
  await page.getByLabel('Nazwa planu', { exact: true }).fill('Plan po edycji')
  await page.getByLabel('Opis (opcjonalnie)').fill('')
  await secondDay().getByRole('button', { name: 'Usuń dzień 2', exact: true }).click()
  await firstDay().getByRole('button', { name: 'Ćwiczenie 2: w górę', exact: true }).click()
  await firstDay().getByRole('group', { name: /^Ćwiczenie 1:/ }).getByLabel('Serie', { exact: true }).fill('5')
  await firstDay().getByRole('group', { name: /^Ćwiczenie 1:/ }).getByLabel('Powtórzenia').fill('6')
  await firstDay().getByRole('group', { name: /^Ćwiczenie 1:/ }).getByLabel('Ciężar (kg)').fill('12,50')
  await screenshot('plan-editor-edit-mobile')
  await page.route(`**/api/plans/${plan.id}`, (route) => route.request().method() === 'PUT' ? route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Testowy błąd edycji' }) }) : route.continue())
  await page.getByRole('button', { name: 'Zapisz zmiany', exact: true }).click()
  await page.getByRole('alert').filter({ hasText: 'Testowy błąd edycji' }).waitFor()
  assert.equal(await page.getByLabel('Nazwa planu', { exact: true }).inputValue(), 'Plan po edycji')
  await page.unroute(`**/api/plans/${plan.id}`)
  await page.getByRole('button', { name: 'Zapisz zmiany', exact: true }).click()
  await page.getByRole('heading', { name: 'Plan po edycji', exact: true }).waitFor()
  await page.getByText('12,5 kg', { exact: true }).waitFor()
  const edited = await api(`/api/plans/${plan.id}`)
  assert.equal(edited.days.length, 1)
  assert.equal(edited.description, null)
  assert.deepEqual(edited.days[0].exercises.map((entry) => entry.exercise.id), [...catalog].reverse().map((exercise) => exercise.id))
  assert.equal(edited.days[0].exercises[0].sets, 5)
  assert.equal(edited.days[0].exercises[0].weight, 12.5)
  await page.reload()
  await page.getByRole('heading', { name: 'Plan po edycji', exact: true }).waitFor()
  await screenshot('plan-edited-mobile')

  stage = 'loading and retry after network failure'
  let release
  const gate = new Promise((done) => { release = done })
  await page.route('**/api/exercises*', async (route) => { await gate; await route.continue() })
  await page.goto(new URL('/cwiczenia', app).href)
  await page.getByText('Wczytywanie ćwiczeń…', { exact: true }).waitFor()
  release()
  await results(48)
  await page.unroute('**/api/exercises*')
  await page.route('**/api/exercises*', (route) => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Testowy błąd serwera' }) }))
  await page.reload()
  await page.getByRole('alert').filter({ hasText: 'Testowy błąd serwera' }).waitFor()
  await screenshot('exercises-error-mobile')
  await page.unroute('**/api/exercises*')
  await page.getByRole('button', { name: 'Spróbuj ponownie' }).click()
  await results(48)

  stage = 'plans error and retry'
  await page.route('**/api/plans', (route) => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Nie udało się wczytać planów' }) }))
  await trainingNav().getByRole('link', { name: 'Moje plany' }).click()
  await page.getByRole('alert').filter({ hasText: 'Nie udało się wczytać planów' }).waitFor()
  await page.unroute('**/api/plans')
  await page.getByRole('button', { name: 'Spróbuj ponownie' }).click()
  await page.getByText('Liczba planów: 1', { exact: true }).waitFor()

  stage = 'delete requires confirmation and can be retried without losing the plan'
  await page.goto(new URL(`/plany/${plan.id}`, app).href)
  await page.getByRole('button', { name: 'Usuń plan', exact: true }).click()
  const confirmation = () => page.getByRole('dialog', { name: 'Usunąć plan?' })
  await confirmation().waitFor()
  await screenshot('delete-plan-mobile')
  await confirmation().getByRole('button', { name: 'Anuluj', exact: true }).click()
  assert.equal((await api(`/api/plans/${plan.id}`)).id, plan.id)
  await page.route(`**/api/plans/${plan.id}`, (route) => route.request().method() === 'DELETE' ? route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Testowy błąd usuwania' }) }) : route.continue())
  await page.getByRole('button', { name: 'Usuń plan', exact: true }).click()
  await confirmation().getByRole('button', { name: 'Potwierdź usunięcie', exact: true }).click()
  await confirmation().getByRole('alert').filter({ hasText: 'Testowy błąd usuwania' }).waitFor()
  assert.equal((await api(`/api/plans/${plan.id}`)).id, plan.id)
  await page.unroute(`**/api/plans/${plan.id}`)
  await confirmation().getByRole('button', { name: 'Potwierdź usunięcie', exact: true }).click()
  await page.getByRole('heading', { name: 'Nie masz jeszcze planów' }).waitFor()
  await api(`/api/plans/${plan.id}`, { expected: 404 })

  stage = 'missing plan'
  await page.goto(new URL('/plany/9223372036854775807', app).href)
  await page.getByRole('alert').filter({ hasText: 'Nie znaleziono planu' }).waitFor()

  stage = 'expired session redirect'
  await page.route('**/api/plans', (route) => route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ message: 'Zaloguj się ponownie' }) }))
  await page.getByRole('link', { name: 'Moje plany', exact: true }).last().click()
  await page.waitForURL('**/logowanie')
  assert.equal(await trainingNav().count(), 0)
  assert.deepEqual(errors, [], 'No React runtime errors')
  console.log('PASS: UI registration/activation/login, URL filters/history, create/edit/reorder/delete plans, validation, retained drafts, confirmation/cancel, mobile layout, errors/retry, 404 and 401.')
  console.log(`Screenshots: ${output}`)
} catch (error) {
  console.error(`FAIL at ${stage}: ${error.message}`)
  console.error(`Browser errors: ${JSON.stringify(errors)}`)
  if (page) {
    console.error(`Visible page: ${(await page.locator('body').innerText()).slice(0, 3000)}`)
    await page.screenshot({ path: resolve(output, 'failure.png'), fullPage: true })
  }
  throw error
} finally {
  await browser?.close()
  if (jwt) {
    await api('/api/users/me/deletion-request', { method: 'POST', body: { password } })
    await api('/api/auth/confirm-deletion', { method: 'POST', body: { token: await mailToken('confirm-deletion') }, authenticated: false })
    console.log('PASS: disposable UI account and its plans removed.')
  } else console.error(`Account may require cleanup: ${email}`)
}
