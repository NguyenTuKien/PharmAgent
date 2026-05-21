import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const layoutRoot = dirname(fileURLToPath(import.meta.url))
const topbarSource = readFileSync(join(layoutRoot, 'Topbar.jsx'), 'utf8')

test('profile dropdown links directly to personal information only', () => {
  assert.doesNotMatch(topbarSource, /Trang chính/)
  assert.doesNotMatch(topbarSource, /Hồ sơ & cài đặt/)
  assert.match(topbarSource, /Thông tin cá nhân/)
  assert.match(topbarSource, /navigate\('\/my-profile'\)/)
})
