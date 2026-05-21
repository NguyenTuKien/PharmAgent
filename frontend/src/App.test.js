import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const projectRoot = dirname(fileURLToPath(import.meta.url))
const appSource = readFileSync(join(projectRoot, 'App.jsx'), 'utf8')

test('profile settings route is exposed at my profile instead of settings', () => {
  assert.match(appSource, /path="\/my-profile"/)
  assert.doesNotMatch(appSource, /path="\/settings"/)
})
