# 테스트

::: v-pre

화면 대부분은 **서버 테스트로 검증**할 수 있습니다. 컨트롤러가 올바른 props를 만드는지, 템플릿이 island에 올바른 값을 넘기는지는 HTML 응답만 보면 됩니다. 브라우저에서 실제로 동작하는지는 마지막에 소수의 e2e 테스트로 확인합니다.

## 프론트 빌드 없이 테스트하기

테스트마다 프론트를 빌드하면 느립니다. 두 가지를 설정하면 빌드 없이 Spring 테스트가 돕니다.

1. **테스트용 manifest:** 빌드 결과 대신 작은 JSON 파일을 읽게 합니다.

   ```json
   // src/test/resources/test-manifest.json
   {
     "virtual:mustache-react/entry": {
       "file": "assets/entry-test.js", "name": "mustache-react", "isEntry": true,
       "src": "virtual:mustache-react/entry", "css": ["assets/entry-test.css"]
     },
     "pages/orders/OrderList.tsx": {
       "file": "assets/OrderList-test.js", "isDynamicEntry": true, "imports": ["virtual:mustache-react/entry"]
     }
   }
   ```

2. **테스트 설정:**

   ```java
   @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
           "mustache-react.manifest-location=classpath:/test-manifest.json",
           "mustache-react.ssr=off"})
   class OrderPageTest { ... }
   ```

그리고 빌드에서 프론트를 건너뜁니다.

```bash
./gradlew test -PmustacheReact.skipFrontend
```

```bash
mvn test -DmustacheReact.skipFrontend
```

예제 앱(`examples/basic`)의 테스트가 모두 이 방식입니다.

## `@ReactPage` 컨트롤러

같은 URL을 `Accept`만 바꿔 두 번 확인합니다.

```java
@Autowired MockMvc mvc;   // @AutoConfigureMockMvc

@Test
void browsersGetThePage() throws Exception {
    mvc.perform(get("/orders").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.VARY, containsString("Accept")))
            .andExpect(content().string(containsString("data-mr-island=\"pages/orders/OrderList\"")))
            // props는 Jackson JSON 그대로 페이지에 들어갑니다
            .andExpect(content().string(containsString("\"customer\":\"김민수\"")));
}

@Test
void apiClientsGetJson() throws Exception {
    mvc.perform(get("/orders").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(4));
}
```

props만 정확히 보고 싶다면 JSON 응답을 검사하는 쪽이 더 편합니다. HTML 쪽은 "페이지가 렌더링되고 올바른 컴포넌트를 가리킨다"만 확인하면 충분합니다.

- **상태 코드:** `ResponseEntity`로 404를 돌려주면 HTML 요청도 404입니다.
- **폼:** `redirect:`는 HTML 요청이면 303, JSON 요청이면 `{"redirect":"/orders"}`입니다. 검증 에러는 JSON 요청이면 400 `{"errors":{...}}`, HTML 요청이면 같은 페이지에 서버 컨텍스트로 들어갑니다.
- **보안:** POST 테스트에는 CSRF 토큰이 필요합니다(`.with(csrf())`).

## Mustache island

뷰를 렌더링한 HTML에 island 표시와 props가 있는지 봅니다.

```java
mvc.perform(get("/dashboard").accept(MediaType.TEXT_HTML))
        .andExpect(content().string(containsString("data-mr-island=\"islands/StatusBadge\"")))
        .andExpect(content().string(containsString("{\"status\":\"PAID\",\"orderId\":1001}")));
```

island id는 페이지 안에서 `mr-0`, `mr-1` 순서로 붙습니다. 특정 순서를 검사해도 되고, props 내용만 검사해도 됩니다.

### 잘못된 템플릿 잡기

`@ReactIsland`로 props를 선언했다면, 템플릿이 없는 prop을 넘기거나 필수 prop을 빼먹으면 **렌더링이 실패**합니다(500). Model에 없는 값을 참조해도 마찬가지입니다. 화면마다 "200으로 렌더링되는지" 테스트 하나씩만 있어도 이런 실수가 배포 전에 잡힙니다.

```java
@ParameterizedTest
@ValueSource(strings = {"/", "/orders", "/dashboard"})
void everyPageRenders(String path) throws Exception {
    mvc.perform(get(path).accept(MediaType.TEXT_HTML)).andExpect(status().isOk());
}
```

## 서버 렌더링

SSR을 테스트하려면 실제 프론트 빌드와 Node.js가 필요합니다. 일반 테스트는 `ssr=off`로 두고, SSR 테스트는 따로 둡니다. 예제 앱의 `ServerSideRenderingTest`처럼 `SsrEngine` 빈을 테스트용으로 바꿔 끼우면 Node.js 없이도 "SSR HTML이 들어가는지, 실패하면 브라우저 렌더링으로 넘어가는지"를 검사할 수 있습니다.

## 프론트 단위 테스트 (선택)

컴포넌트에 로직이 많다면 Vitest와 Testing Library로 단위 테스트를 둘 수 있습니다. 백엔드 개발자만 있는 팀이라면 위의 서버 테스트와 아래 e2e로 충분한 경우가 많습니다.

```bash
cd src/main/frontend
npm install -D vitest jsdom @testing-library/react
```

```tsx
// islands/StatusBadge.test.tsx
// @vitest-environment jsdom
import { render, screen } from '@testing-library/react';
import { ServerContextProvider } from '@mustache-react/client';
import StatusBadge from './StatusBadge';

it('shows the status label', () => {
  render(
    <ServerContextProvider value={{ messages: { 'orders.status.PAID': '결제 완료' } }}>
      <StatusBadge status="PAID" orderId={1001} />
    </ServerContextProvider>,
  );
  expect(screen.getByRole('button').textContent).toBe('결제 완료');
});
```

`ServerContextProvider`로 `useServerContext()`, `useMessages()`가 읽을 값을 넣어 줍니다.

## 브라우저 e2e

실제 브라우저에서 "React가 붙고 클릭이 동작하는지"는 Playwright로 확인합니다. 예제 앱의 `examples/basic/e2e/smoke.mjs`가 참고용입니다.

```bash
./gradlew bootRun --args=--server.port=18080 &
node e2e/smoke.mjs
```

```js
import { chromium } from 'playwright';

const browser = await chromium.launch();
const page = await browser.newPage();
const errors = [];
page.on('pageerror', (e) => errors.push(e));

await page.goto('http://localhost:18080/orders');
await page.getByRole('heading', { name: '주문 목록' }).waitFor();
await page.getByLabel('고객 검색').fill('이서연');
// ...
if (errors.length) throw errors[0];
await browser.close();
```

- 콘솔 에러(`pageerror`, `console.error`)가 없는지 함께 확인하세요. island 마운트 실패는 콘솔 에러로 나타납니다.
- e2e는 화면마다 핵심 흐름 하나 정도만 둡니다. 세부 검사는 서버 테스트가 빠르고 안정적입니다.

:::
