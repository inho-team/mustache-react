# 폼과 서버 컨텍스트

:::: v-pre
## 서버 컨텍스트

모든 페이지(`@ReactPage`와 island가 있는 Mustache 화면)에 서버 정보가 JSON으로 함께 내려옵니다(`<script id="mr-context">`). React에서는 `useServerContext()`로 읽습니다.

```tsx
import { useServerContext } from '@mustache-react/client';

const { user, csrf, locale, messages, i18n, errors } = useServerContext();
```

| 키 | 내용 | 켜고 끄기 |
|---|---|---|
| `locale` | 요청 locale 태그 (`ko`, `en-US`) | 항상 |
| `csrf` | `{headerName, parameterName, token}` (Spring Security가 있을 때) | `mustache-react.context.csrf` |
| `user` | `{name, authorities}`, 비로그인은 `null` (Spring Security가 있을 때) | `mustache-react.context.user` |
| `messages` | `mustache-react.context.messages`에 적은 키의 메시지. [다국어](./i18n) | 키를 적을 때만 |
| `i18n` | `{param, locales}`: 언어 전환 설정 | `mustache-react.i18n.*`를 설정할 때만 |
| `errors` | 폼을 다시 그릴 때의 검증 에러 `{fields, global}` | 에러가 있을 때만 |
| `mustacheReact` | `{version}`: 서버의 mustache-react 버전 | 항상 |

### 직접 항목 추가

`ServerContextContributor` 빈을 등록하면 모든 페이지에 값이 추가됩니다. **페이지 HTML에 그대로 쓰이므로 비밀 값은 넣지 마세요.**

```java
@Bean
ServerContextContributor tenant(TenantResolver tenants) {
    return context -> context.put("tenant", tenants.current().getName());
}
```

```tsx
interface MyContext extends ServerContext { tenant?: string }
const { tenant } = useServerContext<MyContext>();
```

요청마다 실행되므로 무거운 조회는 피합니다. 여러 빈의 순서는 `@Order`로 정합니다.

### API 호출: `springFetch`

애플리케이션의 다른 URL을 부를 때는 `springFetch`를 씁니다. `fetch`와 같지만 `Accept: application/json`과 CSRF 헤더를 자동으로 붙이고, 쿠키(세션)를 함께 보냅니다.

```tsx
import { springFetch } from '@mustache-react/client';

// 조회: @ReactPage URL도 JSON으로 응답
const orders = await (await springFetch('/orders')).json();

// 변경: POST/PUT/DELETE에도 CSRF 토큰이 붙음
await springFetch('/api/orders/1001/cancel', { method: 'POST' });

await springFetch('/api/orders', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ customer: '최유나', amount: 45000 }),
});
```

파일 업로드는 `FormData`를 그대로 넘깁니다. `Content-Type`을 지정하지 않으면 브라우저가 multipart 경계를 붙입니다.

```tsx
const data = new FormData();
data.append('file', fileInput.files[0]);
await springFetch('/api/attachments', { method: 'POST', body: data });
```

```java
@PostMapping("/api/attachments")
public AttachmentDto upload(@RequestParam("file") MultipartFile file) { ... }
```

## 폼: `useSpringForm`

Spring MVC 폼 처리(`@Valid` + `BindingResult` + `redirect:`)를 그대로 쓰면서 화면은 React로 만드는 방법입니다.

```java
@ReactPage(value = "orders/OrderForm", title = "orders.new")
@GetMapping("/orders/new")
public OrderFormPage newOrder() {
    return new OrderFormPage(new OrderForm());           // 빈 폼
}

@ReactPage(value = "orders/OrderForm", title = "orders.new")
@PostMapping("/orders/new")
public Object create(@Valid @RequestBody OrderForm form, BindingResult result) {
    if (result.hasErrors()) {
        return new OrderFormPage(form);                  // 입력값 그대로 다시
    }
    orders.add(form);
    return "redirect:/orders";
}
```

```tsx
import { useSpringForm } from '@mustache-react/client';
import type { OrderForm as Form, PageProps } from '../../generated/types';

export default function OrderForm({ form: initial }: PageProps<'orders/OrderForm'>) {
  const form = useSpringForm<Form>({ initialValues: initial, action: '/orders/new' });
  return (
    <form onSubmit={form.submit} noValidate>
      <label>
        고객
        <input {...form.field('customer')} />
        {form.errors.customer?.map((error) => <span key={error} className="error">{error}</span>)}
      </label>
      <label>
        금액
        <input type="number" {...form.field('amount')} />
        {form.errors.amount?.map((error) => <span key={error} className="error">{error}</span>)}
      </label>
      {form.globalErrors.map((error) => <p key={error} className="error">{error}</p>)}
      <button type="submit" disabled={form.submitting}>등록</button>
    </form>
  );
}
```

### 결과 처리

| 서버 결과 | `useSpringForm`(JSON) | JavaScript 없는 일반 폼 전송 |
|---|---|---|
| `return "redirect:/orders"` | 200 `{"redirect":"/orders"}` → 그 주소로 이동 | 303 → 이동 |
| `BindingResult`에 에러 | 400 `{"errors":{fields, global}}` → `form.errors`에 표시 | 같은 페이지를 다시 그림, 에러는 서버 컨텍스트 `errors`로 |
| 그 밖의 값 반환 | 200 JSON → `onSuccess(body)` 호출 | 그 값을 props로 페이지를 그림 |
| 4xx/5xx (위 400 제외) | `submit()`이 `Error`를 던짐 | Spring 에러 페이지 |

페이지를 다시 그린 경우(일반 폼 전송)에도 `useSpringForm`은 서버 컨텍스트의 `errors`를 처음부터 보여 줍니다.

### 옵션

| 옵션 | 기본값 | 설명 |
|---|---|---|
| `initialValues` | (필수) | 초기값. 보통 페이지 props의 폼 객체 |
| `action` | 현재 URL | 보낼 주소 |
| `method` | `'POST'` | `'POST' \| 'PUT' \| 'PATCH' \| 'DELETE'` |
| `encoding` | `'json'` | `'json'`은 `@RequestBody`, `'form'`은 `@ModelAttribute` (아래) |
| `onSuccess` | 없음 | 리다이렉트 없이 성공했을 때 응답 body로 호출 |
| `navigate` | `window.location.assign` | `redirect:` 이동 방법을 바꿀 때 |

### 반환값

| 이름 | 설명 |
|---|---|
| `values` | 현재 입력값 |
| `field(name)` | `<input {...form.field('customer')} />`. `name`, `value`, `onChange`, 에러가 있으면 `aria-invalid` |
| `setValue(name, value)` | 값을 직접 바꿀 때 (체크박스, 커스텀 컴포넌트) |
| `errors` | 필드별 에러 메시지 배열. 중첩 필드는 `errors['address.city']` |
| `globalErrors` | 필드와 무관한 에러 (`BindingResult.reject(...)`) |
| `submitting` | 전송 중 `true`. 버튼 중복 클릭 막기에 씁니다 |
| `submit(event?)` | `<form onSubmit={form.submit}>` |

- 초기값이 숫자인 필드와 `type="number"` 입력은 숫자로 유지됩니다. 비우면 `null`입니다.
- 체크박스·셀렉트처럼 `field()`가 맞지 않는 입력은 `values`와 `setValue`로 직접 연결합니다.

```tsx
<input type="checkbox" checked={form.values.urgent}
       onChange={(e) => form.setValue('urgent', e.target.checked)} />
```

### 에러 메시지와 다국어

에러 메시지는 Spring의 `MessageSource`로 **요청 언어에 맞춰** 만들어집니다. 일반 Spring MVC와 같은 규칙입니다.

```java
// Validator
errors.rejectValue("amount", "min", new Object[] {1}, "The amount must be at least 1");
```

::: code-group

```properties [messages.properties]
# messages.properties
min.amount=금액은 {0}원 이상이어야 합니다.
required.customer=고객 이름을 입력하세요.
```

```yaml [messages.yml]
# messages.yml
min:
  amount: 금액은 {0}원 이상이어야 합니다.
required:
  customer: 고객 이름을 입력하세요.
```

:::

Bean Validation도 같습니다: `@NotBlank(message = "{required.customer}")`.

## `@ModelAttribute` (폼 필드로 보내기)

기존 Spring MVC 폼처럼 `@ModelAttribute`로 받고 싶다면 `encoding: 'form'`을 씁니다. 값이 `application/x-www-form-urlencoded`로 전송되고, 중첩 객체는 `address.city`, 목록은 `items[0].name`처럼 Spring 데이터 바인더가 읽는 이름이 됩니다.

```java
@ReactPage("orders/OrderForm")
@PostMapping("/orders/new")
public Object create(@Valid @ModelAttribute OrderForm form, BindingResult result) {
    if (result.hasErrors()) return new OrderFormPage(form);
    orders.add(form);
    return "redirect:/orders";
}
```

```tsx
const form = useSpringForm<OrderForm>({ initialValues: initial, action: '/orders/new', encoding: 'form' });
```

| 값 | 전송되는 필드 |
|---|---|
| `{ customer: '김민수' }` | `customer=김민수` |
| `{ amount: null }` | `amount=` (숫자 필드는 `null`로 바인딩) |
| `{ tags: ['a', 'b'] }` | `tags=a&tags=b` |
| `{ address: { city: 'Seoul' } }` | `address.city=Seoul` |
| `{ items: [{ name: 'pen' }] }` | `items[0].name=pen` |

이 변환은 `toFormData(values)`로 직접 쓸 수도 있습니다.

같은 핸들러는 JavaScript 없는 일반 HTML `<form method="post">`(Mustache 화면)도 받습니다. 에러가 있으면 페이지를 다시 그리고, 성공하면 303으로 이동합니다.

JSON과 폼 전송을 같은 URL에서 둘 다 받으려면 `consumes`로 나눕니다(예제 `OrderController`).

```java
@PostMapping(path = "/orders/new", consumes = MediaType.APPLICATION_JSON_VALUE)
public Object create(@Valid @RequestBody OrderForm form, BindingResult result) { ... }

@PostMapping(path = "/orders/new", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
public Object createFromForm(@Valid @ModelAttribute("orderForm") OrderForm form, BindingResult result) {
    return create(form, result);
}
```

## CSRF

Spring Security가 있으면 CSRF 토큰이 서버 컨텍스트에 들어옵니다.

| 보내는 방법 | 할 일 |
|---|---|
| `useSpringForm`, `springFetch` | 없음. 헤더로 자동 첨부 |
| 일반 `<form method="post">` (React) | hidden 필드로 넣기 (아래) |
| Mustache 화면의 일반 폼 | Spring Security의 요청 속성 `_csrf`: `spring.mustache.servlet.expose-request-attributes=true`로 켠 뒤 `<input type="hidden" name="{{_csrf.parameterName}}" value="{{_csrf.token}}">` |
| 직접 `fetch` | `csrfHeaders()`를 헤더에 펼치기 |

## 예: Spring Security 로그인 페이지

로그인 화면도 `@ReactPage`로 만들 수 있습니다. Spring Security의 `/login` 처리는 그대로 쓰고, 폼만 React로 그립니다(예제 `auth/Login`).

```java
@Bean
SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http.formLogin(form -> form.loginPage("/login").permitAll()).build();
}

@ReactPage(value = "auth/Login", title = "login.title")
@GetMapping("/login")
public LoginPage login(@RequestParam(name = "error", required = false) String error) {
    return new LoginPage(error != null);
}
```

```tsx
export default function Login({ error }: PageProps<'auth/Login'>) {
  const { csrf } = useServerContext();
  return (
    <form method="post" action="/login">
      {error && <p className="error">아이디 또는 비밀번호가 올바르지 않습니다.</p>}
      <input name="username" autoComplete="username" />
      <input name="password" type="password" autoComplete="current-password" />
      {csrf && <input type="hidden" name={csrf.parameterName} value={csrf.token} />}
      <button type="submit">로그인</button>
    </form>
  );
}
```

::::
