// @vitest-environment jsdom
import { createElement } from 'react';
import { renderToString } from 'react-dom/server';
import { afterEach, describe, expect, it } from 'vitest';
import { resetServerContext, ServerContextProvider } from './context.js';
import { formatMessage, localeHref, messages, useMessages } from './i18n.js';

afterEach(() => {
  document.body.innerHTML = '';
  resetServerContext();
});

describe('formatMessage', () => {
  it('fills arguments like java.text.MessageFormat', () => {
    expect(formatMessage('주문 {0}건, 합계 {1}원', [3, 1234567], 'ko-KR')).toBe('주문 3건, 합계 1,234,567원');
    expect(formatMessage('{1} before {0}', ['a', 'b'])).toBe('b before a');
    expect(formatMessage('{0,number,integer} / {1,number,percent}', [2.7, 0.25], 'en')).toBe('3 / 25%');
    expect(formatMessage('missing {1}', ['a'])).toBe('missing {1}');
    expect(formatMessage('{0}', [null])).toBe('null');
  });

  it('follows Java quoting', () => {
    expect(formatMessage("It''s {0}", ['ok'])).toBe("It's ok");
    expect(formatMessage("'{0}' is {0}", ['x'])).toBe('{0} is x');
    expect(formatMessage("'it''s' {0}", ['x'])).toBe("it's x");
  });

  it('returns the message unchanged without arguments, as Spring does', () => {
    expect(formatMessage("It''s {0}")).toBe("It''s {0}");
  });
});

describe('messages', () => {
  const context = { locale: 'en', messages: { 'orders.count': '{0} orders', 'app.name': 'Shop' } };

  it('looks up and formats messages, and shows missing keys as they are', () => {
    const { t, has } = messages(context);
    expect(t('orders.count', 1200)).toBe('1,200 orders');
    expect(t('app.name')).toBe('Shop');
    expect(t('no.such.key')).toBe('no.such.key');
    expect(has('app.name')).toBe(true);
    expect(has('no.such.key')).toBe(false);
  });

  it('reads the server context in components', () => {
    function Title() {
      const { t, locale } = useMessages();
      return createElement('h1', { lang: locale }, t('orders.count', 2));
    }
    const html = renderToString(createElement(ServerContextProvider, { value: context }, createElement(Title)));
    expect(html).toBe('<h1 lang="en">2 orders</h1>');
  });
});

describe('localeHref', () => {
  it('sets the language parameter and keeps the path, query and hash', () => {
    const context = { i18n: { param: 'lang', locales: ['ko', 'en'] } };
    expect(localeHref('en', context, '/orders?page=2#top')).toBe('/orders?page=2&lang=en#top');
    expect(localeHref('ko', context, '/orders?lang=en')).toBe('/orders?lang=ko');
  });

  it('uses the current location in the browser', () => {
    window.history.replaceState(null, '', '/orders/1001');
    expect(localeHref('en', { i18n: { param: 'locale', locales: [] } })).toBe('/orders/1001?locale=en');
  });
});
