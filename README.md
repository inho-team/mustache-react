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

> **상태:** 개발 초기입니다. M4(`@ReactPage` 페이지, Mustache island, DTO → TS 타입 생성, Vite HMR 개발 모드, Gradle/Maven 플러그인)까지 동작합니다. 아직 Maven Central과 npm에 배포된 버전은 없습니다(M6 예정).
> 설계와 로드맵은 [docs/PLAN.md](docs/PLAN.md)에 있습니다.

## 시작하기

> 배포 전이라 아래 좌표는 아직 받을 수 없습니다. 지금은 저장소의 `examples/basic`으로 확인할 수 있습니다.

**1. 빌드 플러그인과 starter를 추가합니다.** npm을 직접 실행하거나 Vite를 설정할 필요가 없습니다.

```kotlin
// build.gradle.kts
plugins {
    id("org.springframework.boot") version "…"
    id("run.iknow.mustache-react") version "…"
}
dependencies {
    implementation("run.iknow:mustache-react-spring-boot-starter:…")
}
```

<details>
<summary>Maven</summary>

```xml
<dependency>
  <groupId>run.iknow</groupId>
  <artifactId>mustache-react-spring-boot-starter</artifactId>
  <version>…</version>
</dependency>

<plugin>
  <groupId>run.iknow</groupId>
  <artifactId>mustache-react-maven-plugin</artifactId>
  <version>…</version>
  <executions><execution><goals><goal>build</goal></goals></execution></executions>
</plugin>
```

TS 타입을 생성하려면 `maven-compiler-plugin`에 `mustache-react-processor`를 `annotationProcessorPaths`로 추가하고 `-Amustachereact.typesFile=${project.basedir}/src/main/frontend/generated/types.ts`를 넘깁니다. 자세한 설정은 [examples/basic/pom.xml](examples/basic/pom.xml)을 참고하세요.
</details>

**2. `src/main/frontend`에 `package.json`과 페이지 컴포넌트를 둡니다.**

```json
{
  "private": true,
  "dependencies": { "@mustache-react/client": "…", "react": "^19", "react-dom": "^19" },
  "devDependencies": { "@mustache-react/vite-plugin": "…", "@vitejs/plugin-react": "^6", "vite": "^8", "typescript": "^5" }
}
```

**3. 빌드와 실행은 평소대로 합니다.**
- `./gradlew bootJar` 또는 `mvn package` 한 번이면 다음이 모두 끝나고, 결과는 실행 jar 하나입니다.
  - Node.js를 `.mustache-react/node`에 내려받습니다.
  - 의존성을 설치하고 프론트를 빌드합니다.
  - Java DTO에서 TS 타입을 생성합니다.
- 개발할 때는 `mustache-react.dev-server.enabled=true` 프로파일로 `bootRun`/`spring-boot:run`을 실행합니다. Spring이 Vite dev server를 함께 띄우므로 컴포넌트 수정이 바로 반영됩니다(HMR).

`.mustache-react/`는 `.gitignore`에 추가하세요.

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
| `build-plugins/` | Gradle 플러그인, Maven 플러그인, 둘이 함께 쓰는 빌드 도구 (별도 Gradle 빌드) |
| `examples/basic` | 예제 앱. 같은 소스를 Gradle과 Maven으로 빌드하고 Spring Boot 버전을 바꿔 가며 실행 |
| `build-logic/` | Gradle convention plugin |
| `docs/` | 설계 문서 |

## 개발

필요한 도구: JDK 17 이상(Gradle 실행용), Node 20 이상, pnpm 10.

```bash
./gradlew build
```

```bash
./gradlew -p build-plugins build
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

개발 모드에서는 Spring이 Vite dev server를 함께 띄웁니다. 컴포넌트 수정은 새로고침 없이 바로 반영되고(HMR), Mustache 템플릿 수정은 브라우저를 새로고침합니다.

```bash
./gradlew :examples:basic:run -PmustacheReact.skipFrontend --args=--spring.profiles.active=dev
```

같은 예제를 Maven으로 빌드할 수도 있습니다. 먼저 mustache-react를 로컬 Maven 저장소에 배포합니다.

```bash
./gradlew publishToMavenLocal && ./gradlew -p build-plugins publishToMavenLocal
```

```bash
cd examples/basic && mvn package
```

브라우저 e2e 테스트 (앱을 18080 포트로 띄운 상태에서):

```bash
BASE_URL=http://localhost:18080 pnpm e2e
```

개발 모드 e2e(HMR, 템플릿 새로고침)는 위 개발 모드 상태에서 `pnpm e2e:dev`로 실행합니다.

자세한 내용은 [CONTRIBUTING.md](CONTRIBUTING.md)를 보세요.

## 라이선스

[Apache License 2.0](LICENSE)
