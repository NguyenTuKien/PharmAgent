import assert from 'node:assert/strict'
import { test } from 'node:test'

import { buildRegisterPayload, normalizeAuthEmail } from './registerPayload.js'

test('normalizeAuthEmail trims and lowercases email addresses', () => {
  assert.equal(normalizeAuthEmail('  CareGiver@Example.COM  '), 'caregiver@example.com')
})

test('buildRegisterPayload creates backend signup payload with caregiver profile only by default', () => {
  const payload = buildRegisterPayload({
    email: 'CareGiver@Example.COM',
    password: 'Password123!',
    firstName: 'An',
    lastName: 'Nguyen',
    phone: '0912345678',
    dateOfBirth: '1990-01-01',
    gender: 'MALE',
    address: '',
  })

  assert.deepEqual(payload, {
    email: 'caregiver@example.com',
    password: 'Password123!',
    confirmPassword: 'Password123!',
    caregiver: {
      firstName: 'An',
      lastName: 'Nguyen',
      phone: '0912345678',
      dateOfBirth: '1990-01-01',
      gender: 'MALE',
    },
  })
})

test('buildRegisterPayload includes elderly profile when explicitly provided', () => {
  const payload = buildRegisterPayload({
    email: 'caregiver@example.com',
    password: 'Password123!',
    confirmPassword: 'Password123!',
    firstName: 'An',
    lastName: 'Nguyen',
    phone: '0912345678',
    dateOfBirth: '1990-01-01',
    gender: 'MALE',
    includeElderly: true,
    elderlyFirstName: 'Binh',
    elderlyLastName: 'Tran',
    elderlyPhone: '0987654321',
    elderlyDateOfBirth: '1950-01-01',
    elderlyGender: 'FEMALE',
    elderlyAddress: 'Da Nang',
    caregiverTitle: 'Con',
    elderlyTitle: 'Me',
    permissionLevel: 'MANAGE_ALL',
  })

  assert.deepEqual(payload.elderly, {
    firstName: 'Binh',
    lastName: 'Tran',
    phone: '0987654321',
    dateOfBirth: '1950-01-01',
    gender: 'FEMALE',
    address: 'Da Nang',
    caregiverTitle: 'Con',
    elderlyTitle: 'Me',
    permissionLevel: 'MANAGE_ALL',
  })
})
