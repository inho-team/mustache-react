# Changelog

All notable changes are listed here. Versions follow [Semantic Versioning](https://semver.org/); Java artifacts,
the Gradle plugin and npm packages share one version.

## [Unreleased]

First release: 0.1.0.

### Spring
- `@ReactPage` renders a handler's return value as a React page; the same URL answers JSON to API clients. A
  returned `ResponseEntity` sets the status and headers of both.
- Mustache views place React islands as HTML tags, `<react:StatusBadge status="{{status}}" order-id="{{id}}" />`
  (a lone `{{path}}` passes the model object, other values are text), or as `{{#react}}Name a x=a.b{{/react}}`
  sections; `<react:head />` / `<react:scripts />` (or `{{{mr.head}}}` / `{{{mr.scripts}}}`).
- Thymeleaf views: `<react:OrderChart orders="${orders}" currency="KRW" />` places an island with props from
  Thymeleaf expressions (objects, not strings), inside `th:each` / `th:if`; `<react:head />`, `<react:scripts />`.
  Registered automatically when Thymeleaf is on the classpath.
- Server context in every page: locale, CSRF token and signed-in user (Spring Security 5.7–7), chosen messages,
  and `ServerContextContributor` beans.
- i18n: messages from `messages.properties` in React (`useMessages().t('orders.count', n)`, MessageFormat-style
  arguments), in Mustache (`{{#mr.t}}key arg{{/mr.t}}`, `{{mr.lang}}`) and as `@ReactPage` titles; key patterns
  such as `orders.*`; `?lang=en` switching kept in a cookie (`mustache-react.i18n.*`); Mustache views always get
  `charset=UTF-8`, so Korean pages are no longer sent as ISO-8859-1.
- Forms: `useSpringForm` posts JSON (`@RequestBody`) or form fields (`encoding: 'form'`, `@ModelAttribute`, nested
  names such as `address.city`); `@Valid` errors (400 with field errors for JSON requests) and `return "redirect:/path"`.
- Server-side rendering (`mustache-react.ssr=auto|always`) with a Node.js sidecar running the bundle from the jar,
  or the Vite dev server in development; falls back to browser rendering per page or island, and restarts the
  sidecar when its process dies.
- Warnings when the Java artifacts, `@mustache-react/client` or `@mustache-react/vite-plugin` have different versions.
- Deployment: the frontend is built once with relative URLs; pages load it from the jar (default) or, with
  `mustache-react.public-path=https://cdn.example.com/app/`, from a CDN where the same files were uploaded
  (`crossorigin` tags, no rebuild).
- Development mode: the application starts the Vite dev server; HMR for components, reload for Mustache templates.
- Supports Java 8+, Spring Boot 2.7, 3.5 and 4.x (Spring MVC).

### Type generation
- `@ReactIsland("StatusBadge")` on a props class writes `IslandProps<'StatusBadge'>`, and templates that pass an
  unknown prop or leave out a required one fail with a message naming the island and its props.
- `mustache-react-processor` writes TypeScript types for `@ReactPage` props and `@ReactType` classes, following
  Jackson's output (records, Lombok, generics, `@JsonProperty`, `@JsonIgnore`, `@Nullable`).

### Build plugins
- A project without `src/main/frontend/package.json` gets one on the first build, with `tsconfig.json`,
  `.gitignore` and empty `pages/` and `islands/`; existing files are never changed.
- Gradle plugin `run.iknow.mustache-react` (Gradle 7.6+, configuration cache) and Maven plugin
  `mustache-react-maven-plugin`: download Node.js, install the frontend dependencies, build the frontend and the
  SSR bundle into the jar, wire the type generator. Tested on Linux and Windows.

### npm
- `@mustache-react/client` (bootstrap, `useServerContext`, `useSpringForm`, `springFetch`),
  `@mustache-react/server` (SSR renderer and sidecar), `@mustache-react/vite-plugin` (Vite plugin and
  `mustache-react-vite` CLI).
