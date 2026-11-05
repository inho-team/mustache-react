# mustache-react

**Spring 개발자가 지금 쓰는 방식 그대로 React 생태계의 UI를 쓰게 해 주는 라이브러리입니다.**

컨트롤러가 화면을 정하고, DTO가 그대로 React 컴포넌트의 props가 됩니다.
Node, Vite 같은 프론트 빌드 도구는 Gradle/Maven 플러그인 뒤에 숨기고, 배포는 실행 JAR 하나로 끝납니다.

```java
@RestController
public class OrderController {

    @ReactPage("orders/OrderList")   // 브라우저로 접속하면 React 페이지, API 호출이면 JSON
    @GetMapping("/orders")
    public OrderListResponse list() {
        return orderService.list();
    }
}
```

```tsx
// src/main/frontend/pages/orders/OrderList.tsx
import type { PageProps } from '../../generated/types';   // Java DTO에서 자동 생성

export default function OrderList({ orders, total }: PageProps<'orders/OrderList'>) {
  return <DataGrid rows={orders} />;
}
```

TS 타입은 annotation processor가 Java DTO에서 만듭니다. Java 필드 이름을 바꾸면 TS 쪽에서 컴파일 에러가 납니다.

같은 `/orders` URL을 브라우저로 열면 React 화면이, curl이나 `fetch`로 부르면 기존 JSON이 응답됩니다.

기존 Mustache 화면에는 필요한 부분만 React로 넣을 수 있습니다. 컨트롤러는 평소처럼 Model만 채우면 됩니다.

```mustache
{{#react}}OrderChart orders{{/react}}        {{! islands/OrderChart.tsx 에 model의 orders 전달 }}

{{#orders}}
  <td>{{customer}}</td>
  <td>{{#react}}StatusBadge status orderId=id{{/react}}</td>
{{/orders}}

{{{mr.scripts}}}
```

> **상태:** 개발 초기입니다. M3(`@ReactPage` 페이지, Mustache island, DTO → TS 타입 생성, Vite HMR 개발 모드)까지 동작하며, 아직 배포된 버전은 없습니다.
> 설계와 로드맵은 [docs/PLAN.md](docs/PLAN.md)에 있습니다.

## 지원 범위 (목표)

| | 지원 |
|---|---|
| Java | 8 이상 |
| Spring Boot | 2.7, 3.5, 4.x (Spring MVC) |
| 빌드 도구 | Gradle, Maven |
| React | 18 이상 |
| 렌더링 | CSR, SSR |

## 저장소 구조

| 경로 | 내용 |
|---|---|
| `java/` | Maven Central에 배포하는 Java 모듈 (`run.iknow:mustache-react-*`) |
| `js/` | npm에 배포하는 TypeScript 패키지 (`@mustache-react/*`) |
| `examples/basic` | Spring Boot 버전을 바꿔 가며 실행하는 예제 앱 |
| `build-logic/` | Gradle convention plugin |
| `docs/` | 설계 문서 |

## 개발

필요한 도구: JDK 17 이상(Gradle 실행용), Node 20 이상, pnpm 10.

```bash
./gradlew build
```

```bash
pnpm install && pnpm build && pnpm test
```

Spring Boot 버전과 테스트 JDK를 바꿔서 실행할 수 있습니다.

```bash
./gradlew build -PspringBootVersion=4.1.2 -PtestJavaVersion=21
```

예제 앱 실행 (프론트도 함께 빌드됩니다):

```bash
pnpm install
```

```bash
./gradlew :examples:basic:run
```

- <http://localhost:8080/orders>: `@ReactPage` 페이지. 같은 URL을 curl로 호출하면 JSON이 나옵니다.
- <http://localhost:8080/dashboard>: Mustache 화면에 React island(recharts 차트, 상태 배지)를 넣은 예

```bash
curl http://localhost:8080/orders
```

개발 모드에서는 컴포넌트 수정이 새로고침 없이 바로 반영되고(HMR), Mustache 템플릿 수정은 브라우저를 새로고침합니다. 터미널 두 개를 씁니다.

```bash
pnpm dev:example
```

```bash
./gradlew :examples:basic:run -PskipFrontend --args=--spring.profiles.active=dev
```

브라우저 e2e 테스트 (앱을 18080 포트로 띄운 상태에서):

```bash
BASE_URL=http://localhost:18080 pnpm e2e
```

개발 모드 e2e(HMR, 템플릿 새로고침)는 위 개발 모드 상태에서 `pnpm e2e:dev`로 실행합니다.

자세한 내용은 [CONTRIBUTING.md](CONTRIBUTING.md)를 보세요.

## 라이선스

[Apache License 2.0](LICENSE)
