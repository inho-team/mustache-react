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
  page.on('console', (message) => {
    // Chrome logs every 4xx response; the 400 with @Valid errors is the expected answer to an invalid form.
    const expected = /Failed to load resource: the server responded with a status of 400/.test(message.text());
    if (message.type() === 'error' && !expected) {
      errors.push(new Error(message.text()));
    }
  });

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

  // Mustache page with React islands.
  await page.goto(`${baseUrl}/dashboard`);
  await page.getByRole('heading', { name: '대시보드' }).waitFor();
  await page.locator('[data-testid="order-chart"] .recharts-bar-rectangle').first().waitFor();
  assert.equal(await page.locator('[data-testid="order-chart"] .recharts-bar-rectangle').count(), 3);
  const badges = page.locator('tbody .badge');
  await badges.first().waitFor();
  assert.equal(await badges.count(), 4);
  assert.equal(await badges.nth(1).textContent(), '배송 중');
  await badges.nth(1).click();
  assert.equal(await badges.nth(1).textContent(), '배송 중 · 주문 #1002');
  // Server-rendered Mustache content stays in place.
  assert.equal(await page.locator('tbody tr').nth(0).locator('td').nth(1).textContent(), '김민수');

  assert.deepEqual(errors, []);

  // Sign in through the React login page (Spring Security form login, CSRF token from the server context).
  await page.goto(`${baseUrl}/orders/new`);
  await page.getByRole('heading', { name: '로그인' }).waitFor();
  await page.getByLabel('비밀번호').fill('password');
  await page.getByRole('button', { name: '로그인' }).click();
  await page.getByRole('heading', { name: '주문 등록' }).waitFor();
  assert.equal(await page.getByTestId('user').textContent(), 'kim님');

  // Server-side validation errors appear next to the fields.
  await page.getByRole('button', { name: '등록' }).click();
  await page.getByTestId('customer-error').waitFor();
  assert.equal(await page.getByTestId('customer-error').textContent(), '고객 이름을 입력하세요.');
  assert.equal(await page.getByTestId('amount-error').textContent(), '금액은 1원 이상이어야 합니다.');

  // A valid order is saved; "redirect:/orders" brings the browser to the list.
  await page.getByLabel('고객').fill('최유나');
  await page.getByLabel('금액').fill('45000');
  await page.getByRole('button', { name: '등록' }).click();
  await page.waitForURL(`${baseUrl}/orders`);
  await page.getByText('최유나').waitFor();
  assert.equal(await page.getByTestId('summary').textContent(), '5 / 5건');
  assert.equal(await page.getByTestId('user').textContent(), 'kim님');

  assert.deepEqual(errors, []);

  const api = await fetch(`${baseUrl}/orders`, { headers: { Accept: 'application/json' } });
  assert.match(api.headers.get('content-type') ?? '', /^application\/json/);
  assert.equal((await api.json()).total, 5);

  console.log('e2e smoke test passed');
} finally {
  await browser.close();
}
