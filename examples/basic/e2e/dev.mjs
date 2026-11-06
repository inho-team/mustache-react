// Browser test of development mode: pages load modules from the Vite dev server, a component edit is applied in
// place by hot module replacement (React state survives), and a Mustache template edit reloads the page.
//
//   ./gradlew :examples:basic:run --args="--server.port=18080 --spring.profiles.active=dev"
//   BASE_URL=http://localhost:18080 pnpm e2e:dev
//
// The application starts the Vite dev server itself (mustache-react.dev-server.auto-start).
//
// The test edits two source files and restores them when it ends.
import assert from 'node:assert/strict';
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright';

const baseUrl = process.env.BASE_URL ?? 'http://localhost:18080';
const source = (path) => fileURLToPath(new URL(`../src/main/${path}`, import.meta.url));
const page$ = source('frontend/pages/orders/OrderList.tsx');
const template$ = source('resources/templates/dashboard.mustache');
const originals = new Map([page$, template$].map((file) => [file, readFileSync(file, 'utf8')]));

function edit(file, from, to) {
  const text = readFileSync(file, 'utf8');
  assert.ok(text.includes(from), `${file} does not contain ${from}`);
  writeFileSync(file, text.replace(from, to));
}

const browser = await chromium.launch();
try {
  const page = await browser.newPage();
  const errors = [];
  page.on('pageerror', (error) => errors.push(error));

  // 1. The page is served by Spring, its modules by Vite.
  await page.goto(`${baseUrl}/orders`);
  await page.getByRole('heading', { name: '주문 목록' }).waitFor();
  const html = await (await fetch(`${baseUrl}/orders`, { headers: { Accept: 'text/html' } })).text();
  assert.match(html, /http:\/\/localhost:5173\/mustache-react\/@vite\/client/);

  // 2. Editing a component updates it in place: the text typed into the search box is kept.
  await page.getByLabel('고객 검색').fill('이서연');
  await page.evaluate(() => (window.__notReloaded = true));
  edit(page$, "<h1>{t('orders.title')}</h1>", "<h1>{t('orders.title')} (HMR)</h1>");
  await page.getByRole('heading', { name: '주문 목록 (HMR)' }).waitFor({ timeout: 10_000 });
  assert.equal(await page.getByLabel('고객 검색').inputValue(), '이서연');
  assert.equal(await page.evaluate(() => window.__notReloaded), true);

  // 3. Editing a Mustache template reloads the page with the new content.
  await page.goto(`${baseUrl}/dashboard`);
  await page.locator('tbody .badge').first().waitFor();
  edit(template$, '<h1>{{#mr.t}}dashboard.title{{/mr.t}}</h1>', '<h1>{{#mr.t}}dashboard.title{{/mr.t}} (reloaded)</h1>');
  await page.getByRole('heading', { name: '대시보드 (reloaded)' }).waitFor({ timeout: 10_000 });
  await page.locator('tbody .badge').first().waitFor();

  assert.deepEqual(errors, []);
  console.log('dev mode e2e test passed');
} finally {
  for (const [file, text] of originals) {
    writeFileSync(file, text);
  }
  await browser.close();
}
