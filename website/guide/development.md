# 개발 모드

개발 중에는 프론트를 매번 빌드하지 않고 **Vite 개발 서버**에서 바로 불러옵니다. 컴포넌트를 저장하면 브라우저에 즉시 반영됩니다(HMR).

## 설정

::: code-group

```properties [application-dev.properties]
# src/main/resources/application-dev.properties
mustache-react.dev-server.enabled=true

# Mustache 템플릿을 jar가 아니라 소스에서 읽어, 고치면 바로 반영
spring.mustache.prefix=file:src/main/resources/templates/
spring.mustache.servlet.cache=false
```

```yaml [application-dev.yml]
# src/main/resources/application-dev.yml
mustache-react:
  dev-server:
    enabled: true

# Mustache 템플릿을 jar가 아니라 소스에서 읽어, 고치면 바로 반영
spring:
  mustache:
    prefix: file:src/main/resources/templates/
    servlet:
      cache: false
```

:::

`messages.properties`는 개발 모드에서 따로 설정하지 않아도 재시작 없이 반영됩니다. Spring의 메시지 캐시가 1초로 줄고(`spring.messages.cache-duration`을 직접 두면 그 값을 씁니다), `orders.*` 같은 패턴도 페이지마다 다시 펼칩니다. 단, 고친 파일이 클래스패스에 복사되어야 합니다(IDE의 빌드, 또는 `./gradlew processResources`).

## 실행

```bash
./gradlew bootRun --args=--spring.profiles.active=dev
```

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

IDE에서는 실행 설정의 Active profiles에 `dev`를 넣고 메인 클래스를 실행합니다.

프론트 빌드는 필요 없으므로 `-PmustacheReact.skipFrontend`(Gradle) / `-DmustacheReact.skipFrontend`(Maven)로 건너뛰면 시작이 빨라집니다. 처음 한 번은 의존성 설치(`node_modules`)가 필요하니 일반 빌드를 한 번 돌려 두세요.

## 무엇이 자동으로 되나

| 일 | 동작 |
|---|---|
| Vite 개발 서버 | 애플리케이션이 시작할 때 함께 띄우고, 종료할 때 함께 끕니다. 이미 그 포트(기본 5173)에 떠 있으면 그것을 씁니다 |
| 컴포넌트 수정 (`.tsx`, `.css`) | 새로고침 없이 반영(HMR). 입력 중인 값 같은 React 상태도 유지됩니다 |
| Mustache 템플릿 수정 | 브라우저가 자동으로 새로고침 (`src/main/resources/templates` 감시) |
| 서버 렌더링 | `ssr=auto`면 Vite가 현재 소스로 서버 렌더링 |
| Java 코드 수정 | 평소처럼 재시작(또는 Spring DevTools). `types.ts`는 컴파일할 때 다시 생성되고, 바뀌면 Vite가 반영 |
| Node.js | 빌드 플러그인이 내려받은 `.mustache-react/node`, 없으면 `PATH`의 `node` |

island나 페이지가 마운트에 실패하면 개발 모드에서는 그 자리에 빨간 오류 상자가 나옵니다(운영에서는 콘솔에만 기록).

## 설정값

| 설정 | 기본값 | 설명 |
|---|---|---|
| `mustache-react.dev-server.enabled` | `false` | 개발 서버 사용. **개발 프로필에만** 켜세요 |
| `mustache-react.dev-server.url` | `http://localhost:5173` | 개발 서버 주소. 포트를 바꾸면 그 포트로 띄웁니다 |
| `mustache-react.dev-server.auto-start` | `true` | `false`면 직접 띄운 개발 서버를 씁니다 |
| `mustache-react.dev-server.node` | (자동) | 쓸 `node` 실행 파일 경로 |
| `mustache-react.dev-server.node-install-dir` | `.mustache-react/node` | 플러그인이 Node.js를 내려받은 곳 |
| `mustache-react.dev-server.startup-timeout` | `30s` | 개발 서버가 뜨기를 기다리는 시간 |
| `mustache-react.frontend-dir` | `src/main/frontend` | 프론트 소스 위치 (Gradle `bootRun`은 절대 경로를 넘겨 줌) |

개발 서버를 직접 띄우려면 프론트 디렉터리에서 실행하고 `auto-start=false`로 둡니다.

```bash
cd src/main/frontend && npx mustache-react-vite dev
```

## 재시작이 필요한 것

| 바꾼 것 | 반영 |
|---|---|
| 컴포넌트, CSS | 즉시 (HMR) |
| Mustache 템플릿 | 즉시 (위 `spring.mustache.*` 설정 시 새로고침) |
| `messages.properties` 내용, 새 키 | 파일이 클래스패스에 복사된 뒤 1초 안에 다음 요청부터 (재시작 불필요) |
| `@ReactPage` 레이아웃 (`layout.mustache`) | 새로고침 (개발 모드에서는 페이지마다 다시 읽음) |
| `package.json` (라이브러리 추가) | 의존성 설치 후 재시작 |
| Java 코드, `application.properties` | 재시작 |

## 자주 보는 문제

| 증상 | 원인과 해결 |
|---|---|
| `cannot start the Vite dev server: ... not found` | `node_modules`가 없음. 일반 빌드(`./gradlew build`)를 한 번 돌려 의존성을 설치하세요 |
| `could not start the Vite dev server with 'node'` | Node.js를 못 찾음. 빌드를 한 번 돌리면 플러그인이 내려받습니다 |
| `did not listen on localhost:5173` | 시작이 느리거나 실패. 로그 위쪽의 Vite 오류를 확인하고 `startup-timeout`을 늘리세요 |
| 다른 프로젝트 화면이 뜸 | 5173 포트에 다른 프로젝트의 Vite 개발 서버가 떠 있어 그것을 씀. 끄거나 `dev-server.url` 포트를 바꾸세요 |
| `port 5173 is used by another program, not by a Vite dev server` | Vite가 아닌 프로그램이 그 포트를 씀. 그 프로그램을 끄거나 `dev-server.url` 포트를 바꾸세요 |
| 화면이 비고 콘솔에 `No component file for "pages/..."` | `@ReactPage` 이름과 파일 경로가 다름 |
| 운영 jar에서 화면이 안 뜸 | `dev-server.enabled=true`가 기본 설정에 들어감. 개발 프로필로 옮기세요 |
