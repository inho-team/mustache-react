# 문제 해결

::: v-pre

자주 만나는 오류와 해결 방법입니다. 오류 메시지는 로그나 화면에 나오는 영문 그대로 적었습니다. 검색(<kbd>Ctrl</kbd>+<kbd>K</kbd>)으로 메시지 일부를 찾아보세요.

## 빌드

### `mustache-react: ... node_modules/@mustache-react/vite-plugin/dist/bin.js not found`

프론트 의존성이 설치되지 않았거나 `package.json`에 `@mustache-react/vite-plugin`이 없습니다.

- `package.json`의 `devDependencies`에 `@mustache-react/vite-plugin`이 있는지 확인합니다.
- 설치를 끄지 않았는지 확인합니다(Gradle `install = false`, Maven `<install>false</install>`).
- `package.json`이 없던 프로젝트는 첫 빌드에서 플러그인이 만들어 줍니다.

### `Checksum mismatch for node-v...` / `GET https://nodejs.org/dist/... returned HTTP ...`

Node.js 다운로드가 실패했거나 받은 파일이 손상됐습니다. 빌드 서버가 `nodejs.org`에 접근할 수 있는지 확인하고, 사내 미러를 쓴다면 `nodeDistributionUrl`을 바꿉니다. PATH의 Node.js를 쓰려면 `nodeDownload = false`로 둡니다.

### `No Node.js download for the operating system ...` / `... for the CPU ...`

공식 Node.js 배포판이 없는 OS·CPU입니다. Node.js를 직접 설치하고 `nodeDownload = false`로 둡니다.

### `@vitejs/plugin-react is not installed in ... Add it to devDependencies.`

`vite.config.ts` 없이 빌드할 때 기본 설정이 쓰는 패키지입니다. `devDependencies`에 추가합니다.

### `The Vite manifest has no 'mustache-react' entry. Is @mustache-react/vite-plugin listed in vite.config.ts?`

`vite.config.ts`를 직접 만들었는데 `mustacheReact()` 플러그인을 넣지 않았습니다. 이 파일이 있으면 기본 설정 대신 이 파일만 쓰므로 `plugins: [react(), mustacheReact(), ...]`가 모두 있어야 합니다.

### 프론트 코드를 고쳤는데 빌드 결과가 그대로다

`mustacheReactBuild`는 `src/main/frontend`의 파일(그리고 lock 파일)을 기준으로 다시 빌드할지 정합니다. `node_modules` 안을 직접 고친 경우처럼 소스 밖의 변화는 감지하지 못합니다. `./gradlew mustacheReactBuild --rerun-tasks`로 강제로 다시 빌드합니다.

## 실행

### `mustache-react: Vite manifest not found at 'classpath:/static/mustache-react/.vite/manifest.json'`

프론트가 빌드되지 않은 상태로 실행했습니다.

- `-PmustacheReact.skipFrontend`(Maven `-DmustacheReact.skipFrontend`)로 실행하지 않았는지 확인합니다.
- 개발 중이라면 개발 모드(`mustache-react.dev-server.enabled=true`)로 실행하면 빌드가 필요 없습니다.
- 테스트라면 `mustache-react.manifest-location`으로 테스트용 manifest를 지정합니다. [테스트](./testing) 참고.

### `mustache-react: the frontend was built with @mustache-react/vite-plugin X but the application runs mustache-react Y`

Java 라이브러리와 npm 패키지 버전이 다릅니다. 브라우저 콘솔에도 같은 경고가 나옵니다. 둘은 항상 같은 버전으로 올려야 합니다.

```kotlin
implementation("run.iknow:mustache-react-spring-boot-starter:0.1.1")
```

```json
"@mustache-react/client": "0.1.1",
"@mustache-react/server": "0.1.1",
"@mustache-react/vite-plugin": "0.1.1"
```

### `Props must be a JSON object, but ... was written as [...]`

`@ReactPage` 메서드가 리스트나 문자열, 숫자를 반환했습니다. props는 객체여야 합니다. DTO로 감싸서 반환합니다.

```java
public OrderListResponse list() { return new OrderListResponse(orders.findAll()); }   // List<Order> 대신
```

### `No HttpMessageConverter can write ... as application/json`

JSON 변환기가 없습니다. `spring-boot-starter-web`(Jackson 포함)을 쓰는지 확인합니다.

## Mustache 화면

### `<react:OrderChart ... />: 'orders' was not found in the model or the enclosing sections.`

태그의 `{{orders}}`에 해당하는 값이 Model과 바깥 섹션 어디에도 없습니다. 컨트롤러에서 `model.addAttribute("orders", ...)`를 했는지, 이름 철자를 확인합니다. 섹션 문법(`{{#react}}`)에서는 `... Add it with model.addAttribute("orders", ...).`까지 안내합니다.

값이 `null`인 것은 오류가 아니라 `null` prop입니다.

### `island StatusBadge: unknown prop 'statsu', missing prop 'status'. Its @ReactIsland props are ...`

`@ReactIsland`로 선언한 props와 템플릿이 넘긴 props가 다릅니다. 메시지의 목록과 템플릿 속성을 맞춥니다. `order-id`는 `orderId`로 바뀐다는 점을 기억하세요. 생략해도 되는 prop은 Java 쪽에 `@Nullable`을 붙입니다.

### `Invalid component name '...'`

island 이름에는 영문자, 숫자, `_ $ . -`와 `/`만 쓸 수 있고 `..`은 안 됩니다. 파일 경로에서 `islands/`와 확장자를 뺀 이름을 씁니다.

### `<react:island ...>: write component="dir/Name"`

`<react:island>`에는 `component` 속성이 필요합니다. 하위 폴더가 없다면 `<react:StatusBadge ... />`처럼 이름을 태그로 씁니다.

### 템플릿이 통째로 깨진다, 엉뚱한 곳에 `}}`가 보인다

Mustache 주석 `{{! ... }}`은 **처음 나오는 `}}`에서 끝납니다.** 주석 안에 `{{변수}}`, `{{{mr.head}}}`를 쓰면 주석이 거기서 끝나고 나머지가 화면에 나오거나 템플릿 오류가 납니다. 주석에는 중괄호를 쓰지 마세요.

### 한글이 `????`로 나온다

Spring Boot의 Mustache 뷰는 charset 없이 `text/html`을 보내서, 요청 언어가 한국어면 서블릿 컨테이너가 ISO-8859-1을 고릅니다. mustache-react는 Mustache 뷰에 `text/html;charset=UTF-8`을 자동으로 붙입니다. 그래도 깨진다면:

- `spring.mustache.servlet.content-type`을 직접 지정했다면 charset을 포함했는지 확인합니다.
- `mustache-react.enabled=false`면 이 보정도 꺼집니다.

### island 자리가 비어 있다 (브라우저 콘솔에 `No component file for "islands/X"`)

`src/main/frontend/islands/X.tsx`가 없거나 이름 대소문자가 다릅니다. 콘솔 메시지의 `Known components` 목록과 비교합니다. 파일을 추가한 뒤에는 프론트를 다시 빌드해야 합니다(개발 모드는 자동).

### `"islands/X" has no default export`

컴포넌트를 `export default function X() {...}`로 내보내야 합니다.

### island 자리가 비어 있다 (콘솔 오류 없음)

`<react:scripts />`(`{{{mr.scripts}}}`)가 없거나 **마지막 island보다 앞에** 있습니다. `</body>` 바로 앞에 둡니다.

## 다국어

### 메시지 대신 키(`orders.title`)가 보인다

- `mustache-react.context.messages`에 그 키나 패턴(`orders.*`)이 있는지 확인합니다. 나열한 것만 페이지에 들어갑니다.
- 그 언어의 `messages_xx.properties`에 키가 있는지 확인합니다.

### 한국어를 요청했는데 영어 메시지가 나온다

`messages.properties`가 한국어이고 `messages_en.properties`가 있을 때, 서버 OS 언어가 영어면 Spring이 `messages_en`으로 대신합니다. 끕니다.

```properties
spring.messages.fallback-to-system-locale=false
```

### `?lang=en`이 동작하지 않는다

`mustache-react.i18n.param=lang`을 설정했는지 확인합니다. 직접 `localeResolver` 빈을 등록했다면 그 리졸버가 언어 변경을 지원해야 합니다(`AcceptHeaderLocaleResolver`는 지원하지 않습니다).

## 서버 렌더링

### `mustache-react: no SSR bundle at classpath:/META-INF/mustache-react/ssr/server.mjs ... Pages render in the browser.`

`mustache-react.ssr=auto`인데 SSR 번들이 jar에 없습니다. 빌드 플러그인에 `ssr = true`(Maven `<ssr>true</ssr>`)를 켜고, `package.json`에 `@mustache-react/server`가 있는지 확인합니다. `always`면 이 상태로는 시작이 실패합니다.

### `could not start the SSR server with 'node': ...`

운영 서버에 Node.js가 없습니다. SSR에는 `node` 실행 파일이 필요합니다(`node_modules`는 필요 없음). 설치하거나 `mustache-react.dev-server.node`로 경로를 지정하거나, SSR을 끕니다.

### `server-side rendering failed, rendering in the browser instead (logged once until it works again)`

`auto` 모드에서 렌더링이 실패해 브라우저 렌더링으로 대신했습니다. 화면은 정상입니다. 흔한 원인:

- 컴포넌트가 렌더링 중에 `window`, `document`, `localStorage`를 씁니다. `useEffect` 안으로 옮깁니다. [라이브러리](./libraries#브라우저-전용-라이브러리와-ssr) 참고.
- 렌더링이 `mustache-react.ssr-server.timeout`(기본 3초)을 넘었습니다.

### `the SSR server exited with code ...; restarting it`

Node.js 프로세스가 종료되어 다시 띄우는 중입니다(최대 10초에 한 번). 그동안은 브라우저 렌더링입니다. 반복되면 앞의 로그에서 Node.js 쪽 오류를 확인합니다.

### 콘솔에 hydration 경고가 나온다

서버에서 그린 HTML과 브라우저에서 처음 그린 결과가 다릅니다. 렌더링 중에 다음을 쓰면 생깁니다.

- `Date.now()`, `new Date()`, `Math.random()`
- `typeof window !== 'undefined'`로 분기
- 서버(Node.js)와 브라우저의 시간대·로캘이 다른 날짜·숫자 포맷

이런 값은 서버에서 props로 계산해 넘기거나, `useEffect`에서 설정합니다. 서버 컨텍스트(`useServerContext`)는 서버와 브라우저가 같은 값을 받으므로 안전합니다.

## 개발 모드

### `cannot start the Vite dev server: ... not found. Install the frontend dependencies`

프론트 의존성이 설치되지 않았습니다. `./gradlew mustacheReactInstall`(Maven `mvn generate-resources`)을 한 번 실행합니다.

### `the Vite dev server did not listen on localhost:5173 within ... ms`

개발 서버가 늦게 뜨거나 실패했습니다. 바로 위의 Vite 로그를 확인합니다. 처음 실행은 의존성 사전 번들 때문에 오래 걸릴 수 있습니다(`mustache-react.dev-server.startup-timeout`).

### 화면에 스크립트가 안 붙는다, 5173 포트가 이미 쓰인다

5173 포트에서 이미 무언가가 응답하면 mustache-react는 그것을 Vite 개발 서버로 보고 씁니다. 다른 프로젝트의 개발 서버가 떠 있는지 확인합니다. 포트를 바꾸려면:

```properties
mustache-react.dev-server.url=http://localhost:5199
```

### Mustache 템플릿을 고쳐도 반영되지 않는다

개발 프로필에 다음이 있는지 확인합니다. 없으면 jar 안의 템플릿을 캐시해서 씁니다.

```properties
spring.mustache.prefix=file:src/main/resources/templates/
spring.mustache.servlet.cache=false
```

## 배포

### CDN으로 바꾼 뒤 JS·CSS가 404

- CDN에 `static/mustache-react/` **폴더 안의 내용**(`assets/` 포함)을 올렸는지, `mustache-react.public-path`가 그 위치를 가리키는지 확인합니다(끝의 `/` 포함).
- 이전 버전 파일을 지웠다면, 아직 이전 jar로 도는 서버가 그 파일을 찾습니다. 해시가 붙은 파일은 지우지 마세요.

### CDN으로 바꾼 뒤 콘솔에 CORS 오류

다른 도메인의 JS 모듈은 CORS 헤더가 필요합니다. CDN 응답에 `Access-Control-Allow-Origin`을 추가합니다. [배포](./deployment) 참고.

:::
