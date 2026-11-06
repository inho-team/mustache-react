import { createContext, createElement, useContext, type ReactNode } from 'react';
import { Protocol } from './protocol.js';

/** CSRF token of the request, from Spring Security. */
export interface CsrfToken {
  headerName: string;
  parameterName: string;
  token: string;
}

/** The signed-in user, from Spring Security. */
export interface ServerUser {
  name: string;
  authorities: string[];
}

/** Validation errors of the last form submission, by field. */
export interface ValidationErrors {
  fields: Record<string, string[]>;
  global: string[];
}

/**
 * What the server writes into every page (`mustache-react.context.*`). Applications add their own entries with a
 * `ServerContextContributor` bean; type them by extending this interface.
 */
export interface ServerContext {
  locale?: string;
  csrf?: CsrfToken;
  user?: ServerUser | null;
  /** Messages of the request locale, see `useMessages`. */
  messages?: Record<string, string>;
  /** Language switching (`mustache-react.i18n.*`): the request parameter and the languages on offer. */
  i18n?: { param?: string; locales: string[] };
  errors?: ValidationErrors;
  /** The Java library that rendered the page. */
  mustacheReact?: { version: string };
  [key: string]: unknown;
}

let cached: ServerContext | undefined;

/** Reads the server context from the page (`<script id="mr-context">`). Returns `{}` when there is none. */
export function serverContext(): ServerContext {
  if (cached === undefined) {
    const script = typeof document === 'undefined' ? null : document.getElementById(Protocol.CONTEXT_SCRIPT_ID);
    const text = script?.textContent?.trim();
    cached = text ? (JSON.parse(text) as ServerContext) : {};
  }
  return cached;
}

/** For tests: forget the context read from the page. */
export function resetServerContext(): void {
  cached = undefined;
}

const ServerContextReact = createContext<ServerContext | undefined>(undefined);

/** Provides the server context explicitly, e.g. during server-side rendering where there is no page to read. */
export function ServerContextProvider({ value, children }: { value: ServerContext; children?: ReactNode }) {
  return createElement(ServerContextReact.Provider, { value }, children);
}

/**
 * The server context: `locale`, `csrf`, `user`, `messages`, and anything the application added.
 *
 * ```tsx
 * const { user } = useServerContext();
 * return user ? <span>{user.name}</span> : <a href="/login">Sign in</a>;
 * ```
 */
export function useServerContext<T extends ServerContext = ServerContext>(): T {
  return (useContext(ServerContextReact) ?? serverContext()) as T;
}

/**
 * A message from `mustache-react.context.messages`, or the key itself when it is not exposed.
 * For `{0}` arguments, use `useMessages().t(key, ...args)`.
 */
export function message(key: string, context: ServerContext = serverContext()): string {
  return context.messages?.[key] ?? key;
}

/** Headers that carry the CSRF token, for `fetch` calls Spring Security must accept. */
export function csrfHeaders(context: ServerContext = serverContext()): Record<string, string> {
  return context.csrf ? { [context.csrf.headerName]: context.csrf.token } : {};
}

/**
 * `fetch` for the application's own endpoints: asks for JSON and sends the CSRF token.
 * The same URL that renders a `@ReactPage` returns its data as JSON.
 */
export function springFetch(input: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json');
  }
  for (const [name, value] of Object.entries(csrfHeaders())) {
    if (!headers.has(name)) {
      headers.set(name, value);
    }
  }
  return fetch(input, { credentials: 'same-origin', ...init, headers });
}
