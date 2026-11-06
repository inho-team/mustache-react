# 서버 렌더링 (SSR)

```properties
mustache-react.ssr=auto
```

```kotlin
mustacheReact { ssr = true }   // Maven: <configuration><ssr>true</ssr></configuration>
```

프론트 의존성에 `@mustache-react/server`를 추가합니다.

## 동작

- 페이지와 Mustache island를 서버에서 HTML로 렌더링하고 브라우저가 hydrate합니다.
- **운영**: 빌드 플러그인이 React와 컴포넌트를 모두 포함한 `server.mjs` 하나를 jar에 넣습니다. 애플리케이션이 이를 `127.0.0.1`의 빈 포트에서 Node.js로 실행하고 종료할 때 끕니다. 서버에는 `node`만 있으면 됩니다(`node_modules` 불필요).
- **개발**: Vite dev server가 현재 소스로 렌더링합니다.
- 서버 컨텍스트(`useServerContext`)도 서버 렌더링에 그대로 전달되어 hydration이 일치합니다.
- Node.js 프로세스가 죽으면 다음 요청에서 감지해 백그라운드로 다시 띄웁니다(최대 10초에 한 번). 그동안은 브라우저 렌더링으로 대신합니다.

## 모드

| 값 | 동작 |
|---|---|
| `off` (기본) | 브라우저에서만 렌더링 |
| `auto` | 서버에서 렌더링, 실패하면 그 페이지나 island만 브라우저에서 렌더링. 실패는 한 번 기록됩니다 |
| `always` | 번들이 없으면 시작 실패, 렌더링 실패는 요청 실패 |

## 주의

- 컴포넌트가 렌더링 중에 `window`, `document`를 쓰면 서버에서 실패합니다. 이런 코드는 `useEffect` 안으로 옮깁니다(`auto`에서는 그 island만 브라우저 렌더링으로 대신합니다).
- 렌더링 시간 제한은 `mustache-react.ssr-server.timeout`(기본 3초)입니다.
