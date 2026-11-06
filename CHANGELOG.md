# Changelog

All notable changes are listed here. Versions follow [Semantic Versioning](https://semver.org/); Java artifacts,
the Gradle plugin and npm packages share one version.

## [Unreleased]

First release: 0.1.0.

### Spring
- `@ReactPage` renders a handler's return value as a React page; the same URL answers JSON to API clients. A
  returned `ResponseEntity` sets the status and headers of both.
- `{{#react}}Name a b x=a.b{{/react}}` places React islands in Mustache views; `{{{mr.head}}}` / `{{{mr.scripts}}}`.
- Server context in every page: locale, CSRF token and signed-in user (Spring Security 5.7–7), chosen messages,
  and `ServerContextContributor` beans.
- Forms: `@Valid` errors (400 with field errors for JSON requests) and `return "redirect:/path"`.
- Server-side rendering (`mustache-react.ssr=auto|always`) with a Node.js sidecar running the bundle from the jar,
  or the Vite dev server in development; falls back to browser rendering per page or island, and restarts the
  sidecar when its process dies.
- Warnings when the Java artifacts, `@mustache-react/client` or `@mustache-react/vite-plugin` have different versions.
- Development mode: the application starts the Vite dev server; HMR for components, reload for Mustache templates.
- Supports Java 8+, Spring Boot 2.7, 3.5 and 4.x (Spring MVC).

### Type generation
- `mustache-react-processor` writes TypeScript types for `@ReactPage` props and `@ReactType` classes, following
  Jackson's output (records, Lombok, generics, `@JsonProperty`, `@JsonIgnore`, `@Nullable`).

### Build plugins
- Gradle plugin `run.iknow.mustache-react` (Gradle 7.6+, configuration cache) and Maven plugin
  `mustache-react-maven-plugin`: download Node.js, install the frontend dependencies, build the frontend and the
  SSR bundle into the jar, wire the type generator. Tested on Linux and Windows.

### npm
- `@mustache-react/client` (bootstrap, `useServerContext`, `useSpringForm`, `springFetch`),
  `@mustache-react/server` (SSR renderer and sidecar), `@mustache-react/vite-plugin` (Vite plugin and
  `mustache-react-vite` CLI).
