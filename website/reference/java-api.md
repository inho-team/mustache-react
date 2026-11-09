# Java API

::: v-pre

애플리케이션 코드에서 쓰는 것은 대부분 애너테이션 세 개와 `ServerContextContributor` 하나입니다. 나머지는 자동 설정이 만들어 주는 빈입니다. 패키지는 `run.iknow.mustachereact.spring`(SsrEngine은 `run.iknow.mustachereact.core`)입니다.

## 애너테이션

### `@ReactPage`

```java
@Target(METHOD) @Retention(RUNTIME)
public @interface ReactPage {
    String value();             // pages/ 아래 컴포넌트 경로, 확장자 없이: "orders/OrderList"
    String title() default "";  // 페이지 제목, 또는 메시지 키
}
```

Spring MVC 핸들러 메서드에 붙이면 반환값이 `src/main/frontend/pages/<value>.tsx`의 props가 됩니다. `@Controller`와 `@RestController` 모두에서 씁니다.

```java
@ReactPage(value = "orders/OrderList", title = "orders.title")
@GetMapping("/orders")
public OrderListResponse list() {
    return new OrderListResponse(orders.findAll());
}
```

- `value`: 글자, 숫자, `_ $ . -`와 `/`만 쓸 수 있고 `..`는 안 됩니다.
- `title`: `MessageSource`에 같은 코드의 메시지가 있으면 요청 locale의 메시지가 제목이 됩니다(`orders.title` → "주문 목록"). 없으면 문자열 그대로, 비어 있으면 페이지 이름입니다.

### `@ReactIsland`

```java
@Target(TYPE) @Retention(CLASS)
public @interface ReactIsland {
    String value();   // islands/ 아래 컴포넌트 경로: "StatusBadge", "charts/OrderChart"
}
```

island의 props를 Java 클래스로 선언합니다. 클래스는 선언용이고, 실제 값은 템플릿이 넘깁니다.

```java
@ReactIsland("StatusBadge")
public class StatusBadgeProps {
    private OrderStatus status;
    private long orderId;
    @Nullable private String note;   // 생략 가능
    // getter
}
```

- 타입 생성기가 `IslandProps<'StatusBadge'>`를 `types.ts`에 씁니다.
- 템플릿이 island를 렌더링할 때, 없는 prop을 넘기거나 `@Nullable`이 아닌 prop을 빠뜨리면 렌더링이 실패합니다. 예외 메시지는 이렇습니다: `island StatusBadge: unknown prop 'statsu', missing prop 'status'. Its @ReactIsland props are status, orderId, note (optional).`
- 같은 이름을 두 클래스에 선언하면 컴파일 에러입니다.

### `@ReactType`

```java
@Target(TYPE) @Retention(CLASS)
public @interface ReactType { }
```

`@ReactPage` 반환 타입에서 닿지 않는 클래스의 TypeScript 타입을 만들 때 붙입니다. `@ReactPage` 반환 타입과 그 타입이 참조하는 클래스는 애너테이션 없이도 타입이 생깁니다.

## `@ReactPage` 반환값

| 반환값 | 브라우저 (`Accept: text/html`) | JSON 요청 |
|---|---|---|
| DTO, record, `Map` | 페이지. 반환값이 props | 반환값 JSON |
| `null` | 페이지. props 없음 | 빈 body |
| `ResponseEntity<T>` | 상태 코드와 헤더를 적용한 페이지. body가 props | 상태 코드, 헤더, body |
| `"redirect:/path"` | `303 See Other`, `Location: /path` | `200 {"redirect": "/path"}` |
| 검증 에러가 있을 때 | 페이지를 다시 그림. 에러는 서버 컨텍스트 `errors`로 | `400 {"errors": {"fields": {...}, "global": [...]}}` |

- **HTML인지 JSON인지:** `Accept`에 `text/html`(또는 XHTML)이 명시돼 있고 JSON보다 품질 값이 낮지 않을 때만 HTML입니다. `Accept`가 없거나 `*/*`, `application/json`이면 JSON입니다. `mustache-react.content-negotiation=false`면 항상 HTML입니다.
- 두 응답 모두 `Vary: Accept` 헤더가 붙습니다.
- props JSON은 Spring MVC의 `HttpMessageConverter`로 만듭니다. 그래서 Jackson 설정과 애너테이션(`@JsonProperty`, `@JsonIgnore` 등)이 API 응답과 똑같이 적용됩니다.
- **redirect 경로:** `/`로 시작하면 컨텍스트 경로가 앞에 붙습니다. `//`로 시작하거나 절대 URL이면 그대로 씁니다.
- **검증 에러:** Model에 에러가 있는 `BindingResult`가 하나라도 있으면 검증 에러로 봅니다. 핸들러가 `BindingResult` 파라미터를 받으면 Model에 들어갑니다. 메시지는 `MessageSource`로 요청 locale에 맞춰 만듭니다. `fields`는 필드 이름 → 메시지 목록, `global`은 `reject(...)`로 넣은 메시지입니다.

```java
@ReactPage("orders/OrderForm")
@PostMapping("/orders/new")
public Object create(@Valid @RequestBody OrderForm form, BindingResult result) {
    if (result.hasErrors()) {
        return new OrderFormPage(form);   // 에러와 함께 다시
    }
    orders.add(form);
    return "redirect:/orders";
}
```

## 서버 컨텍스트: `ServerContextContributor`

```java
@FunctionalInterface
public interface ServerContextContributor {
    void contribute(Map<String, Object> context);
}
```

모든 페이지(`@ReactPage`, island가 있는 Mustache 화면)에 들어가는 `<script id="mr-context">`를 채웁니다. React에서는 `useServerContext()`로 읽습니다. 빈으로 등록하면 되고, 순서는 `@Order`를 따릅니다.

```java
@Bean
@Order(100)
ServerContextContributor tenant(TenantResolver tenants) {
    return context -> context.put("tenant", tenants.current().getName());
}
```

- 요청 스레드에서 실행되므로 `RequestContextHolder`와 Spring Security 컨텍스트를 쓸 수 있습니다.
- 넣은 값은 그대로 페이지 HTML에 들어갑니다. **비밀 값은 넣지 마세요.**
- 하나가 예외를 던지면 경고 로그만 남고 그 항목만 빠집니다. 페이지는 정상으로 나갑니다.
- 직렬화는 props와 같은 메시지 변환기를 씁니다.

기본으로 등록되는 항목:

| 키 | 내용 | 끄기 |
|---|---|---|
| `mustacheReact` | `{version}`: 서버 라이브러리 버전. 클라이언트와 다르면 콘솔 경고 | - |
| `locale` | 요청 locale(`ko-KR`) | - |
| `i18n` | `{param, locales}`. `mustache-react.i18n.param`이나 `locales`가 있을 때만 | - |
| `messages` | `mustache-react.context.messages`에 적은 메시지 | 비워 두면 없음 |
| `csrf` | `{headerName, parameterName, token}` (Spring Security) | `context.csrf=false` |
| `user` | `{name, authorities}`, 비로그인은 `null` (Spring Security) | `context.user=false` |
| `errors` | 검증 에러 `{fields, global}`. 에러가 있는 요청에만 | - |

## Mustache Model: `react`, `mr`

`template.model-attributes=true`(기본)면 모든 Spring MVC Model에 두 객체가 들어갑니다. 이름은 `template.react-name`, `template.helpers-name`으로 바꿉니다.

| 템플릿에서 | 내용 |
|---|---|
| `<react:Name a="{{x}}" b="글자" flag />` | island. 값이 `{{경로}}` 하나면 객체 그대로, 아니면 문자열, 값이 없으면 `true`. `order-id` → `orderId` |
| `<react:island component="dir/Name" ... />` | 하위 디렉터리의 island |
| `{{#react}}Name a x=a.b item=this{{/react}}` | 섹션 문법. 결과는 태그와 같습니다 |
| `<react:head />`, `{{{mr.head}}}` | 엔트리 CSS와 modulepreload. `<head>` 안 |
| `<react:scripts />`, `{{{mr.scripts}}}` | 쓰인 island의 미리 불러오기, 서버 컨텍스트, 진입 스크립트. 마지막 island 뒤 |
| `{{#mr.t}}key arg ...{{/mr.t}}` | 요청 locale의 메시지(HTML 이스케이프). 인자는 Model 경로이고 `{0}`, `{1}`을 채웁니다. 없는 키는 키 그대로 |
| `{{mr.lang}}` | 요청 locale(`ko-KR`). `<html lang="{{mr.lang}}">` |

값은 Mustache 규칙대로 찾습니다(현재 루프 항목 → 바깥 섹션 → Model). `Map` 키, record 접근자(`total()`), getter, public 필드를 읽고, 없는 이름은 템플릿 오류입니다.

## `@ReactPage` 레이아웃

`src/main/resources/templates/mustache-react/layout.mustache`(`mustache-react.layout`)를 두면 내장 레이아웃 대신 씁니다. 애플리케이션의 Mustache 컴파일러로 컴파일되므로 partial(`{{> header}}`)을 쓸 수 있습니다.

| 변수 | 내용 |
|---|---|
| `{{title}}` | `@ReactPage(title)`(메시지 키면 메시지), 없으면 페이지 이름 |
| `{{lang}}` | 요청 locale |
| `{{page}}` | 페이지 이름(`orders/OrderList`) |
| `{{{mr.head}}}` | 페이지의 CSS와 modulepreload |
| `{{{mr.page}}}` | React가 렌더링할 요소와 props(SSR이면 HTML 포함) |
| `{{{mr.scripts}}}` | 서버 컨텍스트와 진입 스크립트 |
| `{{mr.lang}}`, `{{#mr.t}}key{{/mr.t}}` | 요청 locale 태그와 메시지 (Mustache 화면과 같음) |
| `<react:Name ... />`, `<react:head />`, `<react:scripts />` | island와 에셋 태그 (`template.html-tags=true`일 때) |

`mr`은 `template.helpers-name`으로 바꾼 이름으로도 들어갑니다. 레이아웃은 운영에서는 한 번 컴파일되어 유지되고, 개발 모드(`dev-server.enabled=true`)에서는 페이지마다 다시 읽습니다.

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

## 확장 지점

자동 설정의 빈은 대부분 `@ConditionalOnMissingBean`이라, 같은 타입의 빈을 등록하면 교체됩니다.

### `SsrEngine` (서버 렌더링 엔진)

```java
package run.iknow.mustachereact.core;

@FunctionalInterface
public interface SsrEngine {
    String render(String component, String propsJson, String contextJson) throws SsrException;
}
```

`component`는 `pages/orders/OrderList`, `islands/StatusBadge` 같은 경로입니다. 기본 구현은 `HttpSsrEngine`으로, Node.js SSR 서버(운영)나 Vite 개발 서버(개발)의 `POST /__mustache-react/render`를 부릅니다. GraalJS처럼 다른 엔진을 쓰려면 `SsrEngine` 빈을 등록합니다. `SsrException`을 던지면 `ssr=auto`에서는 그 요소만 브라우저 렌더링으로 넘어갑니다.

### 그 밖의 빈

| 빈 | 역할 |
|---|---|
| `FrontendAssets` | 에셋 태그를 만듭니다. 빌드 결과는 `ManifestFrontendAssets`, 개발 서버는 `DevServerFrontendAssets` |
| `ViteManifestSource` | Vite manifest를 읽습니다(`manifest-location`) |
| `ReactPageRenderer` | `@ReactPage` HTML을 레이아웃으로 렌더링합니다 |
| `ServerSideRendering` | `ssr` 모드에 따라 SSR을 시도하고, 실패 시 처리합니다 |
| `ServerContextFactory` | `ServerContextContributor`들을 모아 서버 컨텍스트 JSON을 만듭니다 |
| `PropsSerializer` | props를 Spring MVC 메시지 변환기로 JSON으로 만듭니다 |
| `ReactIslandLambda`, `MustacheReactHelpers` | Mustache Model의 `react`, `mr` |
| `MessageKeys` | 메시지 파일의 키 목록(`orders.*` 패턴 확장용) |
| `IslandRenderer` | island 마크업을 만들고 `@ReactIsland` props를 검사합니다. `IslandDeclarations.load(classLoader)`로 선언을 읽습니다 |
| `IslandTagCompiler` | `<react:...>` 태그를 이해하는 Mustache 컴파일러. 기존 컴파일러 설정을 그대로 유지합니다. JMustache의 `with...()`는 일반 컴파일러를 돌려주므로 설정을 마친 컴파일러를 감싸야 합니다 |

:::
