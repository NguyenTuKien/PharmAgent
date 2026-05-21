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
  assert.match(topbarSource, /className="role-nav-label"/);
});

test("header stylesheet defines balanced desktop, tablet, and mobile layouts", () => {
  assert.match(cssSource, /\.topbar-main\s*\{/);
  assert.match(cssSource, /grid-template-columns:\s*minmax\(190px,\s*max-content\)\s*minmax\(56px,\s*336px\)\s*minmax\(360px,\s*1fr\)\s*auto/);
  assert.match(cssSource, /@media\s*\(max-width:\s*1120px\)/);
  assert.match(cssSource, /grid-template-areas:\s*"brand utilities"/);
  assert.match(cssSource, /\.role-nav-wrap\s*\{[\s\S]*position:\s*fixed/);
  assert.match(cssSource, /grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/);
  assert.match(cssSource, /\.header-search:focus-within/);
  assert.match(cssSource, /@media\s*\(max-width:\s*720px\)/);
  assert.match(cssSource, /@media\s*\(max-width:\s*520px\)/);
  assert.match(cssSource, /\.role-nav-wrap::after/);
});
