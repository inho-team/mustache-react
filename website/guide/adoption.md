# 기존 프로젝트에 도입하기

:::: v-pre

이미 Spring MVC + Mustache로 운영 중인 서비스에 조금씩 도입하는 순서입니다. 기존 화면과 API는 그대로 두고, 한 화면의 한 부분부터 시작합니다.

## 0. 준비 확인

| 항목 | 조건 |
|---|---|
| Java | 8 이상 |
| Spring Boot | 2.7, 3.5, 4.x, Spring MVC (WebFlux 미지원) |
| 템플릿 | Mustache (`spring-boot-starter-mustache`) |
| 빌드 | Gradle 7.6 이상 또는 Maven 3.6 이상 |
| Node.js | 설치 불필요. 빌드 플러그인이 내려받습니다 (빌드 서버에서 `nodejs.org`, npm 레지스트리 접근 필요) |

## 1. 의존성과 빌드 플러그인 추가

::: code-group

```kotlin [Gradle (build.gradle.kts)]
plugins {
    id("run.iknow.mustache-react") version "0.1.0"
}

dependencies {
    implementation("run.iknow:mustache-react-spring-boot-starter:0.1.0")
}
```

```xml [Maven (pom.xml)]
<dependency>
  <groupId>run.iknow</groupId>
  <artifactId>mustache-react-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>

<!-- build/plugins -->
<plugin>
  <groupId>run.iknow</groupId>
  <artifactId>mustache-react-maven-plugin</artifactId>
  <version>0.1.0</version>
  <executions><execution><goals><goal>build</goal></goals></execution></executions>
</plugin>
```

:::

Maven에서 TypeScript 타입 생성을 쓰려면 [타입 생성](./types#maven)의 컴파일러 설정도 추가합니다. Gradle은 플러그인이 알아서 설정합니다.

`.gitignore`에 추가합니다.

```
.mustache-react/
src/main/frontend/node_modules/
```

빌드를 한 번 돌립니다.

```bash
./gradlew build
```

`src/main/frontend/`에 `package.json`, `tsconfig.json`, `.gitignore`, 빈 `pages/`, `islands/`가 생깁니다. 이 파일들과 lock 파일을 커밋합니다. **이 단계까지는 기존 화면에 아무 변화가 없습니다.** 기존 컨트롤러, Mustache 뷰, API는 그대로 동작합니다.

## 2. 첫 island: 기존 화면의 한 부분만 React로

인터랙션이 필요한 작은 부분 하나를 고릅니다. 검색 필터, 차트, 상태 배지, 날짜 선택기 같은 것이 좋습니다.

```tsx
// src/main/frontend/islands/StatusBadge.tsx
import { useState } from 'react';

export default function StatusBadge({ status, orderId }: { status: string; orderId: number }) {
  const [open, setOpen] = useState(false);
  return (
    <button type="button" onClick={() => setOpen(!open)}>
      {status}
      {open && ` · 주문 #${orderId}`}
    </button>
  );
}
```

기존 템플릿에서 그 부분만 바꿉니다. 컨트롤러는 손대지 않습니다.

```mustache
<head>
  ...
  <react:head />
</head>
<body>
  ...
  {{#orders}}
    <tr>
      <td>{{customer}}</td>
      <td><react:StatusBadge status="{{status}}" order-id="{{id}}" /></td>  {{! 바뀐 부분 }}
    </tr>
  {{/orders}}
  ...
  <react:scripts />
</body>
```

공통 레이아웃 partial(`{{> layout/head}}` 등)이 있다면 `<react:head />`와 `<react:scripts />`를 거기에 한 번만 넣으세요. island가 없는 화면에서도 무해합니다.

## 3. 개발 환경

::: code-group

```properties [application-dev.properties]
# src/main/resources/application-dev.properties
mustache-react.dev-server.enabled=true
spring.mustache.prefix=file:src/main/resources/templates/
spring.mustache.servlet.cache=false
```

```yaml [application-dev.yml]
# src/main/resources/application-dev.yml
mustache-react:
  dev-server:
    enabled: true
spring:
  mustache:
    prefix: file:src/main/resources/templates/
    servlet:
      cache: false
```

:::

```bash
./gradlew bootRun --args=--spring.profiles.active=dev
```

컴포넌트를 고치면 바로 반영되고, 템플릿을 고치면 브라우저가 새로고침합니다. [개발 모드](./development) 참고.

## 4. island props를 Java로 선언 (권장)

island가 늘어나면 템플릿과 컴포넌트 사이 props를 Java 클래스로 고정합니다. 템플릿의 오타는 렌더링할 때, 컴포넌트의 오타는 TypeScript 컴파일할 때 잡힙니다.

```java
@ReactIsland("StatusBadge")
public class StatusBadgeProps {
    private OrderStatus status;
    private long orderId;
    // getter
}
```

```tsx
export default function StatusBadge({ status, orderId }: IslandProps<'StatusBadge'>) { ... }
```

## 5. 화면 전체를 React로: `@ReactPage`

상호작용이 많은 화면(복잡한 폼, 대시보드)은 화면 전체를 React로 만드는 편이 낫습니다. 이미 JSON을 돌려주는 API 컨트롤러가 있다면 애너테이션 한 줄이면 됩니다.

```java
@ReactPage(value = "orders/OrderList", title = "orders.title")
@GetMapping("/orders")
public OrderListResponse list() { ... }   // 기존 API 그대로
```

- 같은 `/orders`가 브라우저에는 React 화면, API 클라이언트(`Accept: application/json`)에는 지금까지와 같은 JSON을 돌려줍니다. **기존 API 사용자에게 영향이 없습니다.**
- 기존 Mustache 화면이 같은 URL을 쓰고 있었다면, 새 화면을 다른 URL로 먼저 만들어 확인한 뒤 바꾸세요.
- 공통 헤더·푸터가 Mustache partial이라면 `templates/mustache-react/layout.mustache`에서 그 partial을 그대로 씁니다. [@ReactPage 페이지](./pages) 참고.

## 6. 운영 반영

- 처음에는 서버 렌더링 없이(`mustache-react.ssr=off`, 기본값) 배포하는 것을 권합니다. 운영 서버에 Node.js가 없어도 됩니다.
- 첫 화면 표시 속도나 검색 노출이 중요해지면 SSR을 켭니다. 운영 서버에 `node`가 필요합니다. [서버 렌더링](./ssr) 참고.
- jar 하나로 배포됩니다. CDN을 쓰고 싶다면 [배포](./deployment) 참고.
- CI 빌드 서버가 `nodejs.org`와 npm 레지스트리에 접근할 수 있는지 확인하세요. 사내 미러가 있다면 `nodeDistributionUrl`과 `.npmrc`를 설정합니다.

## 되돌리기

도입을 취소하려면 역순으로 합니다.

1. `<react:...>` 태그와 `{{#react}}` 섹션을 원래 Mustache 마크업으로 되돌립니다.
2. `@ReactPage`, `@ReactIsland`, `@ReactType` 애너테이션을 지웁니다. `@ReactPage`를 지운 컨트롤러는 원래 JSON API로 그대로 동작합니다.
3. starter 의존성과 빌드 플러그인, `src/main/frontend/`, `mustache-react.*` 설정을 지웁니다.

기존 컨트롤러와 서비스 코드는 바꾼 적이 없으므로 되돌릴 것이 없습니다.

::::
