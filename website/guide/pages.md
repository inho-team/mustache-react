# @ReactPage 페이지

::: v-pre

```java
@ReactPage(value = "orders/OrderList", title = "주문 목록")
@GetMapping("/orders")
public OrderListResponse list() {
    return orderService.list();
}
```

- 반환값이 `src/main/frontend/pages/orders/OrderList.tsx`의 props가 됩니다(default export).
- 반환값은 DTO, record, `Map`처럼 JSON 객체가 되는 값입니다. `null`이면 빈 props입니다.
- `@RestController`와 `@Controller` 모두에서 쓸 수 있습니다.

## 상태 코드와 헤더: `ResponseEntity`

`ResponseEntity`를 반환하면 상태 코드와 헤더가 HTML 페이지와 JSON 응답 모두에 적용되고, body가 props가 됩니다.

```java
@ReactPage("orders/OrderDetail")
@GetMapping("/orders/{id}")
public ResponseEntity<OrderDetailPage> detail(@PathVariable("id") long id) {
    return orders.findById(id)
            .map(order -> ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(new OrderDetailPage(id, order)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new OrderDetailPage(id, null)));
}
```

타입 생성기는 `ResponseEntity<T>`의 `T`를 props 타입으로 씁니다.

## 같은 URL, HTML과 JSON

| `Accept` | 응답 |
|---|---|
| `text/html` (브라우저 주소창) | React 페이지 |
| `*/*`, `application/json`, 없음 (curl, `fetch`, 앱) | 기존 JSON |

두 응답 모두 `Vary: Accept`를 붙입니다. props JSON은 Spring MVC의 HttpMessageConverter로 만들기 때문에 API JSON과 같습니다(Jackson 설정, Jackson 3, Gson).
`mustache-react.content-negotiation=false`면 항상 HTML입니다.

## 레이아웃

`src/main/resources/templates/mustache-react/layout.mustache`를 두면 기본 레이아웃 대신 쓰입니다.

```mustache
<!doctype html>
<html lang="{{lang}}">
<head>
  <meta charset="utf-8">
  <title>{{title}} | 내 서비스</title>
  {{{mr.head}}}
</head>
<body>
  {{> header}}
  {{{mr.page}}}
  {{{mr.scripts}}}
</body>
</html>
```

| 변수 | 내용 |
|---|---|
| `{{title}}` | `@ReactPage(title=...)`, 없으면 페이지 이름 |
| `{{lang}}` | 요청 locale |
| `{{page}}` | 페이지 이름 |
| `{{{mr.head}}}` | CSS, modulepreload |
| `{{{mr.page}}}` | React가 렌더링하는 요소와 props |
| `{{{mr.scripts}}}` | 서버 컨텍스트와 엔트리 script |

애플리케이션의 Mustache 컴파일러로 컴파일되므로 partial을 쓸 수 있습니다.

## 공통 Provider

`src/main/frontend/app.tsx`의 default export가 모든 페이지와 island를 감쌉니다. 테마, QueryClient, i18n Provider를 둡니다. island는 표 칸 안에도 들어가므로 레이아웃 요소는 넣지 않습니다.

:::
