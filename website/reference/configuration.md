# 설정

::: v-pre

모든 설정은 `application.properties` / `application.yml`의 `mustache-react.*`에 있습니다. 전부 기본값이 있어서 대부분의 프로젝트는 아무것도 설정하지 않아도 됩니다.

## 전체 예시

```yaml
mustache-react:
  enabled: true
  ssr: off                              # off | auto | always
  frontend-dir: src/main/frontend
  content-negotiation: true
  manifest-location: classpath:/static/mustache-react/.vite/manifest.json
  public-path: /mustache-react/         # 또는 https://cdn.example.com/shop/
  layout: classpath:/templates/mustache-react/layout.mustache
  template:
    model-attributes: true
    react-name: react
    helpers-name: mr
    html-tags: true
  dev-server:
    enabled: false
    url: http://localhost:5173
    auto-start: true
    node: ""
    node-install-dir: .mustache-react/node
    startup-timeout: 30s
  ssr-server:
    bundle-location: classpath:/META-INF/mustache-react/ssr/server.mjs
    timeout: 3s
    startup-timeout: 15s
  context:
    csrf: true
    user: true
    messages: []                        # app.name, orders.*, *
  i18n:
    param:                              # 예: lang → ?lang=en
    locales: []                         # 예: ko,en
    default-locale:                     # 예: ko
    cookie-name: locale
    cookie-max-age: 365d
```

## 기본

| 키 | 기본값 | 설명 |
|---|---|---|
| `mustache-react.enabled` | `true` | `false`면 자동 설정 전체를 끕니다 |
| `mustache-react.ssr` | `off` | 서버 렌더링. `off`: 브라우저에서만 렌더링. `auto`: 서버에서 렌더링하고 실패하면 브라우저로 넘김(실패는 로그). `always`: 항상 서버에서 렌더링. SSR 번들이 없으면 시작 시 실패하고, 렌더링할 수 없는 요청도 실패 |
| `mustache-react.frontend-dir` | `src/main/frontend` | React 소스 디렉터리(작업 디렉터리 기준). 개발 모드에서 Vite 개발 서버를 띄울 때 씁니다. Gradle 플러그인은 `bootRun`/`run`에 절대 경로를 넘겨줍니다 |
| `mustache-react.content-negotiation` | `true` | `@ReactPage` URL이 `Accept`에 따라 JSON으로 답할지. `false`면 항상 HTML |
| `mustache-react.manifest-location` | `classpath:/static/mustache-react/.vite/manifest.json` | 프론트 빌드가 쓰는 Vite manifest 위치 |
| `mustache-react.public-path` | `/mustache-react/` | 페이지가 빌드된 에셋을 불러오는 위치. 경로면 jar에서 서빙하고, URL(`https://cdn.example.com/shop/`, `//cdn...`)이면 같은 파일을 올려둔 CDN에서 불러옵니다. URL이면 태그에 `crossorigin`이 붙습니다. 빌드는 어느 쪽이든 같습니다. [배포](../guide/deployment) 참고 |
| `mustache-react.layout` | `classpath:/templates/mustache-react/layout.mustache` | `@ReactPage`의 Mustache 레이아웃. 이 리소스가 없으면 내장 레이아웃을 씁니다 |

## `template.*`: Mustache 화면

| 키 | 기본값 | 설명 |
|---|---|---|
| `template.model-attributes` | `true` | 모든 Spring MVC Model에 island 섹션(`react`)과 헬퍼(`mr`)를 넣을지 |
| `template.react-name` | `react` | island 섹션의 Model 이름(`{{#react}}...{{/react}}`). 태그 접두어(`<react:...>`)도 이 이름을 따릅니다 |
| `template.helpers-name` | `mr` | 헬퍼의 Model 이름(`{{{mr.head}}}`, `{{{mr.scripts}}}`, `{{#mr.t}}`, `{{mr.lang}}`) |
| `template.html-tags` | `true` | Mustache 템플릿에서 `<react:StatusBadge status="{{status}}" />`, `<react:head />`, `<react:scripts />` 태그를 쓸 수 있게 할지. 애플리케이션의 `Mustache.Compiler` 빈을 감싸서 동작합니다 |

## `dev-server.*`: 개발 모드

| 키 | 기본값 | 설명 |
|---|---|---|
| `dev-server.enabled` | `false` | 빌드 결과 대신 Vite 개발 서버에서 프론트를 불러옵니다(HMR, 빌드 불필요). 개발 프로파일에서만 켭니다 |
| `dev-server.url` | `http://localhost:5173` | 개발 서버 origin. 여기에 public path가 붙습니다 |
| `dev-server.auto-start` | `true` | 애플리케이션과 함께 개발 서버를 띄우고 끕니다. 그 포트에서 이미 돌고 있는 개발 서버가 있으면 그것을 씁니다 |
| `dev-server.node` | (빈 값) | 개발 서버와 SSR 서버가 쓰는 Node.js 실행 파일. 비우면 `node-install-dir`의 가장 최신 버전, 없으면 PATH의 `node` |
| `dev-server.node-install-dir` | `.mustache-react/node` | Gradle/Maven 플러그인이 Node.js를 내려받는 위치 |
| `dev-server.startup-timeout` | `30s` | 개발 서버 시작 대기 시간 |

`public-path`가 CDN URL이어도 개발 서버는 `/mustache-react/` 경로를 씁니다.

## `ssr-server.*`: 서버 렌더링

`ssr`이 `auto`나 `always`일 때 쓰는 Node.js 서버 설정입니다. Node.js 실행 파일은 `dev-server.node` / `node-install-dir`을 따릅니다.

| 키 | 기본값 | 설명 |
|---|---|---|
| `ssr-server.bundle-location` | `classpath:/META-INF/mustache-react/ssr/server.mjs` | 빌드 플러그인에서 `ssr = true`로 만든 SSR 번들 |
| `ssr-server.timeout` | `3s` | 한 번의 렌더링 제한 시간. 넘기면 그 페이지는 브라우저에서 렌더링합니다(`auto`) |
| `ssr-server.startup-timeout` | `15s` | SSR 서버 시작 대기 시간 |

`dev-server.enabled=true`이면 SSR 서버 대신 Vite 개발 서버가 현재 소스로 렌더링합니다.

## `context.*`: 서버 컨텍스트

React의 `useServerContext()`로 받는 값입니다.

| 키 | 기본값 | 설명 |
|---|---|---|
| `context.csrf` | `true` | CSRF 토큰(`csrf`)을 넣을지. Spring Security가 있을 때만 |
| `context.user` | `true` | 로그인 사용자(`user`: `{name, authorities}`)를 넣을지. Spring Security가 있을 때만 |
| `context.messages` | (없음) | 요청 locale로 내려보낼 메시지 키. 키(`app.name`), 접두어(`orders.*`), 전부(`*`). 적은 것만 HTML에 들어갑니다 |

## `i18n.*`: 언어 전환

| 키 | 기본값 | 설명 |
|---|---|---|
| `i18n.param` | (없음) | 언어를 바꾸는 요청 파라미터. 예: `lang` → `?lang=en`. 선택은 쿠키에 저장됩니다. 비우면 `Accept-Language`만 따릅니다(Spring Boot 기본) |
| `i18n.locales` | (없음) | 제공하는 언어 목록(예: `ko,en`). 언어 메뉴용으로 페이지에 들어갑니다 |
| `i18n.default-locale` | (없음) | 사용자가 고르기 전 언어. 비우면 `Accept-Language`. `param`이 있을 때만 쓰입니다 |
| `i18n.cookie-name` | `locale` | 선택한 언어를 저장하는 쿠키 |
| `i18n.cookie-max-age` | `365d` | 쿠키 유지 기간 |

`i18n.param`을 설정하면 `CookieLocaleResolver`(빈 이름 `localeResolver`)와 `LocaleChangeInterceptor`가 등록됩니다. 애플리케이션이 직접 `localeResolver` 빈을 등록했다면 그것을 그대로 씁니다.

## 함께 알아둘 Spring Boot 설정

| 키 | 왜 |
|---|---|
| `spring.messages.basename` | 메시지 파일 이름(기본 `messages`). `context.messages`의 패턴(`orders.*`)도 이 파일들에서 키를 찾습니다 |
| `spring.messages.encoding` | 메시지 파일 인코딩(기본 UTF-8) |
| `spring.messages.fallback-to-system-locale` | 기본 `true`. `messages.properties`가 한국어라면 `false`로 두세요. 그렇지 않으면 서버 OS가 영어일 때 한국어 요청에 `messages_en.properties`가 나갑니다 |
| `spring.mustache.servlet.content-type` | 지정하지 않으면 mustache-react가 Mustache 뷰에 `text/html;charset=UTF-8`을 붙입니다(한글 깨짐 방지). 지정하면 그 값을 씁니다 |
| `spring.mustache.prefix`, `spring.mustache.servlet.cache` | 개발 중 템플릿을 소스에서 바로 읽으려면 `file:src/main/resources/templates/`, `false` |

## 프로파일 예시

```properties
# application.properties (공통)
mustache-react.ssr=auto
mustache-react.context.messages=app.name,orders.*
spring.messages.fallback-to-system-locale=false

# application-dev.properties
mustache-react.dev-server.enabled=true
spring.mustache.prefix=file:src/main/resources/templates/
spring.mustache.servlet.cache=false

# application-prod.properties (프론트를 CDN에 따로 올릴 때)
mustache-react.public-path=https://cdn.example.com/shop/
```

## 타입 생성기 옵션

`mustache-react-processor`(annotation processor)는 javac 옵션 하나를 읽습니다.

| 옵션 | 설명 |
|---|---|
| `-Amustachereact.typesFile=<경로>` | 생성할 `types.ts` 경로. Gradle 플러그인이 자동으로 `src/main/frontend/generated/types.ts`를 넘깁니다. 없으면 클래스 출력의 `META-INF/mustache-react/types.ts`에 씁니다 |

`@ReactIsland`가 있으면 클래스 출력에 `META-INF/mustache-react/islands.properties`도 씁니다. 런타임이 이 파일로 템플릿의 island props를 검사합니다.

:::
