# 시작하기

## 요구 사항

| | |
|---|---|
| Java | 8 이상 |
| Spring Boot | 2.7, 3.5, 4.x (Spring MVC) |
| 빌드 도구 | Gradle 7.6 이상 또는 Maven 3.6 이상 |
| Node.js | 필요 없음. 빌드 플러그인이 `.mustache-react/node`에 내려받습니다 |

## 1. 플러그인과 starter

::: code-group

```kotlin [Gradle (build.gradle.kts)]
plugins {
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    id("run.iknow.mustache-react") version "0.1.0"
    java
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

starter가 Spring MVC(`spring-boot-starter-web`)와 Mustache를 함께 가져옵니다. Maven에서 TypeScript 타입을 생성하려면 [타입 생성](./types#maven) 설정을 추가합니다.

`.gitignore`에 `.mustache-react/`(내려받은 Node.js)를 추가합니다.

## 2. 프론트엔드 디렉터리

따로 만들 필요가 없습니다. 첫 빌드 때 `src/main/frontend/`에 `package.json`이 없으면 플러그인이 다음 파일을 만듭니다.
- `package.json`: React, Vite, 플러그인과 같은 버전의 `@mustache-react/*`
- `tsconfig.json`, `.gitignore`
- 빈 `pages/`, `islands/`

만들어진 파일은 커밋하세요. 이미 있는 파일은 건드리지 않습니다. React 라이브러리를 더 쓰려면 `package.json`의 `dependencies`에 추가합니다.

```
src/main/frontend/
├── package.json
├── pages/          @ReactPage("Hello") → pages/Hello.tsx
├── islands/        Mustache의 <react:Chart/> → islands/Chart.tsx
├── app.tsx         (선택) 모든 페이지와 island를 감싸는 Provider
└── generated/      Java DTO에서 생성되는 types.ts
```

`vite.config.ts`는 두지 않아도 됩니다. 두면 그 설정이 쓰이고, `mustacheReact()` 플러그인을 넣어야 합니다.

## 3. 첫 화면

화면을 만드는 방법은 두 가지이고, 한 애플리케이션에서 섞어 써도 됩니다.

- **HTML 템플릿 + React 부품:** Mustache로 화면을 쓰고, 필요한 곳에만 React 컴포넌트를 태그로 넣습니다. [Mustache island](./islands) 참고.
- **페이지 전체를 React로:** 컨트롤러 반환값이 props가 되는 `@ReactPage`입니다. 아래 예제가 이 방식입니다.

### `@ReactPage` 예제

```java
@RestController
public class HelloController {

    public record Hello(String message, List<String> frameworks) {}

    @ReactPage("Hello")
    @GetMapping("/")
    public Hello hello() {
        return new Hello("Hello from Spring", List.of("Spring Boot", "React"));
    }
}
```

```tsx
// src/main/frontend/pages/Hello.tsx
import type { PageProps } from '../generated/types';

export default function Hello({ message, frameworks }: PageProps<'Hello'>) {
  return (
    <main>
      <h1>{message}</h1>
      <ul>{frameworks.map((name) => <li key={name}>{name}</li>)}</ul>
    </main>
  );
}
```

## 4. 실행과 배포

```bash
./gradlew bootRun        # 또는 mvn spring-boot:run
./gradlew bootJar        # 또는 mvn package: 프론트가 포함된 실행 jar 하나
```

`http://localhost:8080/`을 브라우저로 열면 React 페이지가, `curl`로 부르면 JSON이 나옵니다.

개발 중에는 [개발 모드](./development)로 실행하면 코드 수정이 바로 반영됩니다.
