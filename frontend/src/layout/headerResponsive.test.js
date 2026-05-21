import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";

const projectRoot = dirname(dirname(fileURLToPath(import.meta.url)));
const topbarSource = readFileSync(join(projectRoot, "layout", "Topbar.jsx"), "utf8");
const cssSource = readFileSync(join(projectRoot, "index.css"), "utf8");

test("topbar exposes a responsive grid shell for brand, search, nav, and actions", () => {
  assert.match(topbarSource, /className="topbar-main"/);
  assert.match(topbarSource, /className="header-utilities"/);
  assert.match(topbarSource, /tabs=\{roleNavigationTabs\}/);
  assert.match(topbarSource, /activeTab=\{activeNavigationPath\}/);
  assert.match(topbarSource, /tabList:\s*'role-nav'/);
  assert.match(topbarSource, /tab:\s*'role-nav-link'/);
  assert.match(topbarSource, /activeTab:\s*'is-active'/);
});

test("header stylesheet defines balanced desktop, tablet, and mobile layouts", () => {
  assert.match(cssSource, /\.topbar-main\s*\{/);
  assert.match(cssSource, /grid-template-columns:\s*minmax\(220px,\s*1fr\)\s*auto\s*minmax\(220px,\s*1fr\)/);
  assert.match(cssSource, /\.role-nav-wrap--desktop\s*\{[\s\S]*justify-self:\s*center/);
  assert.match(cssSource, /\.role-nav-wrap--desktop\s*\{[\s\S]*overflow:\s*visible/);
  assert.match(cssSource, /\.header-gooey-search \.gooey-search-tabs-bar\s*\{[\s\S]*border:\s*1px solid rgb\(184 216 207 \/ 70%\)[\s\S]*border-radius:\s*999px/);
  assert.match(cssSource, /\.header-gooey-search \.gooey-search-tabs-tabs-content\.role-nav\s*\{[\s\S]*gap:\s*0[\s\S]*padding:\s*0/);
  assert.match(cssSource, /\.header-gooey-search \.gooey-search-tabs-tab-indicator\s*\{[\s\S]*inset:\s*0[\s\S]*border-radius:\s*999px/);
  assert.match(cssSource, /\.header-gooey-search \.gooey-search-tabs-tab\.role-nav-link\s*\{[\s\S]*height:\s*60px[\s\S]*font-size:\s*0\.7rem/);
  assert.match(cssSource, /\.role-nav-link\s*\{[\s\S]*height:\s*60px[\s\S]*overflow:\s*hidden[\s\S]*border-radius:\s*999px/);
  assert.match(cssSource, /\.role-nav-link \.gooey-search-tabs-tab-icon svg\s*\{[\s\S]*width:\s*20px[\s\S]*height:\s*20px/);
  assert.match(cssSource, /@media\s*\(max-width:\s*1120px\)/);
  assert.match(cssSource, /grid-template-areas:\s*"brand utilities"/);
  assert.match(cssSource, /\.role-nav-wrap\s*\{[\s\S]*position:\s*fixed/);
  assert.match(cssSource, /grid-template-columns:\s*repeat\(5,\s*minmax\(0,\s*1fr\)\)/);
  assert.match(cssSource, /\.header-search:focus-within/);
  assert.match(cssSource, /@media\s*\(max-width:\s*720px\)/);
  assert.match(cssSource, /@media\s*\(max-width:\s*520px\)/);
  assert.match(cssSource, /\.role-nav-wrap::after/);
});
