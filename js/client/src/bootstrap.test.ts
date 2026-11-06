// @vitest-environment jsdom
import { createElement, type ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { bootstrap, readProps, resetVersionCheck, toIslandName } from './bootstrap.js';
import { resetServerContext } from './context.js';
import { VERSION } from './version.js';

const tick = () => new Promise((resolve) => setTimeout(resolve, 0));

function page(html: string): void {
  document.body.innerHTML = html;
}

afterEach(() => {
  document.body.innerHTML = '';
  vi.restoreAllMocks();
});

describe('toIslandName', () => {
  it('strips the leading slash and the extension', () => {
    expect(toIslandName('/pages/orders/OrderList.tsx')).toBe('pages/orders/OrderList');
    expect(toIslandName('./islands/Chart.jsx')).toBe('islands/Chart');
  });
});

describe('readProps', () => {
  it('parses the JSON script linked by id, including escaped characters', () => {
    page(
      '<div data-mr-island="pages/A" data-mr-id="mr-0"></div>' +
        '<script type="application/json" data-mr-props="mr-0">{"text":"\\u003c/script\\u003e"}</script>',
    );
    expect(readProps(document.querySelector('[data-mr-island]')!)).toEqual({ text: '</script>' });
  });

  it('returns an empty object when there is no props script', () => {
    page('<div data-mr-island="pages/A" data-mr-id="mr-0"></div>');
    expect(readProps(document.querySelector('[data-mr-island]')!)).toEqual({});
  });
});

describe('version check', () => {
  it('warns once when the server runs another mustache-react version', async () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    page('<script type="application/json" id="mr-context">{"mustacheReact":{"version":"9.9.9"}}</script>');
    resetServerContext();
    resetVersionCheck();

    await bootstrap({});
    await bootstrap({});

    expect(warn).toHaveBeenCalledOnce();
    expect(String(warn.mock.calls[0]![0])).toContain(`mustache-react 9.9.9 but the page uses @mustache-react/client ${VERSION}`);
  });

  it('stays quiet when the versions match', async () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    page(`<script type="application/json" id="mr-context">{"mustacheReact":{"version":"${VERSION}"}}</script>`);
    resetServerContext();
    resetVersionCheck();

    await bootstrap({});

    expect(warn).not.toHaveBeenCalled();
  });
});

describe('bootstrap', () => {
  it('renders the component with the server props inside the wrapper', async () => {
    page(
      '<div data-mr-island="pages/orders/OrderList" data-mr-id="mr-0"></div>' +
        '<script type="application/json" data-mr-props="mr-0">{"count":3}</script>',
    );
    const OrderList = ({ count }: { count: number }) => createElement('p', null, `orders: ${count}`);
    const Wrapper = ({ children }: { children?: ReactNode }) => createElement('main', null, children);

    await bootstrap({ '/pages/orders/OrderList.tsx': async () => ({ default: OrderList }) }, { wrapper: Wrapper });
    await tick();

    expect(document.querySelector('[data-mr-island]')!.innerHTML).toBe('<main><p>orders: 3</p></main>');
  });

  it('keeps mounting other islands when one fails', async () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    page('<div data-mr-island="pages/Missing"></div><div data-mr-island="pages/Ok"></div>');

    await bootstrap({ '/pages/Ok.tsx': async () => ({ default: () => createElement('span', null, 'ok') }) });
    await tick();

    expect(document.querySelector('[data-mr-island="pages/Ok"]')!.textContent).toBe('ok');
    expect(error).toHaveBeenCalledOnce();
    expect(String(error.mock.calls[0]![1])).toContain('No component file for "pages/Missing"');
    expect(document.querySelector('[data-mr-error]')).toBeNull();
  });

  it('shows mount errors in place in development', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    page('<div data-mr-island="islands/Missing"></div>');

    await bootstrap({}, { dev: true });

    const box = document.querySelector('[data-mr-island="islands/Missing"] [data-mr-error]');
    expect(box?.textContent).toContain('"islands/Missing" failed to mount');
    expect(box?.textContent).toContain('No component file for "islands/Missing"');
  });
});
