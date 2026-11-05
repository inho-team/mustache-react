# mustache-react

**Spring 개발자가 지금 쓰는 방식 그대로 React 생태계의 UI를 쓰게 해 주는 라이브러리입니다.**

컨트롤러가 화면을 정하고, DTO가 그대로 React 컴포넌트의 props가 됩니다.
Node, Vite 같은 프론트 빌드 도구는 Gradle/Maven 플러그인 뒤에 숨기고, 배포는 실행 JAR 하나로 끝납니다.

```java
@RestController
public class OrderController {

    @ReactPage("orders/OrderList")   // 브라우저로 접속하면 React 페이지, API 호출이면 JSON
    @GetMapping("/orders")
    public OrderListResponse list(Pageable pageable) {
        return orderService.list(pageable);
    }
}
```

```tsx
// src/main/frontend/pages/orders/OrderList.tsx
export default function OrderList({ content }: OrderListResponse) {
  return <DataGrid rows={content} />;
}
```

> **상태:** 개발 초기(M0, 뼈대 단계)입니다. 위 API는 목표 모습이며 아직 동작하지 않습니다.
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

예제 앱 실행:

```bash
./gradlew :examples:basic:run
```

자세한 내용은 [CONTRIBUTING.md](CONTRIBUTING.md)를 보세요.

## 라이선스

[Apache License 2.0](LICENSE)
