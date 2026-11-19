import assert from 'node:assert/strict'
import test from 'node:test'
import { decimal, historyQuery, localDateTime, metricPoints, resultPoints, sessionDraft, toInstant } from '../src/lib/progress.ts'

test('local date/time preserves selected instant including local timezone and rejects invalid dates', () => {
  const date = new Date(2026, 10, 19, 9, 30)
  assert.equal(localDateTime(date), '2026-11-19T09:30')
  assert.equal(toInstant('2026-11-19T09:30'), date.toISOString())
  assert.equal(toInstant('2026-02-30T09:30'), null)
  assert.equal(toInstant('not-a-date'), null)
})

test('history includes full local boundary days while exercise filters stay optional', () => {
  const query = new URLSearchParams(historyQuery('12', '2026-11-01', '2026-11-19').slice(1))
  assert.equal(query.get('exerciseId'), '12')
  assert.equal(query.get('from'), new Date('2026-11-01T00:00:00').toISOString())
  assert.equal(query.get('to'), new Date('2026-11-19T23:59:59.999').toISOString())
  assert.equal(historyQuery('', '', ''), '')
})

test('decimal accepts comma, zero load and exact bounds but rejects exponent and excessive precision', () => {
  assert.equal(decimal('82,25', 0, 2000), 82.25)
  assert.equal(decimal('0', 0, 2000), 0)
  assert.equal(decimal('2000', 0, 2000), 2000)
  for (const value of ['-1', '1e2', '', '1.234', '2000,01', 'NaN', 'Infinity']) assert.equal(decimal(value, 0, 2000), null)
})

test('exercise chart chooses daily maximum for just the selected exercise and sorts dates', () => {
  const rows = [
    { exerciseId: 1, time: new Date(2026, 10, 19, 10).toISOString(), weight: 50 },
    { exerciseId: 1, time: new Date(2026, 10, 18, 12).toISOString(), weight: 40 },
    { exerciseId: 1, time: new Date(2026, 10, 19, 9).toISOString(), weight: 60 },
    { exerciseId: 2, time: new Date(2026, 10, 19, 9).toISOString(), weight: 120 },
  ]
  assert.deepEqual(resultPoints(rows, 1), [{ time: new Date(2026, 10, 18, 12).getTime(), value: 40 }, { time: new Date(2026, 10, 19, 12).getTime(), value: 60 }])
  assert.deepEqual(resultPoints([], 1), [])
})

test('metric chart excludes missing readings without manufacturing zero measurements', () => {
  const rows = [{ time: '2026-11-19T12:00:00Z', weightKg: 80 }, { time: '2026-11-18T12:00:00Z', weightKg: null }, { time: '2026-11-17T12:00:00Z', weightKg: 82 }]
  assert.deepEqual(metricPoints(rows, 'weightKg').map((point) => point.value), [82, 80])
  assert.deepEqual(metricPoints(rows, 'waistCm'), [])
})

test('repeated exercise entries have distinct consecutive set numbers within safe request limits', () => {
  const entry = { exercise: { id: 5, name: 'Przysiad' }, sets: 3, reps: 10, weight: 42.5 }
  const draft = sessionDraft({ exercises: [entry, entry] })
  assert.deepEqual(draft.map((row) => row.setNo), [1, 2, 3, 4, 5, 6])
  assert.equal(draft[0].weight, '42,5')
  const capped = sessionDraft({ exercises: Array.from({ length: 8 }, () => ({ ...entry, sets: 999999999 })) })
  assert.equal(capped.length, 100)
  assert.equal(Math.max(...capped.map((row) => row.setNo)), 100)
})
