import assert from 'node:assert/strict'
import test from 'node:test'

import { buildRegisterPayload } from './registerPayload.js'

test('builds elderly registration payload with family relation instead of permission level', () => {
  const payload = buildRegisterPayload({
    email: 'Caregiver@Example.com',
    password: 'Password123!',
    includeElderly: true,
    firstName: 'An',
    lastName: 'Nguyen',
    elderlyFirstName: 'Binh',
    elderlyLastName: 'Nguyen',
    elderlyPhone: '0987654321',
    elderlyDateOfBirth: '1948-06-02',
    elderlyGender: 'MALE',
    elderlyAddress: 'Ha Noi',
    relation: 'FATHER',
    customRelation: '',
    permissionLevel: 'VIEW',
  })

  assert.equal(payload.email, 'caregiver@example.com')
  assert.equal(payload.elderly.relation, 'FATHER')
  assert.equal('customRelation' in payload.elderly, false)
  assert.equal('permissionLevel' in payload.elderly, false)
})

test('keeps custom family relation when Other is selected', () => {
  const payload = buildRegisterPayload({
    email: 'caregiver@example.com',
    password: 'Password123!',
    includeElderly: true,
    firstName: 'An',
    elderlyFirstName: 'Lan',
    elderlyLastName: 'Tran',
    elderlyPhone: '0977777777',
    elderlyDateOfBirth: '1952-03-12',
    elderlyGender: 'FEMALE',
    relation: 'OTHER',
    customRelation: 'Dì ruột',
  })

  assert.equal(payload.elderly.relation, 'OTHER')
  assert.equal(payload.elderly.customRelation, 'Dì ruột')
})
