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

`src/main/frontend/package.json`:

```json
{
  "private": true,
  "type": "module",
  "dependencies": {
    "@mustache-react/client": "0.1.0",
    "react": "^19.3.1",
    "react-dom": "^19.3.1"
  },
  "devDependencies": {
    "@mustache-react/vite-plugin": "0.1.0",
    "@vitejs/plugin-react": "^6.2.1",
    "vite": "^8.3.6",
    "typescript": "^5.9.3"
  }
}
```

```
src/main/frontend/
├── package.json
├── pages/          @ReactPage("Hello") → pages/Hello.tsx
├── islands/        {{#react}}Chart{{/react}} → islands/Chart.tsx
├── app.tsx         (선택) 모든 페이지와 island를 감싸는 Provider
└── generated/      Java DTO에서 생성되는 types.ts
```

`vite.config.ts`는 두지 않아도 됩니다. 두면 그 설정이 쓰이고, `mustacheReact()` 플러그인을 넣어야 합니다.

## 3. 첫 페이지

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
