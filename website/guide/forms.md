# 폼과 서버 컨텍스트

## 서버 컨텍스트

모든 페이지에 서버 정보가 함께 내려옵니다.

```tsx
import { useServerContext, springFetch } from '@mustache-react/client';

const { user, csrf, locale, messages } = useServerContext();
await springFetch('/api/orders', { method: 'POST', body: JSON.stringify(order) });   // JSON + CSRF 헤더
```

| 키 | 내용 |
|---|---|
| `locale` | 요청 locale |
| `csrf` | `{headerName, parameterName, token}` (Spring Security) |
| `user` | `{name, authorities}`, 비로그인은 `null` (Spring Security) |
| `messages` | `mustache-react.context.messages`에 나열한 키(`orders.*` 패턴 가능)의 메시지. [다국어](./i18n) 참고 |
| `i18n` | `{param, locales}`: 언어 전환 설정 (`mustache-react.i18n.*`) |
| `errors` | 직전 폼 제출의 검증 에러 |

항목을 더하려면 `ServerContextContributor` 빈을 등록합니다. 페이지에 그대로 쓰이므로 비밀 값은 넣지 않습니다.

```java
@Bean
ServerContextContributor tenant(TenantResolver tenants) {
    return context -> context.put("tenant", tenants.current().getName());
}
```

일반 HTML 폼(예: Spring Security 로그인)에는 CSRF 토큰을 hidden 필드로 넣습니다.

```tsx
<form method="post" action="/login">
  ...
  {csrf && <input type="hidden" name={csrf.parameterName} value={csrf.token} />}
</form>
```

## 폼

```java
@ReactPage("orders/OrderForm")
@GetMapping("/orders/new")
public OrderFormPage newOrder() { return new OrderFormPage(new OrderForm()); }

@ReactPage("orders/OrderForm")
@PostMapping("/orders/new")
public Object create(@Valid @RequestBody OrderForm form, BindingResult result) {
    if (result.hasErrors()) return new OrderFormPage(form);
    orders.add(form);
    return "redirect:/orders";
}
```

```tsx
import { useSpringForm } from '@mustache-react/client';

export default function OrderForm({ form: initial }: PageProps<'orders/OrderForm'>) {
  const form = useSpringForm<OrderForm>({ initialValues: initial, action: '/orders/new' });
  return (
    <form onSubmit={form.submit}>
      <input {...form.field('customer')} />
      {form.errors.customer?.map((error) => <p key={error}>{error}</p>)}
      <button disabled={form.submitting}>등록</button>
    </form>
  );
}
```

| 결과 | `useSpringForm` (JSON) | 일반 폼 전송 |
|---|---|---|
| `"redirect:/orders"` | `{"redirect"}` → 이동 | 303 |
| 검증 에러 | 400 `{"errors": {fields, global}}` → 필드 옆에 표시 | 페이지를 다시 렌더링, 에러는 서버 컨텍스트로 |

- 에러 메시지는 `MessageSource`로 요청 locale에 맞춰집니다.
- `form.field(name)`은 `name`, `value`, `onChange`를 줍니다. 숫자 필드는 숫자로 유지됩니다.
- 성공 후 `redirect:` 없이 데이터를 반환하면 `onSuccess(body)`가 호출됩니다.
