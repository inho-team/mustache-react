# Thymeleaf

::: v-pre

화면은 평소처럼 Thymeleaf로 쓰고, 인터랙션이 필요한 부분만 React 컴포넌트를 `<react:이름>` 태그로 넣습니다. 컨트롤러, `th:each`, `#{메시지}`, Spring Security 연동은 지금까지 하던 대로 씁니다.

## 설정

`spring-boot-starter-thymeleaf`만 추가하면 됩니다. mustache-react가 Thymeleaf를 찾으면 `react` 방언을 자동으로 등록합니다.

```kotlin
dependencies {
    implementation("run.iknow:mustache-react-spring-boot-starter:0.1.0")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
}
```

## 템플릿

```html
<!doctype html>
<html xmlns:th="http://www.thymeleaf.org" xmlns:react="https://iknow.run/mustache-react">
<head>
  <title th:text="#{board.title}">주문 보드</title>
  <react:head />                      <!-- CSS (app.css 등) -->
</head>
<body>
  <h1 th:text="#{board.title}">주문 보드</h1>

  <react:OrderChart orders="${orders}" currency="원" />

  <table>
    <tr th:each="order : ${orders}">
      <td th:text="${order.customer}">김민수</td>
      <td><react:StatusBadge status="${order.status}" order-id="${order.id}" /></td>
    </tr>
  </table>

  <react:scripts />                   <!-- 마지막 island 뒤에: React를 붙이는 스크립트 -->
</body>
</html>
```

```java
@Controller
public class BoardController {
    @GetMapping("/board")
    public String board(Model model) {
        model.addAttribute("orders", orders.findAll());
        model.addAttribute("currency", "원");
        return "board";
    }
}
```

```tsx
// src/main/frontend/islands/StatusBadge.tsx
export default function StatusBadge({ status, orderId }: { status: string; orderId: number }) {
  const [open, setOpen] = useState(false);
  return <button onClick={() => setOpen(!open)}>{status}{open && ` · #${orderId}`}</button>;
}
```

## 태그 규칙

| 쓰는 법 | 의미 |
|---|---|
| `<react:OrderChart ... />` | `islands/OrderChart.tsx`. `<react:order-chart>`도 같습니다 |
| `<react:island component="charts/OrderChart" ... />` | 하위 디렉터리의 컴포넌트 (`islands/charts/OrderChart.tsx`) |
| `orders="${orders}"` | Thymeleaf 식(`${}`, `*{}`, `#{}`, `@{}`)은 계산한 **객체 그대로**(리스트, DTO, 숫자) props로 전달 |
| `currency="원"` | 그 밖의 값은 문자열 |
| `order-id="..."` | props 이름은 camelCase로: `orderId` |
| `open` (값 없음) | `true` |
| `th:if`, `th:each`, `th:with` | 태그에 그대로 씁니다. `th:each`면 반복마다 island가 하나씩 생깁니다 |
| `<react:head />` | `<head>` 안. 엔트리 CSS |
| `<react:scripts />` | 마지막 island 뒤. 서버 컨텍스트, island 미리 불러오기, 진입 스크립트 |

props는 Spring MVC의 JSON 변환기(Jackson)로 직렬화되고, `</script>` 같은 내용도 안전하게 이스케이프됩니다. 태그 안의 내용은 쓰이지 않으니 `<react:Name ... />`처럼 닫아 주세요.

## 함께 되는 것

- **서버 렌더링:** `mustache-react.ssr=auto`면 island도 서버에서 HTML로 그린 뒤 브라우저에서 이어받습니다(hydration).
- **다국어:** 템플릿은 `#{...}`, island 안에서는 `useMessages()`로 같은 `messages.properties`를 씁니다. [다국어](./i18n) 참고.
- **CSRF, 로그인 사용자:** island 안에서 `useServerContext()`로 받습니다.
- **같은 앱에서 섞어 쓰기:** Mustache 화면, Thymeleaf 화면, `@ReactPage` 페이지를 함께 쓸 수 있습니다.

접두어를 바꾸려면 `mustache-react.template.thymeleaf-prefix=ui`처럼 설정합니다(`<ui:OrderChart>`).

## props 타입 선언 (`@ReactIsland`)

island props를 Java 클래스로 선언하면 템플릿 양쪽이 검사됩니다.

```java
@ReactIsland("StatusBadge")
public class StatusBadgeProps {
    private OrderStatus status;
    private long orderId;
    @Nullable private String note;     // 생략 가능한 prop
    // getter
}
```

```tsx
// islands/StatusBadge.tsx
import type { IslandProps } from '../generated/types';
export default function StatusBadge({ status, orderId }: IslandProps<'StatusBadge'>) { ... }
```

- **React 쪽:** `IslandProps<'StatusBadge'>` 타입이 `types.ts`에 생성되어 컴포넌트 코드가 타입 체크됩니다.
- **템플릿 쪽:** 렌더링할 때 템플릿이 넘긴 props를 확인합니다. 오타(`statsu`)나 빠진 필수 prop이 있으면 화면이 에러로 멈추고 이렇게 알려줍니다: `island StatusBadge: unknown prop 'statsu', missing prop 'status'. Its @ReactIsland props are status, orderId, note (optional).`

선언하지 않은 island는 지금처럼 아무 props나 받습니다.

:::
