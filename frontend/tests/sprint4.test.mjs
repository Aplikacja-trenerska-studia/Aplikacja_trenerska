import assert from 'node:assert/strict'
import test from 'node:test'
import { caloriesQuery, macroShares, oneRepMaxQuery } from '../src/lib/calculators.ts'
import { addDays, groupByDate, inMonth, isoDate, monthGrid, monthKey, monthParam, parseIsoDate, relativeDay, shiftMonth, workoutLink } from '../src/lib/schedule.ts'
import { chartScale, summaryPath, volumeChange, weeksParam } from '../src/lib/stats.ts'
import { isShareToken, shareUrl } from '../src/lib/sharing.ts'
import { loginReturnPath } from '../src/lib/return-path.ts'

const token = 'A'.repeat(20) + '_-' + 'b'.repeat(21)

test('1RM form accepts comma decimals and rejects zero load or too many repetitions', () => {
  assert.deepEqual(oneRepMaxQuery('82,5', '5'), { query: 'weight=82.5&reps=5', error: null })
  assert.equal(oneRepMaxQuery('2000', '1').error, null)
  for (const [weight, reps] of [['0', '5'], ['-5', '5'], ['2000,01', '5'], ['1.001', '5'], ['80', '0'], ['80', '21'], ['80', '5.5'], ['', '']]) {
    assert.equal(oneRepMaxQuery(weight, reps).query, null)
    assert.ok(oneRepMaxQuery(weight, reps).error)
  }
})

test('calorie form validates ranges and keeps selected enums', () => {
  const form = { sex: 'FEMALE', age: '25', heightCm: '165', weightKg: '60,5', activity: 'LIGHT', goal: 'LOSE' }
  assert.equal(caloriesQuery(form).query, 'sex=FEMALE&age=25&heightCm=165&weightKg=60.5&activity=LIGHT&goal=LOSE')
  for (const change of [{ age: '14' }, { age: '101' }, { heightCm: '99' }, { heightCm: '251' }, { weightKg: '29,9' }, { weightKg: '300,5' }, { age: '2e1' }]) {
    assert.equal(caloriesQuery({ ...form, ...change }).query, null)
  }
})

test('macro shares always sum to one hundred percent', () => {
  assert.deepEqual(macroShares({ proteinKcal: 512, fatKcal: 693, carbsKcal: 1556 }), { protein: 19, fat: 25, carbs: 56 })
  assert.deepEqual(macroShares({ proteinKcal: 0, fatKcal: 0, carbsKcal: 0 }), { protein: 0, fat: 0, carbs: 0 })
})

test('calendar dates stay local and reject impossible days', () => {
  assert.equal(isoDate(new Date(2026, 11, 3)), '2026-12-03')
  assert.equal(parseIsoDate('2026-02-30'), null)
  assert.equal(parseIsoDate('2026-2-3'), null)
  assert.equal(addDays('2026-12-31', 1), '2027-01-01')
  assert.equal(addDays('2026-03-01', -1), '2026-02-28')
})

test('month grid starts on Monday, ends on Sunday and covers the whole month', () => {
  const december = monthGrid({ year: 2026, month: 12 })
  assert.equal(december[0], '2026-11-30')
  assert.equal(december.at(-1), '2027-01-03')
  assert.equal(december.length % 7, 0)
  assert.ok(december.includes('2026-12-31'))
  const february = monthGrid({ year: 2027, month: 2 })
  assert.equal(february[0], '2027-02-01')
  assert.equal(february.length, 28)
  assert.ok(inMonth('2026-12-15', { year: 2026, month: 12 }))
  assert.ok(!inMonth('2026-11-30', { year: 2026, month: 12 }))
})

test('month navigation and URL parameter handle year boundaries and invalid input', () => {
  assert.deepEqual(shiftMonth({ year: 2026, month: 12 }, 1), { year: 2027, month: 1 })
  assert.deepEqual(shiftMonth({ year: 2026, month: 1 }, -1), { year: 2025, month: 12 })
  assert.equal(monthKey({ year: 2026, month: 3 }), '2026-03')
  const today = new Date(2026, 10, 23)
  assert.deepEqual(monthParam('2026-12', today), { year: 2026, month: 12 })
  for (const value of [null, '', '2026-13', '1999-05', 'abc']) assert.deepEqual(monthParam(value, today), { year: 2026, month: 11 })
})

test('schedule entries are grouped by day and link to the workout form', () => {
  const entries = [{ id: 1, date: '2026-12-01' }, { id: 2, date: '2026-12-02' }, { id: 3, date: '2026-12-01' }]
  assert.deepEqual(groupByDate(entries).get('2026-12-01').map((entry) => entry.id), [1, 3])
  assert.equal(workoutLink({ id: 7, planId: 3, planDayId: 9 }), '/trening?planId=3&dayId=9&scheduleId=7')
  assert.equal(workoutLink({ id: 7, planId: 3, planDayId: null }), '/trening?planId=3&scheduleId=7')
  assert.equal(workoutLink({ id: 7, planId: null, planDayId: null }), null)
  assert.equal(relativeDay('2026-12-01', '2026-12-01'), 'Dziś')
  assert.equal(relativeDay('2026-12-02', '2026-12-01'), 'Jutro')
  assert.equal(relativeDay('2026-11-30', '2026-12-01'), 'Wczoraj')
})

test('statistics helpers use allowed ranges, encode the time zone and compare weeks', () => {
  assert.equal(weeksParam('26'), 26)
  assert.equal(weeksParam('13'), 12)
  assert.equal(weeksParam(null), 12)
  assert.equal(summaryPath(4, 'Europe/Warsaw'), '/api/stats/summary?weeks=4&zone=Europe%2FWarsaw')
  assert.equal(volumeChange([{ volume: 1000 }, { volume: 1250 }]), 25)
  assert.equal(volumeChange([{ volume: 0 }, { volume: 1250 }]), null)
  assert.equal(volumeChange([{ volume: 1250 }]), null)
  assert.equal(chartScale([830, 900]), 1000)
  assert.equal(chartScale([12000]), 20000)
  assert.equal(chartScale([0]), 1)
})

test('share links accept only generated tokens and are allowed as login return paths', () => {
  assert.ok(isShareToken(token))
  for (const value of [undefined, '', 'short', token + 'x', token.slice(0, 42) + '.']) assert.ok(!isShareToken(value))
  assert.equal(shareUrl(token, 'https://aplikacja-trenerska.vercel.app/'), `https://aplikacja-trenerska.vercel.app/udostepnione/${token}`)
  for (const path of ['/kalendarz?miesiac=2026-12', '/statystyki?tygodnie=26', '/kalkulatory', `/udostepnione/${token}`]) assert.equal(loginReturnPath(path), path)
  assert.equal(loginReturnPath('/udostepnione/short'), '/profil')
})
