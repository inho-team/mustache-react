# JavaScript API

npm 패키지는 세 개이고, 모두 Java artifact와 같은 버전으로 나갑니다.

| 패키지 | 어디에 | 쓰는 곳 |
|---|---|---|
| `@mustache-react/client` | `dependencies` | 컴포넌트 코드(`useServerContext`, `useMessages`, `useSpringForm` 등) |
| `@mustache-react/vite-plugin` | `devDependencies` | 빌드. Gradle/Maven 플러그인이 `mustache-react-vite` CLI로 부릅니다 |
| `@mustache-react/server` | `dependencies` | SSR 번들. 직접 쓸 일은 거의 없습니다 |

## `@mustache-react/client`

### 서버 컨텍스트

```ts
interface ServerContext {
  locale?: string;                                   // 'ko-KR'
  csrf?: CsrfToken;                                  // Spring Security
  user?: ServerUser | null;                          // Spring Security, 비로그인 null
  messages?: Record<string, string>;                 // mustache-react.context.messages
  i18n?: { param?: string; locales: string[] };      // mustache-react.i18n.*
  errors?: ValidationErrors;                         // 직전 폼 제출의 검증 에러
  mustacheReact?: { version: string };               // 서버 라이브러리 버전
  [key: string]: unknown;                            // ServerContextContributor로 추가한 항목
}
interface CsrfToken { headerName: string; parameterName: string; token: string }
interface ServerUser { name: string; authorities: string[] }
interface ValidationErrors { fields: Record<string, string[]>; global: string[] }
```

서버가 모든 페이지에 `<script type="application/json" id="mr-context">`로 넣는 값입니다. 직접 추가한 항목은 인터페이스를 확장해서 타입을 붙입니다.

```ts
interface MyContext extends ServerContext { tenant: string }
const { tenant } = useServerContext<MyContext>();
```

#### `useServerContext()`

```ts
function useServerContext<T extends ServerContext = ServerContext>(): T
```

컴포넌트에서 서버 컨텍스트를 읽습니다. `ServerContextProvider`가 있으면 그 값을, 없으면 페이지의 `mr-context` 스크립트를 씁니다.

```tsx
const { user, csrf } = useServerContext();
return user ? <span>{user.name}</span> : <a href="/login">로그인</a>;
```

#### `serverContext()`, `resetServerContext()`

```ts
function serverContext(): ServerContext
function resetServerContext(): void
```

React 밖에서 페이지의 서버 컨텍스트를 읽습니다. 처음 읽은 값을 캐시하고, 페이지에 없으면 `{}`입니다. `resetServerContext()`는 캐시를 지웁니다(테스트용).

#### `ServerContextProvider`

```ts
function ServerContextProvider(props: { value: ServerContext; children?: ReactNode }): ReactElement
```

서버 컨텍스트를 직접 넣습니다. SSR(`@mustache-react/server`)이 내부적으로 쓰고, 컴포넌트 테스트에서도 씁니다.

```tsx
render(<ServerContextProvider value={{ locale: 'ko', user: null }}><Header /></ServerContextProvider>);
```

#### `csrfHeaders()`

```ts
function csrfHeaders(context?: ServerContext): Record<string, string>
```

CSRF 토큰 헤더(`{'X-CSRF-TOKEN': '...'}`)를 줍니다. 토큰이 없으면 `{}`입니다.

#### `springFetch()`

```ts
function springFetch(input: string, init?: RequestInit): Promise<Response>
```

애플리케이션 자신의 엔드포인트를 부르는 `fetch`입니다. 다음을 자동으로 붙입니다(이미 지정한 헤더는 그대로 둡니다).
- `Accept: application/json`: 같은 `@ReactPage` URL이 HTML 대신 JSON으로 답합니다.
- CSRF 토큰 헤더
- `credentials: 'same-origin'`

```ts
const response = await springFetch('/orders?page=2');
const { orders } = await response.json();
await springFetch('/orders/1001/cancel', { method: 'POST' });
```

### 다국어

#### `useMessages()`, `messages()`

```ts
interface Messages {
  t(key: string, ...args: unknown[]): string;
  has(key: string): boolean;
  locale: string | undefined;
}
function useMessages(): Messages
function messages(context?: ServerContext): Messages
```

`mustache-react.context.messages`로 내려온 메시지를 읽습니다. `t`는 `{0}`, `{1}`을 채우고, 내려오지 않은 키는 키 그대로 돌려줍니다. React 밖에서는 `messages().t(...)`를 씁니다.

```tsx
const { t } = useMessages();
<p>{t('orders.summary', visible.length, total)}</p>   // orders.summary={0} / {1}건
```

#### `formatMessage()`

```ts
function formatMessage(pattern: string, args?: readonly unknown[], locale?: string): string
```

Java `MessageFormat`처럼 메시지를 채웁니다.
- 숫자는 locale에 맞게 포맷합니다(`1234` → `1,234`). `{0,number,integer}`, `{0,number,percent}`도 됩니다.
- `Date`는 `{0}`, `{0,date}`, `{0,time}`을 씁니다.
- `null`은 `"null"`입니다.
- 따옴표 규칙도 Java와 같습니다. `''`는 작은따옴표 하나이고, `'{0}'`는 글자 그대로입니다.
- 인자가 없으면 패턴을 그대로 돌려줍니다(Spring과 같음).

`choice` 형식은 지원하지 않습니다.

#### `localeHref()`

```ts
function localeHref(locale: string, context?: ServerContext, current?: string): string
```

언어를 바꾸는 링크입니다. 현재 경로와 쿼리를 유지하고 `mustache-react.i18n.param`(없으면 `lang`)만 바꿉니다.

```tsx
const { i18n } = useServerContext();
{i18n?.locales.map((lang) => <a key={lang} href={localeHref(lang)}>{lang}</a>)}
```

#### `message()`

```ts
function message(key: string, context?: ServerContext): string
```

인자 없이 메시지 하나를 읽습니다. 내려오지 않은 키는 키 그대로입니다. 인자가 필요하면 `useMessages().t`를 씁니다.

### 폼

#### `useSpringForm()`

```ts
function useSpringForm<T extends object>(options: SpringFormOptions<T>): SpringForm<T>

interface SpringFormOptions<T> {
  initialValues: T;                       // 보통 페이지 props의 폼 객체
  action?: string;                        // 기본: 현재 페이지 URL
  encoding?: 'json' | 'form';             // 기본 'json'(@RequestBody), 'form'은 @ModelAttribute
  method?: 'POST' | 'PUT' | 'PATCH' | 'DELETE';   // 기본 'POST'
  onSuccess?: (body: unknown) => void;    // redirect 없이 성공했을 때 응답 JSON
  navigate?: (location: string) => void;  // redirect: 처리. 기본 window.location.assign
}

interface SpringForm<T> {
  values: T;
  setValue<K extends keyof T>(name: K, value: T[K]): void;
  field(name: keyof T & string): FieldProps;   // <input {...form.field('customer')} />
  errors: Record<string, string[]>;            // 필드별 @Valid 에러
  globalErrors: string[];                      // BindingResult.reject(...)
  submitting: boolean;
  submit(event?: FormEvent): Promise<void>;    // <form onSubmit={form.submit}>
}

interface FieldProps {
  name: string;
  value: string;
  onChange: (event: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => void;
  'aria-invalid'?: boolean;                    // 에러가 있는 필드만 true
}
```

Spring MVC 폼 핸들러용 상태입니다.
- **전송:** CSRF 토큰과 함께 값을 보냅니다.
- **응답 처리:** 400 `{"errors"}`는 필드 옆에 표시하고, `{"redirect"}`면 이동합니다. 그 밖의 오류 응답은 예외로 던집니다.
- **숫자 필드:** `initialValues`에서 숫자였던 필드나 `type="number"` 입력은 숫자로 유지되고, 빈 값은 `null`입니다.
- **서버가 다시 그린 페이지:** JavaScript 없는 일반 폼 전송 뒤처럼 서버가 에러와 함께 페이지를 다시 그렸으면, 그 에러(`serverContext().errors`)를 처음부터 보여줍니다.

```tsx
const form = useSpringForm<OrderForm>({ initialValues: props.form, action: '/orders/new' });
return (
  <form onSubmit={form.submit}>
    <input {...form.field('customer')} />
    {form.errors.customer?.map((e) => <p key={e}>{e}</p>)}
    <button disabled={form.submitting}>등록</button>
  </form>
);
```

#### `toFormData()`

```ts
function toFormData(values: object): URLSearchParams
```

`encoding: 'form'`이 쓰는 변환입니다.
- 중첩 객체는 `address.city`, 객체 목록은 `items[0].name`이 됩니다.
- 값 목록은 같은 이름을 반복합니다(`tags=a&tags=b`).
- `null`은 빈 값, `Date`는 ISO 문자열입니다.

### 버전

```ts
const VERSION: string   // 예: '0.1.0'
```

이 패키지의 버전입니다. 서버 라이브러리 버전(`mustacheReact.version`)과 다르면 페이지를 열 때 콘솔에 경고가 나옵니다.

### 내부용

아래는 생성된 진입 코드와 SSR 서버가 쓰는 것입니다. 애플리케이션 코드에서 쓸 일은 없고, 바뀔 수 있습니다.

| 이름 | 역할 |
|---|---|
| `bootstrap(loaders, options?)` | 페이지의 island(`[data-mr-island]`)를 찾아 마운트합니다. `data-mr-ssr="true"`면 hydrate, 아니면 새로 렌더링합니다. 한 island가 실패해도 나머지는 계속 마운트되고, `dev`면 실패한 자리에 에러를 보여줍니다. `options`: `wrapper`(app.tsx), `root`, `dev` |
| `readProps(element, scope?)` | island의 props 스크립트를 읽습니다 |
| `toIslandName(path)` | `/pages/orders/OrderList.tsx` → `pages/orders/OrderList` |
| `Protocol` | 서버와 공유하는 HTML 표식: `data-mr-island`, `data-mr-id`, `data-mr-ssr`, `data-mr-props`, `mr-context` |
| 타입 `BootstrapOptions`, `ComponentLoaders`, `ComponentModule` | `bootstrap`의 인자 타입 |

## `@mustache-react/vite-plugin`

### `mustacheReact(options?)`

`vite.config.ts`가 없으면 CLI가 React 플러그인과 이 플러그인만으로 설정을 만듭니다. 직접 `vite.config.ts`를 두려면 `mustacheReact()`를 넣어야 합니다.

```ts
// vite.config.ts (선택)
import react from '@vitejs/plugin-react';
import { mustacheReact } from '@mustache-react/vite-plugin';

export default { plugins: [react(), mustacheReact()] };
```

| 옵션 | 환경 변수 | 기본값 | 설명 |
|---|---|---|---|
| `outDir` | `MUSTACHE_REACT_OUT_DIR` | `../../../build/mustache-react/static/mustache-react` | 빌드 결과 위치. Spring이 이 디렉터리를 public path로 서빙합니다 |
| `base` | `MUSTACHE_REACT_BASE` | `/mustache-react/` | 개발 서버의 에셋 경로(`mustache-react.public-path`와 같아야 함). 빌드는 항상 상대 경로(`./`)라서 jar와 CDN 어디서나 동작합니다 |
| `port` | `MUSTACHE_REACT_DEV_PORT` | `5173` | 개발 서버 포트(`mustache-react.dev-server.url`과 같아야 함) |
| `ssrOutDir` | `MUSTACHE_REACT_SSR_OUT_DIR` | `../../../build/mustache-react/ssr` | `build --ssr`이 `server.mjs`를 쓰는 위치 |
| `templatesDir` | `MUSTACHE_REACT_TEMPLATES_DIR` | `../resources/templates` | 개발 중 감시할 Mustache 템플릿. 바뀌면 브라우저를 새로고침합니다. `false`면 끕니다 |

플러그인이 하는 일:
- **진입 코드 생성:** `pages/**`와 `islands/**`의 모든 `.tsx`/`.jsx`를 지연 로딩 컴포넌트로 등록하고, `app.tsx`가 있으면 래퍼로 씁니다.
- **빌드 결과:** manifest와 `mustache-react-build.json`(빌드한 플러그인 버전)을 씁니다.
- **SSR:** `server.mjs` 하나로 번들합니다(node_modules 불필요).
- **개발 서버:** `POST /__mustache-react/render`로 현재 소스를 서버 렌더링합니다.

### CLI: `mustache-react-vite`

```bash
mustache-react-vite build [--ssr] [--out-dir <dir>] [--base <path>] [--ssr-out-dir <dir>]
mustache-react-vite dev [--port <n>] [--base <path>] [--templates-dir <dir>]
```

프론트 디렉터리에서 실행합니다. Gradle/Maven 플러그인과 Spring의 개발 서버 자동 시작이 이것을 부르므로 보통 직접 쓸 일은 없습니다. 옵션은 위 환경 변수로도 줄 수 있습니다(`--name=value` 형식도 됩니다).

## `@mustache-react/server`

SSR 번들(`server.mjs`) 안에서 쓰입니다. 플러그인이 진입 코드를 만들므로 애플리케이션이 직접 부를 일은 없습니다.

| 이름 | 역할 |
|---|---|
| `createRenderer({ components, wrapper })` | `{component, props, context}`를 HTML로 렌더링하는 함수를 만듭니다. 브라우저와 같은 트리(래퍼 > 컴포넌트)에 `ServerContextProvider`를 더합니다 |
| `startServer(render, port, host = '127.0.0.1', token = process.env.MUSTACHE_REACT_SSR_TOKEN)` | `POST /__mustache-react/render` HTTP 서버. 토큰이 있으면 `X-Mustache-React-Token` 헤더가 같은 요청만 받습니다(아니면 403) |
| `handleRender`, `readBody`, `portFromArgs`, `RENDER_PATH` | 위 서버의 부품. 개발 서버 미들웨어도 같은 `handleRender`를 씁니다 |

렌더 요청은 `{component, props, context}`, 응답은 `{html}`(실패 시 500과 `{error}`)입니다.
