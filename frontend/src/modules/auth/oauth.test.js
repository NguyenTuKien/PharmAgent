import assert from 'node:assert/strict'
import { test } from 'node:test'

import {
  buildGoogleOAuthStartUrl,
  readGoogleOAuthCallback,
  redirectToGoogleOAuth,
} from './oauth.js'

test('buildGoogleOAuthStartUrl joins configured API base with Google OAuth start endpoint', () => {
  assert.equal(
    buildGoogleOAuthStartUrl('https://app.example.com/api'),
    'https://app.example.com/api/auth/oauth/google/start',
  )

  assert.equal(
    buildGoogleOAuthStartUrl('/api/'),
    '/api/auth/oauth/google/start',
  )
})

test('readGoogleOAuthCallback extracts successful handoff code and error states', () => {
  assert.deepEqual(readGoogleOAuthCallback('?oauth=google&code=handoff-code'), {
    provider: 'google',
    code: 'handoff-code',
    error: '',
  })

  assert.deepEqual(readGoogleOAuthCallback('?oauth=google&error=access_denied'), {
    provider: 'google',
    code: '',
    error: 'access_denied',
  })

  assert.equal(readGoogleOAuthCallback('?code=ignored'), null)
})

test('redirectToGoogleOAuth uses assign so the browser leaves the SPA for backend OAuth', () => {
  let assignedUrl = ''
  const location = {
    assign: (url) => {
      assignedUrl = url
    },
  }

  redirectToGoogleOAuth({ apiBaseUrl: '/api', location })

  assert.equal(assignedUrl, '/api/auth/oauth/google/start')
})
