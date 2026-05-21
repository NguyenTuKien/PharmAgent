import assert from 'node:assert/strict'
import { test } from 'node:test'

import { finalizeLoginProfiles } from './authFacade.js'

test('finalizeLoginProfiles keeps caregiver and elderly accounts on profile selection', async () => {
  let selectedProfileId = null
  const profiles = [
    { id: 'caregiver-profile', firstName: 'An', role: 'CAREGIVER' },
    { id: 'elderly-profile', firstName: 'Binh', role: 'ELDERLY' },
  ]

  const result = await finalizeLoginProfiles(profiles, async (profileId) => {
    selectedProfileId = profileId
  })

  assert.equal(selectedProfileId, null)
  assert.deepEqual(result, {
    profiles,
    activeProfile: null,
    requiresProfileSelection: true,
    redirectTo: '/profiles',
  })
})

test('finalizeLoginProfiles selects caregiver accounts without elderly profiles', async () => {
  const profiles = [{ id: 'caregiver-profile', firstName: 'An', role: 'CAREGIVER' }]

  const result = await finalizeLoginProfiles(profiles, async (profileId) => ({
    id: profileId,
    firstName: 'An',
    role: 'CAREGIVER',
  }))

  assert.deepEqual(result, {
    profiles,
    activeProfile: {
      id: 'caregiver-profile',
      firstName: 'An',
      role: 'CAREGIVER',
    },
    requiresProfileSelection: false,
    redirectTo: '/dashboard',
  })
})

test('finalizeLoginProfiles selects admin accounts and routes to admin area', async () => {
  const profiles = [{ id: 'admin-profile', firstName: 'Minh', role: 'ADMIN' }]

  const result = await finalizeLoginProfiles(profiles, async (profileId) => ({
    id: profileId,
    firstName: 'Minh',
    role: 'ADMIN',
  }))

  assert.equal(result.requiresProfileSelection, false)
  assert.equal(result.activeProfile.id, 'admin-profile')
  assert.equal(result.redirectTo, '/admin')
})
