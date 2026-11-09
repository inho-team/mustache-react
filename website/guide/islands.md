# Mustache island

::: v-pre

기존 Mustache 화면은 그대로 두고, 인터랙션이 필요한 부분(차트, 필터, 버튼, 실시간 배지)만 React 컴포넌트로 넣는 방식입니다. 이렇게 화면 안에 박힌 작은 React 영역을 **island**라고 부릅니다. 컨트롤러는 평소처럼 Model만 채우고 뷰 이름을 반환합니다.

```java
@Controller
public class DashboardController {
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("orders", orders.findAll());
        model.addAttribute("currency", "원");
        return "dashboard";   // templates/dashboard.mustache
    }
}
```

React 컴포넌트는 템플릿에 HTML 태그처럼 씁니다.

```mustache
<head> ... <react:head /> </head>
<body>
  <react:OrderChart orders="{{orders}}" currency="원" />

  {{#orders}}
    <td>{{customer}}</td>
    <td><react:StatusBadge status="{{status}}" order-id="{{id}}" /></td>
  {{/orders}}

  <react:scripts />
</body>
```

| 쓰는 법 | 의미 |
|---|---|
| `<react:OrderChart ... />` | `islands/OrderChart.tsx`. `<react:order-chart>`도 같습니다 |
| `orders="{{orders}}"` | 값이 `{{변수}}` 하나뿐이면 Model 값을 **객체 그대로**(리스트, DTO, 숫자) props로 |
| `currency="원"`, `title="주문 {{id}}"` | 그 밖의 값은 문자열 (`{{...}}`은 채워서) |
| `order-id="..."` | props 이름은 camelCase로: `orderId` |
| `open` (값 없음) | `true` |
| `<react:island component="charts/OrderChart" ... />` | 하위 디렉터리의 컴포넌트 |
| `<react:head />` | `<head>` 안. 엔트리 CSS (`{{{mr.head}}}`와 같음) |
| `<react:scripts />` | **마지막 island 뒤**. 서버 컨텍스트와 진입 스크립트 (`{{{mr.scripts}}}`와 같음) |

- 값은 Mustache 규칙대로 찾습니다: 루프 항목 → 바깥 섹션 → Model. `Map` 키, record 접근자, getter, public 필드를 읽습니다.
- 없는 이름은 템플릿 오류로 렌더링을 멈춥니다. 값이 `null`이면 `null` prop입니다.
- 부분 템플릿(`{{> header}}`)과 `@ReactPage` 레이아웃에서도 같은 태그를 씁니다.
- island를 감싸는 요소는 `<div>`입니다. `<p>` 안에는 넣지 않습니다.
- 주의: Mustache 주석 `{{! ... }}`은 처음 나오는 `}}`에서 끝나므로, 주석 안에 `{{변수}}`를 쓰지 마세요.

### 섹션 문법

태그 대신 Mustache 섹션으로도 쓸 수 있습니다. 결과는 같습니다.

| 문법 | 의미 |
|---|---|
| `{{#react}}이름 a b{{/react}}` | `islands/이름.tsx`에 Model의 `a`, `b`를 같은 이름의 props로 |
| `{{#react}}이름 x=a.b{{/react}}` | 경로 `a.b`의 값을 `x`로 |
| `{{#react}}이름 item=this{{/react}}` | 현재 루프 항목 전체를 `item`으로 |

`react`, `mr`는 모든 Model에 자동으로 들어갑니다. 이름은 `mustache-react.template.react-name`(태그 접두어도 이것을 따릅니다), `helpers-name`으로 바꿉니다. 태그 문법은 `mustache-react.template.html-tags=false`로 끌 수 있습니다.

## 컴포넌트 파일

```tsx
// src/main/frontend/islands/StatusBadge.tsx
import { useState } from 'react';
import type { IslandProps } from '../generated/types';

export default function StatusBadge({ status, orderId }: IslandProps<'StatusBadge'>) {
  const [open, setOpen] = useState(false);
  return (
    <button type="button" onClick={() => setOpen(!open)}>
      {status}{open && ` · 주문 #${orderId}`}
    </button>
  );
}
```

- `islands/` 아래 파일이고 **default export**여야 합니다. 하위 폴더는 `<react:island component="charts/OrderChart" />`로 씁니다.
- props는 템플릿이 넘긴 값입니다. 타입은 [`@ReactIsland`](#props-타입-선언-reactisland)로 선언하면 생성됩니다.

## 여러 island와 반복문

- 한 화면에 island를 몇 개든 둘 수 있습니다. 같은 컴포넌트를 여러 번 써도 됩니다.
- `{{#orders}} ... {{/orders}}` 안의 island는 반복마다 하나씩 생기고, 값은 **현재 항목부터** 찾습니다. 위 예에서 `{{status}}`는 그 줄 주문의 상태입니다.
- 현재 항목 전체를 넘기려면 `order="{{this}}"`(섹션 문법은 `item=this`)를 씁니다.
- 바깥 값도 그대로 찾을 수 있습니다. 반복 안에서 `currency="{{currency}}"`는 항목에 없으면 Model의 `currency`입니다.

## 불러오는 방식

1. 서버가 island 자리에 `<div data-mr-island="islands/StatusBadge">`와 props JSON을 씁니다. 서버 렌더링이 켜져 있으면 그 안에 컴포넌트를 HTML로 미리 그려 넣습니다.
2. `<react:scripts />`(또는 `{{{mr.scripts}}}`)가 서버 컨텍스트와 엔트리 스크립트를 쓰고, **이 페이지에서 쓴 island의 파일만** 미리 받도록 표시합니다.
3. 브라우저에서 엔트리 스크립트가 각 island 파일을 따로 불러와 그 자리에 마운트합니다. 서버에서 그려진 island는 hydration, 아니면 새로 렌더링합니다.
4. 한 island가 실패해도 다른 island는 정상 동작합니다. 실패는 브라우저 콘솔에 남고, 개발 모드에서는 그 자리에 오류 상자가 나옵니다.

그래서 `<react:scripts />`는 **마지막 island보다 뒤**(보통 `</body>` 바로 앞)에 둡니다. 앞에 두면 그 뒤의 island 파일은 미리 받지 못합니다(동작은 하지만 느려집니다).

## island끼리 상태 공유

각 island는 따로 마운트된 독립된 React 트리입니다. `useState`나 Context는 island 사이에 공유되지 않습니다. 함께 움직여야 하면:

| 방법 | 언제 |
|---|---|
| 서버가 같은 값을 둘 다에 props로 넘기기 | 처음 값만 같으면 될 때 |
| 공유 모듈(`components/store.ts`)의 상태 라이브러리(zustand, jotai 등) | 클릭에 따라 서로 바뀌어야 할 때. 같은 번들이라 모듈 상태가 공유됩니다 |
| URL 쿼리(`?status=PAID`) + 페이지 새로고침 | 필터처럼 서버 데이터가 바뀌어야 할 때 |
| `window.dispatchEvent(new CustomEvent(...))` | 아주 단순한 알림 |

여러 island가 긴밀하게 얽힌 화면이라면 화면 전체를 [`@ReactPage`](./pages)로 만드는 편이 낫습니다.

## 스타일

- `app.tsx`에서 `import './app.css'`처럼 불러온 CSS는 모든 화면에 들어가고 `<react:head />`가 `<link>`로 씁니다.
- 컴포넌트 파일에서 불러온 CSS(`import './StatusBadge.css'`)는 그 island를 쓰는 화면에서만 불러옵니다.
- Mustache 화면의 기존 CSS도 island 안 요소에 그대로 적용됩니다.

## 서버 컨텍스트와 다국어

island 안에서도 `useServerContext()`(로그인 사용자, CSRF), `useMessages()`(메시지)를 쓸 수 있습니다. 값은 `<react:scripts />`가 함께 내려줍니다. [폼과 서버 컨텍스트](./forms), [다국어](./i18n) 참고.

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
