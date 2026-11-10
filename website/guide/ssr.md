# 서버 렌더링 (SSR)

기본적으로 React 화면은 브라우저가 JavaScript를 받은 뒤에 그려집니다. 서버 렌더링을 켜면 서버가 컴포넌트를 **HTML로 미리 그려서** 보내고, 브라우저는 그 HTML에 React를 이어 붙입니다(hydration).

- 첫 화면이 빨리 보입니다(JavaScript를 기다리지 않음).
- 검색 엔진, 링크 미리보기가 내용을 읽을 수 있습니다.
- `@ReactPage` 페이지와 Mustache island 모두에 적용됩니다.

## 켜기

::: code-group

```properties [application.properties]
# application.properties
mustache-react.ssr=auto
```

```yaml [application.yml]
# application.yml
mustache-react:
  ssr: auto
```

:::

```kotlin
// build.gradle.kts: 서버용 번들도 함께 빌드
mustacheReact { ssr = true }
```

```xml
<!-- Maven -->
<configuration><ssr>true</ssr></configuration>
```

프론트 의존성에 `@mustache-react/server`가 있어야 합니다. 플러그인이 만들어 주는 `package.json`에는 이미 들어 있습니다.

## 모드

| `mustache-react.ssr` | 동작 |
|---|---|
| `off` (기본) | 브라우저에서만 렌더링. Node.js를 띄우지 않습니다 |
| `auto` | 서버에서 렌더링하고, 실패하면 **그 페이지나 island만** 브라우저 렌더링으로 대신합니다. 실패는 한 번 경고로 남고, 다시 성공하면 "works again"을 남깁니다 |
| `always` | 번들이 없거나 Node.js를 못 띄우면 **시작 실패**, 렌더링 실패는 **요청 실패**(500) |

운영에는 `auto`를 권장합니다. 서버 렌더링이 실패해도 사용자는 화면을 정상으로 봅니다. `always`는 SSR이 반드시 되어야 하는 경우(예: SEO 페이지)에 테스트에서 실패를 잡는 용도입니다.

## 어떻게 동작하나

### 운영

1. 빌드 플러그인이 React와 모든 컴포넌트를 한 파일로 묶은 `server.mjs`를 jar의 `META-INF/mustache-react/ssr/`에 넣습니다.
2. 애플리케이션이 시작할 때 이 파일을 임시 폴더에 풀고, `node server.mjs`를 **`127.0.0.1`의 빈 포트**로 실행합니다. 외부에서는 접근할 수 없습니다.
3. 요청마다 Spring이 컴포넌트 이름, props, 서버 컨텍스트를 HTTP로 보내고 HTML을 받아 페이지에 넣습니다. 서버 컨텍스트(로그인 사용자, 메시지)도 함께 가므로 서버와 브라우저의 결과가 같습니다.
4. 애플리케이션이 종료될 때 Node.js 프로세스도 끕니다.

- 서버에는 **`node` 실행 파일만** 있으면 됩니다. `node_modules`, npm은 필요 없습니다.
- `node`는 `mustache-react.dev-server.node`로 지정하거나, 빌드 플러그인이 내려받은 `.mustache-react/node`, 없으면 `PATH`에서 찾습니다. 운영 서버(Docker 이미지 등)에 Node.js를 설치해 두세요.
- Node.js 프로세스가 죽으면 다음 요청에서 감지해 백그라운드로 다시 띄웁니다(최대 10초에 한 번). 그동안 요청은 브라우저 렌더링으로 처리됩니다.

### 개발

`mustache-react.dev-server.enabled=true`인 개발 모드에서는 별도 Node.js 프로세스 대신 **Vite 개발 서버가 현재 소스로** 렌더링합니다. 코드를 고치면 서버 렌더링 결과에도 바로 반영됩니다. `ssr = true` 빌드가 필요 없습니다.

## 설정

| 설정 | 기본값 | 설명 |
|---|---|---|
| `mustache-react.ssr` | `off` | `off` / `auto` / `always` |
| `mustache-react.ssr-server.timeout` | `3s` | 렌더링 한 번의 제한 시간. 넘으면 실패로 처리(`auto`면 브라우저 렌더링) |
| `mustache-react.ssr-server.startup-timeout` | `15s` | Node.js 프로세스가 뜨기를 기다리는 시간 |
| `mustache-react.ssr-server.bundle-location` | `classpath:/META-INF/mustache-react/ssr/server.mjs` | 번들 위치 |

## 확인하는 방법

서버에서 렌더링된 island와 페이지에는 `data-mr-ssr="true"`가 붙고, 안에 HTML이 들어 있습니다.

```bash
curl -s -H 'Accept: text/html' http://localhost:8080/orders | grep -o 'data-mr-ssr="true"'
```

브라우저의 "페이지 소스 보기"에서 내용이 보이면 서버 렌더링이 된 것입니다. 빈 `<div data-mr-island=...></div>`만 있으면 브라우저 렌더링입니다. 시작 로그의 `SSR server ready on 127.0.0.1:...`도 확인하세요.

## 서버에서 깨지지 않는 컴포넌트 만들기

서버 렌더링은 Node.js에서 **컴포넌트 함수를 한 번 실행해 HTML을 만드는 것**입니다. 그래서 다음이 지켜져야 합니다.

| 하지 말 것 | 대신 |
|---|---|
| 렌더링 중에 `window`, `document`, `localStorage` 사용 | `useEffect` 안에서 사용 (서버에서는 실행되지 않음) |
| 렌더링 결과가 서버와 브라우저에서 다름 (`Date.now()`, `Math.random()`, 브라우저 언어) | 값은 props나 서버 컨텍스트로 받기 |
| 모듈을 불러올 때 바로 `window`를 쓰는 라이브러리를 최상단에서 import | 그 컴포넌트만 `useEffect`에서 동적 import하거나, 해당 island는 SSR 실패 시 브라우저 렌더링에 맡기기 |

```tsx
function Width() {
  const [width, setWidth] = useState<number | null>(null);
  useEffect(() => setWidth(window.innerWidth), []);   // 브라우저에서만
  return <span>{width ?? '-'}</span>;
}
```

`typeof window === 'undefined'`로 분기해서 **다른 내용을 그리면** hydration 불일치 경고가 납니다. 처음 렌더링은 같게 하고, 브라우저 전용 내용은 `useEffect` 뒤에 그리세요.

지도, 에디터, 일부 차트처럼 브라우저를 전제로 하는 라이브러리는 `auto` 모드에서 그 island만 브라우저 렌더링으로 넘어가므로 화면은 정상입니다. 경고 로그가 거슬리면 위 방법으로 고치세요.

## 성능

- 렌더링마다 Spring → Node.js로 로컬 HTTP 요청이 하나 생깁니다. 보통 수 밀리초입니다.
- 한 화면의 island마다 따로 렌더링합니다. island가 매우 많은 화면(수백 개)은 `off`가 나을 수 있습니다.
- 서버 렌더링은 첫 화면을 빠르게 할 뿐, 브라우저가 받는 JavaScript 양은 같습니다.
