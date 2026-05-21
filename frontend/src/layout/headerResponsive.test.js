import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const moduleRoot = dirname(fileURLToPath(import.meta.url))
const sourceRoot = dirname(moduleRoot)
const cssSource = readFileSync(join(sourceRoot, 'index.css'), 'utf8')
const topbarSource = readFileSync(join(moduleRoot, 'Topbar.jsx'), 'utf8')

function blockAfter(marker, length = 4200) {
  const start = cssSource.indexOf(marker)

  assert.notEqual(start, -1, `Missing marker: ${marker}`)

  return cssSource.slice(start, start + length)
}

test('topbar renders separate desktop and fixed mobile role navigation mounts', () => {
  assert.match(topbarSource, /role-nav-wrap role-nav-wrap--desktop/)
  assert.match(topbarSource, /role-nav-wrap role-nav-wrap--mobile/)
})

test('mobile role navigation is docked to the viewport bottom instead of page flow', () => {
  const mobileRules = blockAfter('@media (max-width: 1120px)')

  assert.match(mobileRules, /\.role-nav-wrap--mobile\s*{[\s\S]*position:\s*fixed/)
  assert.match(mobileRules, /\.role-nav-wrap--mobile\s*{[\s\S]*right:\s*0/)
  assert.match(mobileRules, /\.role-nav-wrap--mobile\s*{[\s\S]*bottom:\s*0/)
  assert.match(mobileRules, /\.role-nav-wrap--mobile\s*{[\s\S]*left:\s*0/)
  assert.match(mobileRules, /\.app-main\s*{[\s\S]*min-height:\s*100dvh/)
  assert.match(cssSource, /--bottom-nav-clearance:\s*calc\(98px \+ env\(safe-area-inset-bottom\)\)/)
})

test('mobile gooey search expansion is constrained within the bottom bar', () => {
  const mobileRules = blockAfter('@media (max-width: 1120px)')

  assert.match(
    mobileRules,
    /\.role-nav-wrap--mobile \.header-gooey-search\[data-expanded="true"\] \.gooey-search-tabs-bar\s*{[\s\S]*flex:\s*1 1 auto/,
  )
  assert.match(
    mobileRules,
    /\.role-nav-wrap--mobile \.header-gooey-search\[data-expanded="true"\] \.gooey-search-tabs-input-wrapper\s*{[\s\S]*width:\s*auto !important/,
  )
  assert.match(
    mobileRules,
    /\.role-nav-wrap--mobile \.header-gooey-search \.gooey-search-tabs-right-slot\s*{[\s\S]*flex:\s*1 1 auto/,
  )
  assert.match(
    mobileRules,
    /\.role-nav-wrap--mobile \.header-gooey-search\[data-expanded="true"\] \.gooey-search-tabs-right-slot\s*{[\s\S]*flex:\s*0 0 clamp\(48px, 10vw, 60px\)/,
  )
})
