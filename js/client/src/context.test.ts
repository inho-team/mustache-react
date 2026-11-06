// @vitest-environment jsdom
import { createElement } from 'react';
import { renderToString } from 'react-dom/server';
import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  csrfHeaders,
  message,
  resetServerContext,
  serverContext,
  ServerContextProvider,
  springFetch,
  useServerContext,
} from './context.js';

function page(context: unknown): void {
  document.body.innerHTML = `<script type="application/json" id="mr-context">${JSON.stringify(context)}</script>`;
  resetServerContext();
}

afterEach(() => {
  document.body.innerHTML = '';
  resetServerContext();
  vi.unstubAllGlobals();
});

describe('server context', () => {
  it('reads the context written by the server', () => {
    page({ locale: 'ko-KR', user: { name: 'kim', authorities: [] }, messages: { 'app.title': '주문' } });

    expect(serverContext().user?.name).toBe('kim');
    expect(message('app.title')).toBe('주문');
    expect(message('missing.key')).toBe('missing.key');
  });

  it('is empty without a context script', () => {
    expect(serverContext()).toEqual({});
    expect(csrfHeaders()).toEqual({});
  });

  it('lets a provider supply the context, as during server-side rendering', () => {
    const Name = () => createElement('span', null, useServerContext().user?.name ?? 'anonymous');

    expect(renderToString(createElement(Name))).toBe('<span>anonymous</span>');
    expect(
      renderToString(createElement(ServerContextProvider, { value: { user: { name: 'lee', authorities: [] } } }, createElement(Name))),
    ).toBe('<span>lee</span>');
  });

  it('springFetch asks for JSON and sends the CSRF token', async () => {
    page({ csrf: { headerName: 'X-CSRF-TOKEN', parameterName: '_csrf', token: 't0k' } });
    const fetchMock = vi.fn(async () => new Response('{}'));
    vi.stubGlobal('fetch', fetchMock);

    await springFetch('/orders', { method: 'POST' });

    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    const headers = new Headers(init.headers);
    expect(url).toBe('/orders');
    expect(headers.get('Accept')).toBe('application/json');
    expect(headers.get('X-CSRF-TOKEN')).toBe('t0k');
    expect(init.credentials).toBe('same-origin');
  });
});
