import assert from 'node:assert/strict'
import test from 'node:test'

import { buildRefreshTokensPayload } from './authApi.js'

test('builds refresh payload with profile when profile is selected', () => {
  assert.deepEqual(buildRefreshTokensPayload('refresh-token', ' profile-1 '), {
    refreshToken: 'refresh-token',
    profileId: 'profile-1',
  })
})

test('builds refresh payload without profile before profile selection', () => {
  assert.deepEqual(buildRefreshTokensPayload('refresh-token'), {
    refreshToken: 'refresh-token',
  })
})
