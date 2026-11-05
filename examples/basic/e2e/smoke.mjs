// Browser smoke test for the example app.
//
//   ./gradlew :examples:basic:run --args=--server.port=18080   (in another terminal)
//   BASE_URL=http://localhost:18080 pnpm e2e
//
// Checks that /orders is rendered by React in a real browser, that the page is interactive, and that the same
// URL still answers with JSON for API clients.
import assert from 'node:assert/strict';
import { chromium } from 'playwright';

const baseUrl = process.env.BASE_URL ?? 'http://localhost:18080';

const browser = await chromium.launch();
try {
  const page = await browser.newPage();
  const errors = [];
  page.on('pageerror', (error) => errors.push(error));
  page.on('console', (message) => message.type() === 'error' && errors.push(new Error(message.text())));

  await page.goto(`${baseUrl}/orders`);
  await page.getByRole('heading', { name: '주문 목록' }).waitFor();

  assert.equal(await page.title(), '주문 목록');
  assert.equal(await page.getByTestId('summary').textContent(), '4 / 4건');
  assert.equal(await page.locator('tbody tr').count(), 4);
  // Text that looks like markup is shown as text, not executed.
  await page.getByText('</script><script>alert(1)</script>').waitFor();

  await page.getByLabel('고객 검색').fill('이서연');
  assert.equal(await page.getByTestId('summary').textContent(), '1 / 4건');
  assert.equal(await page.locator('tbody tr').count(), 1);

  assert.deepEqual(errors, []);

  const api = await fetch(`${baseUrl}/orders`, { headers: { Accept: 'application/json' } });
  assert.match(api.headers.get('content-type') ?? '', /^application\/json/);
  assert.equal((await api.json()).total, 4);

  console.log('e2e smoke test passed');
} finally {
  await browser.close();
}
