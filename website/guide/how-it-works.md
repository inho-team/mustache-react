# 동작 원리

::: v-pre

이 문서는 요청 하나가 화면이 되기까지의 흐름, 빌드가 만드는 결과물, 개발 모드의 동작을 설명합니다. 사용하는 데 꼭 알아야 하는 내용은 아니지만, 문제가 생겼을 때 어디를 봐야 하는지 알려줍니다.

## 한눈에 보기

```
 브라우저                  Spring (JVM)                                   Node.js (선택)
 ───────                  ────────────                                   ──────────────
 GET /orders  ───────▶  컨트롤러가 DTO 반환
 Accept: text/html        │
                          ▼
                       @ReactPage 처리기
                          │  DTO → JSON (Spring MVC HttpMessageConverter)
                          │  서버 컨텍스트(locale, csrf, user, messages) → JSON
                          ▼
                       레이아웃 렌더링 ──── SSR이 켜져 있으면 ───────▶  server.mjs (127.0.0.1)
                          │                 HTML을 받아 끼워 넣음  ◀──  React → HTML 문자열
                          ▼
 HTML  ◀──────────────  <div data-mr-island="pages/orders/OrderList" ...>
                        <script type="application/json" data-mr-props="...">{...}</script>
                        <script type="application/json" id="mr-context">{...}</script>
                        <script type="module" src="/mustache-react/assets/mustache-react-xxx.js">
   │
   ▼
 엔트리 스크립트가 data-mr-island 요소를 찾아
 해당 컴포넌트 파일만 내려받고
 SSR HTML이 있으면 hydrate, 없으면 render
```

## `@ReactPage` 요청

1. 컨트롤러 메서드가 평소처럼 값을 반환합니다. `ResponseEntity`를 반환하면 상태 코드와 헤더를 쓰고 body를 props로 씁니다.
2. mustache-react의 반환값 처리기가 `Accept` 헤더를 봅니다.
   - 브라우저(`text/html`을 우선하는 요청)면 HTML 페이지를 만듭니다.
   - 그 밖(`application/json`, `*/*`, 헤더 없음)이면 Spring MVC의 `ResponseEntity` 처리기에 넘겨 **기존 JSON 응답**을 그대로 돌려줍니다.
   - 두 응답 모두 `Vary: Accept`가 붙습니다. `mustache-react.content-negotiation=false`면 항상 HTML입니다.
3. props JSON은 Spring MVC가 등록한 `HttpMessageConverter`로 만듭니다. 그래서 Jackson 설정(날짜 형식, `@JsonProperty`, `@JsonIgnore`)이 API JSON과 똑같이 적용됩니다.
4. 레이아웃(기본 레이아웃 또는 `templates/mustache-react/layout.mustache`)을 렌더링합니다. 레이아웃에는 다음이 들어갑니다.
   - 페이지 요소: `<div data-mr-island="pages/..." data-mr-id="mr-page">`
   - props: `<script type="application/json" data-mr-props="mr-page">`
   - 서버 컨텍스트: `<script type="application/json" id="mr-context">`
   - CSS, 미리 받을 JS(`modulepreload`), 엔트리 스크립트
5. `mustache-react.ssr`이 `auto`나 `always`면 렌더링 전에 Node.js에 페이지를 그려 달라고 요청하고, 받은 HTML을 페이지 요소 안에 넣습니다(`data-mr-ssr="true"`). 실패하면 `auto`에서는 비워 두고 브라우저가 그립니다.

폼 처리(`"redirect:/path"`, `@Valid` 에러)는 같은 처리기가 맡습니다. [폼과 서버 컨텍스트](./forms) 참고.

## Mustache island 요청

1. 컨트롤러는 평소처럼 `Model`을 채우고 뷰 이름을 반환합니다. mustache-react가 모든 Model에 `react`(island 섹션)와 `mr`(헬퍼)를 넣어 둡니다.
2. 템플릿을 **처음 컴파일할 때** `<react:StatusBadge status="{{status}}" />` 같은 태그를 `{{#react}}...{{/react}}` 섹션으로 바꿉니다. 이 변환은 애플리케이션의 Mustache 컴파일러를 감싸서 하므로 뷰, 부분 템플릿(`{{> header}}`), 레이아웃 모두에 적용됩니다.
3. 렌더링 중 island를 만나면:
   - 값이 `{{변수}}` 하나뿐인 속성은 Mustache와 같은 규칙(루프 항목 → 바깥 섹션 → Model)으로 찾은 **객체 그대로**, 나머지는 문자열로 props를 만듭니다.
   - `@ReactIsland`로 선언된 island면 props 이름을 검사합니다.
   - `<div data-mr-island="islands/StatusBadge" data-mr-id="mr-0">`와 props 스크립트를 씁니다. SSR이 켜져 있으면 서버 렌더링 HTML도 넣습니다.
   - 이 요청에서 쓴 island 이름을 기억합니다.
4. `<react:scripts />`(`{{{mr.scripts}}}`)는 서버 컨텍스트, **이 페이지에서 쓴 island의 JS만** 미리 받는 `modulepreload`, 엔트리 스크립트를 씁니다. 그래서 마지막 island 뒤에 있어야 합니다.

## 브라우저에서

엔트리 스크립트는 Vite 플러그인이 만든 가상 파일입니다. 하는 일은 이것뿐입니다.

1. `pages/**`, `islands/**`의 모든 컴포넌트를 **필요할 때 내려받는(lazy)** 목록으로 등록합니다. `app.tsx`가 있으면 모든 페이지와 island를 감싸는 컴포넌트로 씁니다.
2. 페이지에서 `data-mr-island` 요소를 모두 찾아, 각각의 컴포넌트 파일을 내려받고 props를 읽어 마운트합니다.
   - `data-mr-ssr="true"`면 `hydrateRoot`(서버 HTML에 이벤트만 연결)
   - 아니면 `createRoot().render()`
3. island 하나가 실패해도 나머지는 계속 마운트됩니다. 실패는 콘솔에 남고, 개발 모드에서는 그 자리에 오류 상자가 보입니다.
4. 서버 컨텍스트의 `mustacheReact.version`과 `@mustache-react/client` 버전이 다르면 콘솔에 경고합니다.

`useServerContext()`, `useMessages()`, `useSpringForm()`은 모두 `<script id="mr-context">`를 읽습니다.

## 빌드

`./gradlew build`(또는 `mvn package`) 한 번에 다음이 일어납니다.

```
compileJava ── mustache-react-processor ──▶ src/main/frontend/generated/types.ts
                                          ──▶ META-INF/mustache-react/islands.properties (@ReactIsland가 있을 때)

mustacheReactInstall ── Node.js 다운로드(.mustache-react/node, SHA-256 검증)
                     ── package.json 없으면 생성
                     ── npm/pnpm/yarn install (package.json, lock 파일이 바뀌었을 때만)

mustacheReactBuild ── mustache-react-vite build
                   ──▶ static/mustache-react/assets/*.js, *.css     (파일 이름에 내용 해시)
                   ──▶ static/mustache-react/.vite/manifest.json    (컴포넌트 → 파일 목록)
                   ──▶ static/mustache-react/mustache-react-build.json (플러그인 버전)
                   ──▶ META-INF/mustache-react/ssr/server.mjs       (ssr = true일 때)

processResources / jar ── 위 결과를 클래스패스에 포함
```

| 클래스패스 위치 | 내용 | 누가 읽나 |
|---|---|---|
| `static/mustache-react/assets/` | 빌드된 JS, CSS, 이미지 | 브라우저 (Spring 정적 리소스, 또는 CDN) |
| `static/mustache-react/.vite/manifest.json` | 컴포넌트별 JS/CSS 파일 이름 | Spring (`mustache-react.manifest-location`) |
| `static/mustache-react/mustache-react-build.json` | 빌드한 플러그인 버전 | Spring (버전 불일치 경고) |
| `META-INF/mustache-react/ssr/server.mjs` | 의존성이 모두 포함된 SSR 번들 | Spring이 임시 디렉터리에 풀어 `node`로 실행 |
| `META-INF/mustache-react/islands.properties` | `@ReactIsland` props 목록 | Spring (템플릿 props 검사) |

Spring은 시작할 때가 아니라 **첫 요청에서** manifest를 읽고, 이후에는 메모리에 둡니다.

빌드된 JS는 서로를 **상대 경로**로 불러옵니다. 그래서 같은 파일을 jar에서 서빙하든 CDN에 올리든 다시 빌드할 필요가 없습니다. [배포](./deployment) 참고.

## 개발 모드

`mustache-react.dev-server.enabled=true`면:

- Spring이 시작할 때 프론트 디렉터리에서 Vite 개발 서버(`mustache-react-vite dev`, 기본 5173 포트)를 띄우고, 종료할 때 함께 끕니다. 이미 그 포트에서 무언가 응답하면 그것을 씁니다.
- 페이지의 스크립트 태그가 빌드된 파일 대신 개발 서버를 가리킵니다. 프론트 빌드가 필요 없습니다.
- 컴포넌트를 고치면 새로고침 없이 바뀝니다(HMR). `src/main/resources/templates`의 `.mustache`를 고치면 브라우저가 새로고침합니다.
- SSR이 켜져 있으면 Node 사이드카 대신 개발 서버가 현재 소스로 렌더링합니다.

:::
