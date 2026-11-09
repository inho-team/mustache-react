# Mustache island

::: v-pre

기존 Mustache 화면에 필요한 부분만 React로 넣습니다. 컨트롤러는 평소처럼 Model만 채웁니다.

```mustache
<head> ... {{{mr.head}}} </head>
<body>
  {{#react}}OrderChart orders currency{{/react}}

  {{#orders}}
    <td>{{customer}}</td>
    <td>{{#react}}StatusBadge status orderId=id{{/react}}</td>
  {{/orders}}

  {{{mr.scripts}}}
</body>
```

| 문법 | 의미 |
|---|---|
| `{{#react}}이름 a b{{/react}}` | `islands/이름.tsx`에 Model의 `a`, `b`를 같은 이름의 props로 |
| `{{#react}}이름 x=a.b{{/react}}` | 경로 `a.b`의 값을 `x`로 |
| `{{#react}}이름 item=this{{/react}}` | 현재 루프 항목 전체를 `item`으로 |

- 값은 Mustache 규칙대로 찾습니다: 루프 항목 → 바깥 섹션 → Model. `Map` 키, record 접근자, getter, public 필드를 읽습니다.
- 없는 이름은 템플릿 오류로 렌더링을 멈추고 `model.addAttribute(...)`를 안내합니다. 값이 `null`이면 `null` prop입니다.
- `{{{mr.scripts}}}`는 **마지막 island 뒤**에 둡니다. 페이지에서 쓴 island의 파일만 미리 받습니다.
- `react`, `mr`는 모든 Model에 자동으로 들어갑니다. 이름은 `mustache-react.template.react-name`, `helpers-name`으로 바꿉니다.
- island를 감싸는 요소는 `<div>`입니다. `<p>` 안에는 넣지 않습니다.

island props용 DTO에는 `@ReactType`을 붙이면 TypeScript 타입이 생성됩니다.

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
