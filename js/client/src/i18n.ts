import { serverContext, useServerContext, type ServerContext } from './context.js';

/**
 * Fills `{0}`, `{1}`, ... in a message from Spring's `messages.properties`, like `java.text.MessageFormat`:
 * numbers are formatted for the locale (`{0}` → `1,234`), `{0,number,integer}` and `{0,number,percent}` work,
 * and quotes follow Java (`''` is one quote, `'{0}'` is literal). Without arguments the message is returned as it
 * is, as Spring does.
 */
export function formatMessage(pattern: string, args: readonly unknown[] = [], locale?: string): string {
  if (args.length === 0) {
    return pattern;
  }
  let out = '';
  let i = 0;
  while (i < pattern.length) {
    const char = pattern[i];
    if (char === "'") {
      if (pattern[i + 1] === "'") {
        out += "'";
        i += 2;
        continue;
      }
      // Quoted text up to the next single quote; '' inside it is a quote.
      i++;
      while (i < pattern.length) {
        if (pattern[i] === "'") {
          if (pattern[i + 1] === "'") {
            out += "'";
            i += 2;
            continue;
          }
          i++;
          break;
        }
        out += pattern[i++];
      }
      continue;
    }
    if (char === '{') {
      const end = pattern.indexOf('}', i);
      if (end > i) {
        const [index, type, style] = pattern
          .slice(i + 1, end)
          .split(',')
          .map((part) => part.trim());
        const n = Number(index);
        if (/^\d+$/.test(index ?? '')) {
          out += n < args.length ? formatArgument(args[n], type, style, locale) : pattern.slice(i, end + 1);
          i = end + 1;
          continue;
        }
      }
    }
    out += char;
    i++;
  }
  return out;
}

function formatArgument(value: unknown, type: string | undefined, style: string | undefined, locale?: string): string {
  if (value === null || value === undefined) {
    return 'null';
  }
  if (typeof value === 'number' || typeof value === 'bigint') {
    if (type === 'number' && style === 'integer') {
      return new Intl.NumberFormat(locale, { maximumFractionDigits: 0 }).format(value);
    }
    if (type === 'number' && style === 'percent') {
      return new Intl.NumberFormat(locale, { style: 'percent' }).format(value);
    }
    return new Intl.NumberFormat(locale).format(value);
  }
  if (value instanceof Date) {
    if (type === 'time') {
      return new Intl.DateTimeFormat(locale, { timeStyle: 'short' }).format(value);
    }
    if (type === 'date') {
      return new Intl.DateTimeFormat(locale, { dateStyle: 'medium' }).format(value);
    }
    return new Intl.DateTimeFormat(locale, { dateStyle: 'short', timeStyle: 'short' }).format(value);
  }
  return String(value);
}

/** Looks up and formats messages of the request locale. */
export interface Messages {
  /**
   * The message for `key` from `mustache-react.context.messages`, with `{0}`, `{1}`, ... filled in.
   * A key that is not exposed is returned as it is, so missing keys are visible on the page.
   */
  t(key: string, ...args: unknown[]): string;
  /** Whether the key is exposed. */
  has(key: string): boolean;
  /** The request locale, e.g. `ko-KR`. */
  locale: string | undefined;
}

/** {@link Messages} for a server context; outside React, `messages().t('app.name')`. */
export function messages(context: ServerContext = serverContext()): Messages {
  const table = context.messages ?? {};
  return {
    t: (key, ...args) => formatMessage(table[key] ?? key, args, context.locale),
    has: (key) => Object.prototype.hasOwnProperty.call(table, key),
    locale: context.locale,
  };
}

/**
 * Messages of the request locale, from Spring's `MessageSource`.
 *
 * ```tsx
 * const { t } = useMessages();
 * return <p>{t('orders.count', orders.length)}</p>;   // orders.count=주문 {0}건
 * ```
 */
export function useMessages(): Messages {
  return messages(useServerContext());
}

/**
 * Link that switches the language (`mustache-react.i18n.param`), keeping the current path and query:
 * `<a href={localeHref('en')}>English</a>`. Spring keeps the choice in a cookie.
 */
export function localeHref(locale: string, context: ServerContext = serverContext(), current?: string): string {
  const param = context.i18n?.param ?? 'lang';
  const here = current ?? (typeof location === 'undefined' ? undefined : location.pathname + location.search + location.hash);
  if (here === undefined) {
    return `?${encodeURIComponent(param)}=${encodeURIComponent(locale)}`;
  }
  const url = new URL(here, 'http://localhost');
  url.searchParams.set(param, locale);
  return url.pathname + url.search + url.hash;
}
