const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawn } = require('node:child_process');

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const remotePort = 9222;
const baseUrl = process.env.CHECK_BASE_URL || 'http://localhost:5173';
const outputDir = process.env.CHECK_OUTPUT_DIR || process.env.TEMP || process.cwd();
const userDataDir = path.join(os.tmpdir(), `pharmagent-edge-cdp-${process.pid}`);

const session = {
  authToken: 'qa-auth-token',
  refreshToken: 'qa-refresh-token',
  accessToken: 'qa-access-token',
  activeProfile: {
    id: 'qa-elderly-profile',
    email: 'qa@example.com',
    firstName: 'An',
    lastName: 'Nguyen',
    role: 'ELDERLY',
  },
  profiles: [
    {
      id: 'qa-elderly-profile',
      email: 'qa@example.com',
      firstName: 'An',
      lastName: 'Nguyen',
      role: 'ELDERLY',
    },
  ],
};

class CdpClient {
  constructor(url) {
    this.url = url;
    this.nextId = 1;
    this.pending = new Map();
    this.events = new Map();
  }

  async connect() {
    this.ws = new WebSocket(this.url);
    this.ws.addEventListener('message', (event) => {
      const payload = JSON.parse(event.data);
      if (payload.id && this.pending.has(payload.id)) {
        const { resolve, reject } = this.pending.get(payload.id);
        this.pending.delete(payload.id);
        if (payload.error) {
          reject(new Error(payload.error.message));
        } else {
          resolve(payload.result);
        }
        return;
      }

      const listeners = this.events.get(payload.method);
      if (listeners) {
        for (const listener of listeners) listener(payload.params);
      }
    });
    await new Promise((resolve, reject) => {
      this.ws.addEventListener('open', resolve, { once: true });
      this.ws.addEventListener('error', reject, { once: true });
    });
  }

  send(method, params = {}) {
    const id = this.nextId++;
    this.ws.send(JSON.stringify({ id, method, params }));
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
      setTimeout(() => {
        if (this.pending.has(id)) {
          this.pending.delete(id);
          reject(new Error(`CDP command timed out: ${method}`));
        }
      }, 15000);
    });
  }

  on(method, listener) {
    const listeners = this.events.get(method) ?? [];
    listeners.push(listener);
    this.events.set(method, listeners);
  }

  once(method) {
    return new Promise((resolve) => {
      const listener = (params) => {
        const listeners = this.events.get(method) ?? [];
        this.events.set(
          method,
          listeners.filter((item) => item !== listener),
        );
        resolve(params);
      };
      this.on(method, listener);
    });
  }

  close() {
    this.ws?.close();
  }
}

function rectInfo(rect) {
  if (!rect) return null;
  return {
    x: Math.round(rect.x * 10) / 10,
    y: Math.round(rect.y * 10) / 10,
    width: Math.round(rect.width * 10) / 10,
    height: Math.round(rect.height * 10) / 10,
    left: Math.round(rect.left * 10) / 10,
    right: Math.round(rect.right * 10) / 10,
    top: Math.round(rect.top * 10) / 10,
    bottom: Math.round(rect.bottom * 10) / 10,
  };
}

async function waitForJson(url, attempts = 50) {
  let lastError;
  for (let index = 0; index < attempts; index += 1) {
    try {
      const response = await fetch(url);
      if (response.ok) return response.json();
      lastError = new Error(`${response.status} ${response.statusText}`);
    } catch (error) {
      lastError = error;
    }
    await new Promise((resolve) => setTimeout(resolve, 200));
  }
  throw lastError;
}

async function newPage() {
  const response = await fetch(`http://127.0.0.1:${remotePort}/json/new?about:blank`, {
    method: 'PUT',
  });
  if (!response.ok) {
    throw new Error(`Could not create CDP target: ${response.status} ${response.statusText}`);
  }
  const target = await response.json();
  const client = new CdpClient(target.webSocketDebuggerUrl);
  await client.connect();
  return { client, targetId: target.id };
}

async function closeTarget(targetId) {
  await fetch(`http://127.0.0.1:${remotePort}/json/close/${targetId}`).catch(() => {});
}

async function evaluate(client, expression) {
  const result = await client.send('Runtime.evaluate', {
    expression,
    awaitPromise: true,
    returnByValue: true,
  });
  if (result.exceptionDetails) {
    throw new Error(result.exceptionDetails.text);
  }
  return result.result.value;
}

async function waitForSelector(client, selector, timeout = 15000) {
  const encoded = JSON.stringify(selector);
  const expression = `
    new Promise((resolve, reject) => {
      const deadline = Date.now() + ${timeout};
      const tick = () => {
        if (document.querySelector(${encoded})) {
          resolve(true);
          return;
        }
        if (Date.now() > deadline) {
          reject(new Error('Missing selector: ' + ${encoded}));
          return;
        }
        setTimeout(tick, 100);
      };
      tick();
    })
  `;
  await evaluate(client, expression);
}

async function capture(client, filename) {
  const screenshot = await client.send('Page.captureScreenshot', {
    format: 'png',
    captureBeyondViewport: false,
  });
  const filePath = path.join(outputDir, filename);
  fs.writeFileSync(filePath, Buffer.from(screenshot.data, 'base64'));
  return filePath;
}

async function preparePage(viewport) {
  const { client, targetId } = await newPage();
  const consoleMessages = [];
  client.on('Runtime.consoleAPICalled', (params) => {
    if (['error', 'warning'].includes(params.type)) {
      consoleMessages.push(`${params.type}: ${params.args.map((arg) => arg.value ?? arg.description).join(' ')}`);
    }
  });
  client.on('Runtime.exceptionThrown', (params) => {
    consoleMessages.push(`exception: ${params.exceptionDetails.text}`);
  });
  await client.send('Page.enable');
  await client.send('Runtime.enable');
  await client.send('Page.addScriptToEvaluateOnNewDocument', {
    source: `window.localStorage.setItem('pharmagent.session.v1', ${JSON.stringify(JSON.stringify(session))});`,
  });
  await client.send('Emulation.setDeviceMetricsOverride', {
    width: viewport.width,
    height: viewport.height,
    deviceScaleFactor: 1,
    mobile: viewport.width < 700,
  });
  const loadEvent = client.once('Page.loadEventFired');
  await client.send('Page.navigate', { url: `${baseUrl}/dashboard` });
  await loadEvent;
  await waitForSelector(client, '.topbar-main');
  await new Promise((resolve) => setTimeout(resolve, 900));
  return { client, targetId, consoleMessages };
}

async function measureDesktop(client) {
  return evaluate(
    client,
    `(() => {
      const getRect = (selector) => {
        const node = document.querySelector(selector);
        return node ? node.getBoundingClientRect().toJSON() : null;
      };
      const topbar = getRect('.topbar-main');
      const nav = getRect('.role-nav-wrap--desktop .header-gooey-search');
      const brand = getRect('.header-brand');
      const actions = getRect('.header-actions');
      const bar = getRect('.role-nav-wrap--desktop .gooey-search-tabs-bar');
      const trigger = getRect('.role-nav-wrap--desktop .gooey-search-tabs-trigger');
      const activeIndicator = getRect('.role-nav-wrap--desktop .gooey-search-tabs-tab-indicator');
      const tabs = Array.from(document.querySelectorAll('.role-nav-wrap--desktop .role-nav-link')).map((tab) => {
        const rect = tab.getBoundingClientRect();
        const label = tab.querySelector('.role-nav-label');
        const labelRect = label ? label.getBoundingClientRect() : null;
        return {
          text: tab.textContent.trim().replace(/\\s+/g, ' '),
          rect: rect.toJSON(),
          labelRect: labelRect ? labelRect.toJSON() : null,
          scrollWidth: tab.scrollWidth,
          scrollHeight: tab.scrollHeight,
        };
      });
      const topbarCenter = topbar ? topbar.left + topbar.width / 2 : null;
      const navCenter = nav ? nav.left + nav.width / 2 : null;
      return {
        url: location.href,
        topbar,
        nav,
        brand,
        actions,
        bar,
        trigger,
        activeIndicator,
        tabs,
        centerDelta: topbarCenter === null || navCenter === null ? null : Math.abs(topbarCenter - navCenter),
        brandGap: brand && nav ? nav.left - brand.right : null,
        actionsGap: actions && nav ? actions.left - nav.right : null,
        clippedTabs: tabs.filter((tab) => tab.scrollHeight > Math.ceil(tab.rect.height) || tab.scrollWidth > Math.ceil(tab.rect.width)),
        lowLabels: tabs.filter((tab) => tab.labelRect && tab.labelRect.bottom > tab.rect.bottom - 3),
      };
    })()`,
  );
}

async function measureMobile(client) {
  return evaluate(
    client,
    `(() => {
      const getRect = (selector) => {
        const node = document.querySelector(selector);
        return node ? node.getBoundingClientRect().toJSON() : null;
      };
      const desktopWrap = document.querySelector('.role-nav-wrap--desktop');
      const mobileWrap = document.querySelector('.role-nav-wrap--mobile');
      const mobile = getRect('.role-nav-wrap--mobile .header-gooey-search');
      const tabs = Array.from(document.querySelectorAll('.role-nav-wrap--mobile .role-nav-link')).map((tab) => {
        const rect = tab.getBoundingClientRect();
        return {
          text: tab.textContent.trim().replace(/\\s+/g, ' '),
          rect: rect.toJSON(),
          scrollWidth: tab.scrollWidth,
          scrollHeight: tab.scrollHeight,
        };
      });
      return {
        url: location.href,
        desktopDisplay: desktopWrap ? getComputedStyle(desktopWrap).display : null,
        mobileDisplay: mobileWrap ? getComputedStyle(mobileWrap).display : null,
        mobile,
        tabs,
        overflowTabs: tabs.filter((tab) => tab.rect.left < 0 || tab.rect.right > window.innerWidth || tab.scrollWidth > Math.ceil(tab.rect.width)),
      };
    })()`,
  );
}

function simplifyDesktop(desktop) {
  return {
    ...desktop,
    topbar: rectInfo(desktop.topbar),
    nav: rectInfo(desktop.nav),
    brand: rectInfo(desktop.brand),
    actions: rectInfo(desktop.actions),
    bar: rectInfo(desktop.bar),
    trigger: rectInfo(desktop.trigger),
    activeIndicator: rectInfo(desktop.activeIndicator),
    tabs: desktop.tabs.map((tab) => ({
      ...tab,
      rect: rectInfo(tab.rect),
      labelRect: rectInfo(tab.labelRect),
    })),
  };
}

function simplifyMobile(mobile) {
  return {
    ...mobile,
    mobile: rectInfo(mobile.mobile),
    tabs: mobile.tabs.map((tab) => ({
      ...tab,
      rect: rectInfo(tab.rect),
    })),
  };
}

async function run() {
  if (!fs.existsSync(edgePath)) {
    throw new Error(`Edge executable not found: ${edgePath}`);
  }

  const browser = spawn(edgePath, [
    `--remote-debugging-port=${remotePort}`,
    `--user-data-dir=${userDataDir}`,
    '--headless=new',
    '--disable-gpu',
    '--no-first-run',
    '--no-default-browser-check',
  ], {
    stdio: 'ignore',
    windowsHide: true,
  });

  try {
    await waitForJson(`http://127.0.0.1:${remotePort}/json/version`);

    const desktopSession = await preparePage({ width: 1280, height: 760 });
    const desktop = await measureDesktop(desktopSession.client);
    const desktopScreenshot = await capture(desktopSession.client, 'pharmagent-header-desktop.png');
    await evaluate(desktopSession.client, `document.querySelector('.role-nav-wrap--desktop .gooey-search-tabs-trigger').click()`);
    await waitForSelector(desktopSession.client, '.role-nav-wrap--desktop .gooey-search-tabs-input');
    const inputVisible = await evaluate(
      desktopSession.client,
      `(() => {
        const input = document.querySelector('.role-nav-wrap--desktop .gooey-search-tabs-input');
        return Boolean(input && input.offsetParent !== null);
      })()`,
    );
    const searchScreenshot = await capture(desktopSession.client, 'pharmagent-header-search-open.png');
    desktopSession.client.close();
    await closeTarget(desktopSession.targetId);

    const mobileSession = await preparePage({ width: 390, height: 844 });
    const mobile = await measureMobile(mobileSession.client);
    const mobileScreenshot = await capture(mobileSession.client, 'pharmagent-header-mobile.png');
    mobileSession.client.close();
    await closeTarget(mobileSession.targetId);

    const failures = [];
    if (desktop.centerDelta === null || desktop.centerDelta > 12) {
      failures.push(`desktop nav is not centered enough: delta=${desktop.centerDelta}`);
    }
    if (desktop.brandGap === null || desktop.brandGap < 16) {
      failures.push(`desktop nav overlaps or crowds brand: gap=${desktop.brandGap}`);
    }
    if (desktop.actionsGap === null || desktop.actionsGap < 16) {
      failures.push(`desktop nav overlaps or crowds actions: gap=${desktop.actionsGap}`);
    }
    if (desktop.bar && Math.abs(desktop.bar.width - desktop.bar.height) > 1) {
      failures.push(`search bar is not circular: ${desktop.bar.width}x${desktop.bar.height}`);
    }
    if (desktop.clippedTabs.length > 0) {
      failures.push(`desktop tabs are clipped: ${desktop.clippedTabs.map((tab) => tab.text).join(', ')}`);
    }
    if (desktop.lowLabels.length > 0) {
      failures.push(`desktop labels are too low: ${desktop.lowLabels.map((tab) => tab.text).join(', ')}`);
    }
    if (!inputVisible) {
      failures.push('desktop search input did not open after clicking trigger');
    }
    if (mobile.desktopDisplay !== 'none') {
      failures.push(`desktop nav should be hidden on mobile, got display=${mobile.desktopDisplay}`);
    }
    if (mobile.mobileDisplay === 'none' || !mobile.mobile) {
      failures.push('mobile nav is not visible');
    }
    if (mobile.overflowTabs.length > 0) {
      failures.push(`mobile tabs overflow viewport: ${mobile.overflowTabs.map((tab) => tab.text).join(', ')}`);
    }

    console.log(JSON.stringify(
      {
        desktop: simplifyDesktop(desktop),
        mobile: simplifyMobile(mobile),
        screenshots: {
          desktop: desktopScreenshot,
          desktopSearchOpen: searchScreenshot,
          mobile: mobileScreenshot,
        },
        consoleMessages: [...desktopSession.consoleMessages, ...mobileSession.consoleMessages],
        failures,
      },
      null,
      2,
    ));

    if (failures.length > 0) {
      process.exitCode = 1;
    }
  } finally {
    browser.kill();
    fs.rmSync(userDataDir, { recursive: true, force: true });
  }
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});
