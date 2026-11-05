# mustache-react 설계 계획

> 상태: 초안 (v0.1) · 공개 오픈소스 프로젝트

## 1. 한 줄 정의

**Spring 컨트롤러가 Mustache로 페이지 틀과 데이터를 내려주면, 그 안에 React 컴포넌트를 island로 붙여 CSR 또는 SSR로 렌더링하는 라이브러리.**

- 라우팅, 인증, 세션, CSRF 같은 서버 쪽 일은 Spring이 맡는다.
- 상호작용은 React가 맡고, npm 생태계를 그대로 사용한다.
- 별도 SPA 배포 없이 **실행 가능한 JAR 하나**로 배포한다.

## 2. 확정된 결정 사항

| # | 항목 | 결정 |
|---|---|---|
| 1 | Java 지원 범위 | **Java 8 이상** |
| 2 | 빌드 도구 | **Gradle, Maven 모두 지원** (사용자 프로젝트 기준) |
| 3 | 렌더링 | **CSR, SSR 모두 지원** (island 단위로 선택) |
| 4 | React 적용 범위 | **부분 island와 페이지 전체 React 모두 지원** |
| 5 | 언어 | **TypeScript 기본** |
| 6 | 배포 | **처음부터 공개 오픈소스** (Maven Central, npm) |
| 7 | 라이선스 | **Apache-2.0** |
| 8 | Maven groupId | **`run.iknow`** (artifact 예: `run.iknow:mustache-react-spring-boot-starter`) |
| 9 | React 최소 버전 | **React 18 이상** (19 포함) |
| 10 | 웹 스택 | **Spring MVC만 지원** (WebFlux는 후속 검토) |

## 3. 결정에 따른 기술적 제약과 대응

### 3-1. Java 8 지원 → Spring Boot 2.7과 3.x를 동시에 지원

- Spring Boot 3.x는 Java 17 이상이 필요하다. 따라서 Java 8 사용자는 **Spring Boot 2.7 (Spring 5.3)**을 쓴다.
- 지원 매트릭스:

  | Spring Boot | Java | Servlet API |
  |---|---|---|
  | 2.7.x | 8, 11, 17 | `javax.servlet` |
  | 3.x | 17, 21+ | `jakarta.servlet` |

- 대응 원칙:
  - 핵심 코드는 **Java 8 바이트코드**(`--release 8`)로 컴파일한다.
  - `javax`와 `jakarta` 차이를 피하기 위해 **Servlet API에 직접 의존하지 않는다.** 요청 정보는 `RequestContextHolder`와 `RequestAttributes`로 읽는다.
  - Auto-configuration은 `META-INF/spring.factories`(Boot 2)와 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`(Boot 3)를 **둘 다 포함한 단일 artifact**로 제공한다.
  - 단일 artifact가 불가능한 부분이 생기면 그 부분만 `-boot2`, `-boot3` 모듈로 분리한다.
- Spring Boot 2.7은 OSS 지원이 종료된 버전이라는 점을 문서에 명시한다. 그래도 레거시 Java 8 환경이 이 라이브러리의 핵심 타깃 중 하나다.

### 3-2. SSR과 Java 8 → SSR 엔진을 SPI로 추상화

- Java 8 내장 Nashorn은 ES5까지만 지원하고 성능이 낮아 React 18 이상의 SSR에 적합하지 않다.
- GraalJS(Polyglot)는 최신 버전 기준 Java 17 이상이 필요하다.
- 그래서 `SsrEngine` 인터페이스를 두고 구현체를 고를 수 있게 한다.

  | 엔진 | 모듈 | Java | 특징 |
  |---|---|---|---|
  | **Node 사이드카** | `mustache-react-ssr-node` | 8+ | HTTP로 Node 렌더 서버를 호출. 운영에서 가장 안정적이고 모든 React 기능 호환 |
  | **GraalJS** | `mustache-react-ssr-graaljs` | 17+ | JVM 안에서 렌더링하며 프로세스를 하나만 띄우면 됨 |
  | (없음) | — | — | SSR을 끄고 CSR만 사용 |

- SSR이 실패하거나 타임아웃이 나면 **CSR로 자동 fallback**한다. 페이지가 깨지지 않는 것을 원칙으로 한다.

### 3-3. Gradle과 Maven 모두 지원

- 라이브러리 저장소 자체는 Gradle(Kotlin DSL, toolchains)로 빌드한다.
- 사용자 프로젝트용 빌드 통합은 두 가지로 제공한다.
  - `mustache-react-gradle-plugin`: 프론트 빌드를 `processResources`에 연결하고, Node 설치는 node-gradle 방식으로 처리한다.
  - `mustache-react-maven-plugin`: `generate-resources` 단계에서 프론트를 빌드한다.
- 두 플러그인 모두 같은 일을 한다. Node와 pnpm을 설치하고, `vite build`(client와 ssr 번들)를 실행한 뒤, 결과물을 `static/`과 `META-INF/mustache-react/`에 배치한다.

## 4. 저장소 구조 (모노레포)

```
mustache-react/
├── java/
│   ├── mustache-react-core                    # Java 8, Spring 비의존: manifest, island 렌더링, JSON 이스케이프, SSR SPI
│   ├── mustache-react-spring                  # Spring MVC/Mustache 통합 (Spring 5.3, 6.x 공용)
│   ├── mustache-react-spring-boot-autoconfigure
│   ├── mustache-react-spring-boot-starter
│   ├── mustache-react-ssr-node                # Java 8+
│   └── mustache-react-ssr-graaljs             # Java 17+
├── build-plugins/
│   ├── mustache-react-gradle-plugin
│   └── mustache-react-maven-plugin
├── js/                                        # pnpm workspace, 전부 TypeScript
│   ├── client/        → @mustache-react/client        # 브라우저 런타임 (mount, hydrate)
│   ├── server/        → @mustache-react/server        # SSR 렌더 엔트리 (Node 사이드카와 GraalJS 번들 공용)
│   └── vite-plugin/   → @mustache-react/vite-plugin   # client와 ssr 번들, manifest, mustache watch
├── examples/
│   ├── boot2-java8-gradle
│   ├── boot2-java8-maven
│   └── boot3-java17-gradle
└── docs/
```

## 5. 핵심 동작

### 5-1. 렌더링 흐름

```
Controller ──Model──▶ Mustache 템플릿
                         │  {{#island}} 람다
                         ▼
                   IslandRenderer
                   ├─ props → JSON (안전 이스케이프)
                   ├─ ssr=true ? SsrEngine.render(name, props) : 빈 HTML
                   └─ <div data-island="Name" data-ssr="true|false">…</div>
                      <script type="application/json" data-island-props>…</script>
                         ▼
브라우저: @mustache-react/client
   ├─ data-ssr=true  → hydrateRoot()
   └─ data-ssr=false → createRoot().render()
```

### 5-2. 목표 API (잠정)

**컨트롤러.** 기존 Spring 방식을 그대로 쓴다.
```java
@GetMapping("/users/{id}")
public String detail(@PathVariable Long id, Model model) {
    model.addAttribute("user", userService.find(id));
    model.addAttribute("orders", orderService.recent(id));
    return "users/detail";
}
```

**템플릿.** 일부만 React로 만드는 경우.
```mustache
{{> layout/header}}
<h1>{{user.name}}</h1>

{{#island}}OrderTable props=orders{{/island}}
{{#island}}UserProfile props=user ssr{{/island}}

{{{vite.scripts}}}
{{> layout/footer}}
```

**페이지 전체를 React로 만드는 경우.**
```java
return ReactView.page("Dashboard", props).ssr(true);
```

**클라이언트 (TS).**
```ts
import { createIslands } from '@mustache-react/client';

createIslands({
  islands: {
    OrderTable: () => import('./islands/OrderTable'),
    UserProfile: () => import('./islands/UserProfile'),
    Dashboard: () => import('./pages/Dashboard'),
  },
  wrapper: ({ children }) => <AppProviders>{children}</AppProviders>,
});
```

**SSR 엔트리 (TS).** 같은 레지스트리를 공유한다.
```ts
import { createSsrRenderer } from '@mustache-react/server';
export default createSsrRenderer({ islands, wrapper });
```

### 5-3. 주요 기능

| 영역 | 기능 |
|---|---|
| 에셋 | Vite manifest 리졸버, 해시 경로, CSS와 modulepreload 태그 |
| 개발 경험 | dev 프로파일에서 Vite dev server 연동, HMR과 React Refresh preamble 자동 주입, `.mustache` 변경 시 reload |
| 보안 | `</script>`, `<!--`, U+2028/2029 이스케이프, CSP nonce 지원 |
| 컨텍스트 | CSRF 토큰, 로그인 사용자, locale, 메시지를 `useServerContext()`로 노출 |
| 마운트 전략 | `load`, `idle`, `visible` (CSR island) |
| 안정성 | SSR 타임아웃과 fallback, island 단위 에러 바운더리 |
| 타입 | (후속) Java DTO에서 TS 타입 생성 연동 |

## 6. 로드맵

| 단계 | 범위 | 완료 기준 |
|---|---|---|
| **M0. 뼈대** | Gradle 멀티모듈, pnpm workspace, CI 매트릭스(Java 8/11/17/21 × Boot 2.7/3.x), 라이선스, 기여 가이드 | CI 통과, 예제 앱 기동 |
| **M1. CSR MVP** | core(manifest, island, 이스케이프), spring, autoconfigure, `@mustache-react/client`, vite-plugin | Boot 2.7/Java 8과 Boot 3/Java 17 예제에서 island 동작 |
| **M2. 개발 경험** | Vite dev server와 HMR, mustache watch, `ReactView.page` | 코드 수정이 즉시 반영됨 |
| **M3. SSR** | SsrEngine SPI, `@mustache-react/server`, ssr-node(사이드카 관리 포함), fallback | SSR island가 hydrate되고 장애 시 CSR로 전환 |
| **M4. 빌드 플러그인** | Gradle 플러그인, Maven 플러그인 | 두 빌드 도구 모두 `jar` 하나로 배포 가능 |
| **M5. 실무 기능** | 서버 컨텍스트, CSP nonce, 마운트 전략, 에러 바운더리 | 로그인과 폼 POST 예제 |
| **M6. GraalJS SSR** | ssr-graaljs (Java 17+) | Node 없이 SSR 가능 |
| **M7. 첫 릴리스** | Maven Central, npm 배포 자동화, 문서 사이트 | `0.1.0` 릴리스 |

## 7. 오픈소스 운영

- **라이선스:** Apache-2.0
- **Maven 좌표:** `run.iknow:mustache-react-*`. Central namespace 등록 시 `iknow.run` 도메인 DNS TXT 인증이 필요하다.
- **Java 패키지:** `run.iknow.mustachereact.*`
- **npm 스코프:** `@mustache-react/*`. npm org 이름이 사용 가능한지 확인해야 한다.
- **버전 정책:** Java artifact와 npm 패키지를 **같은 버전으로 함께 릴리스**한다. 런타임에 manifest 형식 버전이 서로 맞는지 검증한다.
- **CI/CD:** GitHub Actions에서 빌드 매트릭스를 돌리고, 태그를 push하면 Central과 npm에 배포한다(서명 키와 토큰은 Secrets로 관리).
- **문서:** README 빠른 시작, `docs/` 사이트(VitePress), 예제 3종.

## 8. 남은 결정 사항

1. Mustache 템플릿 문법: 후보 비교 중 (`{{#island}}Name prop=path @ssr{{/island}}` 형태를 추천)
