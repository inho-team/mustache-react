// Browser test of a separate frontend deployment: the built assets are served by another server (standing in for a
// CDN) and the application points to it with mustache-react.public-path. Nothing is rebuilt.
//
//   ./gradlew :examples:basic:run --args="--server.port=18080 --mustache-react.public-path=http://localhost:18081/"
//   pnpm e2e:cdn
//
// The "CDN" serves examples/basic/build/mustache-react/resources/static/mustache-react, the same files the jar
// contains, with the CORS header a real CDN needs for module scripts from another origin.
import assert from 'node:assert/strict';
import { createReadStream, existsSync, statSync } from 'node:fs';
import { createServer } from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright';

const baseUrl = process.env.BASE_URL ?? 'http://localhost:18080';
const cdnPort = Number(process.env.CDN_PORT ?? 18081);
const assetsDir = path.resolve(fileURLToPath(import.meta.url), '../../build/mustache-react/resources/static/mustache-react');
const types = { '.js': 'text/javascript', '.css': 'text/css', '.json': 'application/json', '.svg': 'image/svg+xml' };

const served = [];
const cdn = createServer((request, response) => {
  const file = path.join(assetsDir, decodeURIComponent(new URL(request.url, 'http://cdn').pathname));
  response.setHeader('Access-Control-Allow-Origin', '*');
  if (!file.startsWith(assetsDir) || !existsSync(file) || !statSync(file).isFile()) {
    response.writeHead(404).end();
    return;
  }
  served.push(request.url);
  response.writeHead(200, { 'Content-Type': types[path.extname(file)] ?? 'application/octet-stream' });
  createReadStream(file).pipe(response);
});
await new Promise((resolve) => cdn.listen(cdnPort, 'localhost', resolve));

const browser = await chromium.launch();
try {
  const page = await browser.newPage();
  const errors = [];
  const fromApp = [];
  page.on('pageerror', (error) => errors.push(error));
  page.on('console', (message) => message.type() === 'error' && errors.push(new Error(message.text())));
  page.on('response', (response) => response.status() >= 400 && console.log('HTTP', response.status(), response.url()));
  page.on('request', (request) => {
    if (request.url().startsWith(`${baseUrl}/mustache-react/`)) {
      fromApp.push(request.url());
    }
  });

  // A @ReactPage: the entry, the page chunk and its CSS come from the CDN, and the page is interactive.
  const html = await (await fetch(`${baseUrl}/orders`, { headers: { Accept: 'text/html' } })).text();
  await page.goto(`${baseUrl}/orders`);
  assert.match(html, new RegExp(`<script type="module" src="http://localhost:${cdnPort}/assets/[^"]+\\.js" crossorigin>`));
  await page.getByRole('heading', { name: '주문 목록' }).waitFor();
  await page.getByLabel('고객 검색').fill('이서연');
  assert.equal(await page.getByTestId('summary').textContent(), '1 / 4건');

  // Thymeleaf islands, loaded lazily from the CDN by relative imports.
  await page.goto(`${baseUrl}/board`);
  await page.locator('tbody .badge').nth(1).click();
  assert.equal(await page.locator('tbody .badge').nth(1).textContent(), '배송 중 · 주문 #1002');

  assert.deepEqual(errors, []);
  assert.deepEqual(fromApp, [], 'no asset should come from the application server');
  assert.ok(served.some((url) => /OrderList.*\.js$/.test(url)), `page chunk from the CDN: ${served.join(', ')}`);
  assert.ok(served.some((url) => /StatusBadge.*\.js$/.test(url)), `island chunk from the CDN: ${served.join(', ')}`);
  console.log(`CDN e2e test passed: ${served.length} assets from http://localhost:${cdnPort}/`);
} finally {
  await browser.close();
  cdn.close();
}
