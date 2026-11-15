import test from 'node:test'
import assert from 'node:assert/strict'
import { moveItem, newDraftDay, newDraftEntry, planToDraft, validatePlanDraft } from '../src/lib/plan-draft.ts'

const catalog = [{ id: 7 }, { id: 8 }]
function draft() {
  return { name: ' Plan A ', description: ' Opis ', days: [{ ...newDraftDay(0), exercises: [{ ...newDraftEntry(7), weight: '42,75' }] }] }
}

test('draft builds the API payload with Polish decimal input and no client or entity IDs', () => {
  const result = validatePlanDraft(draft(), catalog)
  assert.deepEqual(result.errors, {})
  assert.deepEqual(result.payload, { name: 'Plan A', description: 'Opis', days: [{ name: 'Dzień 1', exercises: [{ exerciseId: 7, sets: 3, reps: 10, weight: 42.75 }] }] })
})

test('bodyweight and maximum supported decimal are accepted without rounding', () => {
  for (const weight of ['0', '0,00', '99999.99', '99999,99']) {
    const input = draft()
    input.days[0].exercises[0].weight = weight
    assert.ok(validatePlanDraft(input, catalog).payload)
  }
})

test('invalid numeric inputs are not silently coerced or rounded', () => {
  for (const [field, values] of Object.entries({ sets: ['', '0', '-1', '3.5', '1e2', '2147483648'], reps: ['0', '2.5'], weight: ['', '-1', '1.001', '100000', '1e2', '1,2,3'] })) {
    for (const value of values) {
      const input = draft()
      input.days[0].exercises[0][field] = value
      const result = validatePlanDraft(input, catalog)
      assert.equal(result.payload, null)
      assert.ok(result.errors[`days[0].exercises[0].${field}`], `${field}: ${value}`)
    }
  }
})

test('missing names, exercises and oversized collections report usable field paths', () => {
  const input = draft()
  input.name = ' '
  input.days[0].name = ''
  input.days[0].exercises = []
  const result = validatePlanDraft(input, catalog)
  assert.ok(result.errors.name)
  assert.ok(result.errors['days[0].name'])
  assert.ok(result.errors['days[0].exercises'])
  const oversized = draft()
  oversized.days = Array.from({ length: 32 }, () => draft().days[0])
  assert.ok(validatePlanDraft(oversized, catalog).errors.days)
  oversized.days = [draft().days[0]]
  oversized.days[0].exercises = Array.from({ length: 101 }, () => newDraftEntry(7))
  assert.ok(validatePlanDraft(oversized, catalog).errors['days[0].exercises'])
})

test('unknown exercise and overlong text do not produce a request', () => {
  const input = draft()
  input.name = 'x'.repeat(121)
  input.description = 'x'.repeat(2001)
  input.days[0].exercises[0].exerciseId = 999
  const result = validatePlanDraft(input, catalog)
  assert.equal(result.payload, null)
  assert.ok(result.errors.name)
  assert.ok(result.errors.description)
  assert.ok(result.errors['days[0].exercises[0].exerciseId'])
})

test('reordering preserves values and stable form keys without mutating the source', () => {
  const first = { ...newDraftEntry(7), sets: '5', weight: '0' }
  const second = { ...newDraftEntry(8), sets: '2', weight: '12,5' }
  const source = [first, second]
  const moved = moveItem(source, 1, 0)
  assert.deepEqual(source, [first, second])
  assert.deepEqual(moved, [second, first])
  assert.equal(moveItem(source, 0, -1), source)
  assert.equal(moveItem(source, 1, 2), source)
})

test('editing restores all plan values and preserves duplicate exercise entries', () => {
  const plan = { id: 11, name: 'Plan', description: null, createdAt: '2026-11-15T12:00:00Z', days: [{ id: 12, dayIndex: 0, name: 'A', exercises: [
    { id: 13, orderIndex: 0, exercise: { id: 7 }, sets: 4, reps: 8, weight: 0 },
    { id: 14, orderIndex: 1, exercise: { id: 7 }, sets: 2, reps: 12, weight: 12.5 },
  ] }] }
  const input = planToDraft(plan)
  assert.notEqual(input.days[0].exercises[0].key, input.days[0].exercises[1].key)
  const { payload } = validatePlanDraft(input, catalog)
  assert.equal(payload.description, null)
  assert.deepEqual(payload.days[0].exercises, [
    { exerciseId: 7, sets: 4, reps: 8, weight: 0 }, { exerciseId: 7, sets: 2, reps: 12, weight: 12.5 },
  ])
})
