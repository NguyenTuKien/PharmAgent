import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const moduleRoot = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(join(moduleRoot, 'RelationshipsPage.jsx'), 'utf8')

function sourceBetween(startMarker, endMarker) {
  const start = source.indexOf(startMarker)
  const end = source.indexOf(endMarker, start + startMarker.length)

  assert.notEqual(start, -1, `Missing marker: ${startMarker}`)
  assert.notEqual(end, -1, `Missing marker: ${endMarker}`)

  return source.slice(start, end)
}

test('caregiver relationships keeps the page shell mounted while loading', () => {
  const caregiverPage = sourceBetween('function CaregiverRelationshipsPage()', 'function ElderlyRelationshipsPage()')

  assert.doesNotMatch(caregiverPage, /if\s*\(\s*loading\s*\)\s*{\s*return\s+<LoadingState>/)
  assert.match(caregiverPage, /<RelationshipTable[\s\S]*isLoading=\{loading\}/)
})

test('caregiver relationship table exposes only a compact detail action per row', () => {
  const relationshipTable = sourceBetween('function RelationshipTable', 'function InviteDrawer')

  assert.match(relationshipTable, /function RelationshipTable\(\{[^}]*isLoading[^}]*onView/)
  assert.doesNotMatch(relationshipTable, /onChat|onEditLocalProfile|onEditRelation|onDeleteLocalProfile/)
  assert.doesNotMatch(relationshipTable, />\s*(Chat|Gọi|Sửa|Xóa)\s*</)
  assert.doesNotMatch(relationshipTable, /<AppButton[\s\S]*>\s*Quan hệ\s*<\/AppButton>/)
})
