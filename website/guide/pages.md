# @ReactPage 페이지

::: v-pre

화면 전체를 React 컴포넌트 하나로 만드는 방식입니다. 컨트롤러가 반환한 값이 그대로 그 컴포넌트의 props가 됩니다. 템플릿을 따로 쓰지 않습니다.

```java
@RestController
public class OrderController {

    @ReactPage(value = "orders/OrderList", title = "orders.title")
    @GetMapping("/orders")
    public OrderListResponse list() {
        return new OrderListResponse(orders.findAll());
    }
}
```

```tsx
// src/main/frontend/pages/orders/OrderList.tsx
import type { PageProps } from '../../generated/types';

export default function OrderList({ orders, total }: PageProps<'orders/OrderList'>) {
  return <p>{orders.length} / {total}</p>;
}
```

## 요청이 처리되는 순서

1. Spring MVC가 평소처럼 컨트롤러 메서드를 호출합니다. 인증, `@PathVariable`, `@Valid`, 인터셉터가 모두 그대로 동작합니다.
2. 메서드에 `@ReactPage`가 있으면 mustache-react가 반환값을 받습니다.
3. 요청의 `Accept` 헤더를 보고 **HTML 페이지**를 줄지 **JSON**을 줄지 정합니다(아래 [같은 URL, HTML과 JSON](#같은-url-html과-json)).
4. HTML이면 반환값을 Spring MVC의 JSON 변환기로 props JSON을 만들고, 레이아웃에 넣어 HTML 문서를 돌려줍니다. 서버 렌더링이 켜져 있으면 컴포넌트를 HTML로 미리 그려 넣습니다.
5. 브라우저가 엔트리 스크립트를 받고, 그 페이지 컴포넌트 파일만 불러와 props로 렌더링(또는 hydration)합니다.

## 페이지 이름

| `@ReactPage` 값 | 컴포넌트 파일 |
|---|---|
| `"Hello"` | `src/main/frontend/pages/Hello.tsx` |
| `"orders/OrderList"` | `src/main/frontend/pages/orders/OrderList.tsx` |
| `"admin/users/UserEdit"` | `src/main/frontend/pages/admin/users/UserEdit.tsx` |

- 확장자 없이, `pages/` 아래 경로로 씁니다. `.tsx`, `.jsx` 모두 됩니다.
- 파일은 컴포넌트를 **default export** 해야 합니다.
- 이름에는 영문, 숫자, `_ $ . -`와 `/`만 쓸 수 있고 `..`은 안 됩니다. 어기면 요청이 오류로 끝납니다.
- 파일이 없으면 서버는 정상 응답하지만 브라우저 콘솔에 `No component file for "pages/..."`가 찍히고 화면이 비어 있습니다. 개발 모드에서는 그 자리에 빨간 오류 상자가 나옵니다.

## props: 무엇을 반환하나

| 반환값 | props |
|---|---|
| DTO, record, POJO | JSON 객체의 각 속성 (getter, record 접근자, public 필드) |
| `Map<String, ?>` | 각 키 |
| `null`, `void` | 빈 props `{}` |
| `ResponseEntity<T>` | body `T` (상태 코드와 헤더는 응답에 적용) |
| `"redirect:/path"` (`String`/`Object` 반환) | 페이지 대신 이동, 아래 참고 |

- props JSON은 애플리케이션의 **Spring MVC JSON 설정을 그대로** 씁니다. 같은 URL을 API로 부를 때와 결과가 똑같습니다(`@JsonProperty`, `@JsonIgnore`, 날짜 형식, Jackson 3, Gson 모두).
- 날짜(`LocalDate` 등)는 Spring Boot 기본 설정이면 ISO 문자열(`"2026-11-01"`)로 갑니다.
- props는 HTML의 `<script type="application/json">` 안에 들어갑니다. `</script>`, `<`, `>`, `&` 등은 이스케이프되므로 사용자 입력이 들어 있어도 스크립트가 실행되지 않습니다.
- 페이지에 쓰이므로 **화면에 보여주면 안 되는 값(비밀번호 해시, 내부 ID 등)은 DTO에 넣지 마세요.** `@JsonIgnore`를 붙여도 됩니다.

## `@Controller`와 `@RestController`

둘 다 됩니다. `@ReactPage`가 붙은 메서드만 이 방식으로 처리되고, 나머지 메서드는 원래대로 동작합니다. 기존 API 컨트롤러에 `@ReactPage`만 붙이면 API는 그대로 두고 화면이 생깁니다.

## 제목

```java
@ReactPage(value = "orders/OrderList", title = "주문 목록")     // 그대로 제목
@ReactPage(value = "orders/OrderList", title = "orders.title")  // 메시지 키
@ReactPage("orders/OrderList")                                  // 제목 없음 → 페이지 이름
```

`title`이 `messages.properties`에 있는 키이면 요청 언어의 메시지가, 아니면 문자열 그대로 `<title>`이 됩니다. 비워 두면 페이지 이름(`orders/OrderList`)이 들어갑니다. [다국어](./i18n) 참고.

## 상태 코드와 헤더: `ResponseEntity`

`ResponseEntity`를 반환하면 상태 코드와 헤더가 HTML 페이지와 JSON 응답 모두에 적용되고, body가 props가 됩니다.

```java
@ReactPage(value = "orders/OrderDetail", title = "orders.detail.title")
@GetMapping("/orders/{id}")
public ResponseEntity<OrderDetailPage> detail(@PathVariable("id") long id) {
    Optional<Order> order = orders.findById(id);
    if (!order.isPresent()) {
        // 404 페이지도 React로: order가 null인 props
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new OrderDetailPage(id, null));
    }
    return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(new OrderDetailPage(id, order.get()));
}
```

```tsx
export default function OrderDetail({ id, order }: PageProps<'orders/OrderDetail'>) {
  return order ? <OrderView order={order} /> : <p>주문 #{id}을(를) 찾을 수 없습니다.</p>;
}
```

타입 생성기는 `ResponseEntity<T>`의 `T`를 props 타입으로 씁니다.

## 같은 URL, HTML과 JSON

`@ReactPage` URL은 요청한 쪽에 따라 다르게 응답합니다.

| 누가 | `Accept` | 응답 |
|---|---|---|
| 브라우저 주소창, 링크 클릭, 폼 제출 | `text/html,...` | React 페이지 (HTML) |
| `fetch`, `springFetch`, axios | `*/*` 또는 `application/json` | 반환값 JSON |
| curl, Postman, 모바일 앱, 다른 서버 | 없음, `*/*`, `application/json` | 반환값 JSON |

규칙: `Accept`에 `text/html`(또는 XHTML)이 **명시되어 있고** 그 우선순위(q)가 JSON 이상일 때만 HTML입니다. 나머지는 모두 JSON이라 기존 API 클라이언트는 영향이 없습니다.

- 두 응답 모두 `Vary: Accept` 헤더가 붙어 브라우저와 프록시 캐시가 둘을 섞지 않습니다.
- 항상 HTML로만 응답하게 하려면 `mustache-react.content-negotiation=false`.
- React 안에서 같은 URL의 데이터를 다시 받으려면 `springFetch`를 씁니다. `Accept: application/json`과 CSRF 헤더를 붙여 줍니다.

```tsx
import { springFetch } from '@mustache-react/client';

const response = await springFetch('/orders');
const { orders } = await response.json();
```

## 리다이렉트와 검증 에러

폼 처리 메서드는 Spring MVC에서처럼 `"redirect:/path"`를 반환하고 `BindingResult`로 검증 에러를 받습니다.

| 상황 | 브라우저(HTML) 요청 | `useSpringForm`(JSON) 요청 |
|---|---|---|
| `return "redirect:/orders"` | 303, `Location: /orders` | 200 `{"redirect":"/orders"}` → 클라이언트가 이동 |
| `BindingResult`에 에러 | 같은 페이지를 다시 그림, 에러는 서버 컨텍스트로 | 400 `{"errors":{"fields":{...},"global":[...]}}` |

`/`로 시작하는 리다이렉트 경로에는 컨텍스트 경로가 붙습니다. 자세한 내용은 [폼과 서버 컨텍스트](./forms).

## 예외와 에러 페이지

`@ReactPage` 메서드에서 던진 예외는 Spring이 평소처럼 처리합니다(`@ExceptionHandler`, `@ControllerAdvice`, Spring Boot 에러 페이지). 404·403 같은 에러 화면을 React로 그리고 싶으면, 위처럼 `ResponseEntity`로 상태 코드와 함께 props를 반환하는 방법이 가장 간단합니다.

## 페이지 사이 이동

페이지 이동은 일반 링크(`<a href="/orders/1001">`)와 리다이렉트로 합니다. 이동할 때마다 서버가 새 HTML을 주는 **전통적인 MVC 방식**이라, 페이지마다 URL, 권한 검사, 서버 렌더링이 그대로 적용됩니다. 한 페이지 안의 인터랙션(검색, 정렬, 탭)은 React 상태로 처리합니다.

## 레이아웃

모든 `@ReactPage`는 하나의 Mustache 레이아웃 안에 그려집니다. `src/main/resources/templates/mustache-react/layout.mustache`를 만들면 기본 레이아웃 대신 그 파일을 씁니다. 위치는 `mustache-react.layout`으로 바꿀 수 있습니다.

기본 레이아웃:

```mustache
<!doctype html>
<html lang="{{lang}}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{{title}}</title>
{{{mr.head}}}
</head>
<body>
{{{mr.page}}}
{{{mr.scripts}}}
</body>
</html>
```

예: 공통 헤더, 파비콘, 분석 스크립트를 넣은 레이아웃

```mustache
<!doctype html>
<html lang="{{lang}}">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>{{title}} | 내 서비스</title>
  <link rel="icon" href="/favicon.ico">
  <react:head />
</head>
<body>
  {{> partials/top-banner}}
  {{{mr.page}}}
  <react:scripts />
</body>
</html>
```

| 변수 | 내용 |
|---|---|
| `{{title}}` | `@ReactPage(title=...)`(메시지 키면 번역됨), 없으면 페이지 이름 |
| `{{lang}}` | 요청 locale 태그 (`ko`, `en-US`) |
| `{{page}}` | 페이지 이름 (`orders/OrderList`) |
| `{{{mr.head}}}` 또는 `<react:head />` | CSS, 미리 불러올 JS (`<head>` 안) |
| `{{{mr.page}}}` | React가 그려질 요소와 props (서버 렌더링 결과 포함) |
| `{{{mr.scripts}}}` 또는 `<react:scripts />` | 서버 컨텍스트와 엔트리 스크립트 (`</body>` 앞) |

- 레이아웃은 애플리케이션의 Mustache 컴파일러로 컴파일되므로 partial(`{{> ...}}`)을 쓸 수 있습니다.
- 레이아웃에는 컨트롤러 Model이 들어오지 않습니다. 위 변수만 쓸 수 있습니다. 페이지마다 다른 내용은 React 컴포넌트에서 그립니다.
- 레이아웃은 처음 쓸 때 한 번 읽어 둡니다. 고친 뒤에는 애플리케이션을 다시 시작해야 합니다.

## 모든 페이지에 공통으로: `app.tsx`

`src/main/frontend/app.tsx`의 default export가 모든 페이지와 island를 감쌉니다. 테마, 데이터 라이브러리, 상태 관리처럼 Provider가 필요한 라이브러리를 여기에 둡니다.

```tsx
// src/main/frontend/app.tsx
import type { ReactNode } from 'react';
import './app.css';

export default function App({ children }: { children?: ReactNode }) {
  return <ThemeProvider theme={theme}>{children}</ThemeProvider>;
}
```

island는 표 칸 안에도 들어가므로 여기에 `<div>` 같은 레이아웃 요소는 넣지 않습니다. 공통 헤더 같은 화면 요소는 `components/Header.tsx`처럼 따로 만들어 각 페이지에서 씁니다(예제의 `Header`).

## 서버 정보: `useServerContext`

모든 페이지에 로그인 사용자, CSRF 토큰, locale, 메시지가 함께 내려옵니다.

```tsx
import { useServerContext } from '@mustache-react/client';

export default function Header() {
  const { user } = useServerContext();
  return user ? <span>{user.name}님</span> : <a href="/login">로그인</a>;
}
```

항목과 확장 방법은 [폼과 서버 컨텍스트](./forms#서버-컨텍스트).

## 테스트

`@ReactPage`는 일반 Spring MVC 핸들러라 `MockMvc`나 `@SpringBootTest`로 테스트합니다. `Accept: text/html`이면 HTML, 아니면 JSON입니다. 자세한 예는 [테스트](./testing).

:::
