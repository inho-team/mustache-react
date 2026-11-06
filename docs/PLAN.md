# mustache-react 설계 계획

> 상태: v0.6 · M4 완료 · 공개 오픈소스 (Apache-2.0)

## 1. 한 줄 정의

**Spring 개발자가 지금 쓰는 방식(Controller, DTO, `@Valid`, Security) 그대로 React 생태계의 UI를 쓸 수 있게 이어 주는 라이브러리.**

- 시작점은 Java다. 컨트롤러가 화면을 정하고, DTO가 곧 React props가 된다.
- 프론트 빌드(Node, Vite, 번들링)는 Gradle/Maven 플러그인 뒤에 숨긴다. 사용자는 `.tsx` 파일만 작성한다.
- 기존 Mustache 화면에 일부만 React를 넣는 것도 가능하다.
- 별도 SPA 배포 없이 **실행 가능한 JAR 하나**로 배포한다.

## 2. 타깃 사용자

| 항목 | 내용 |
|---|---|
| 익숙한 것 | Spring MVC Controller, DTO, `@Valid`, Spring Security, JSP/Thymeleaf/Mustache |
| 낯설거나 피하고 싶은 것 | npm, Vite, 번들러 설정, 프론트 라우팅과 상태 관리, CORS, 토큰 인증 연동 |
| 원하는 것 | 데이터 그리드, 차트, 날짜 선택기, 모달 같은 React 생태계 UI |
| 대표 상황 | 기존 API 서버에 관리자 화면 붙이기, 레거시 JSP/Mustache 화면을 점진적으로 개선 |

React 개발자는 1차 타깃이 아니다. 문법과 API를 고를 때는 **"Spring 개발자가 처음 봤을 때 바로 이해되는가"**를 기준으로 삼는다.

## 3. 설계 원칙

1. **시작점은 Java다.** 화면은 컨트롤러가 정하고, 템플릿 문법은 최소한으로 줄인다.
2. **설정 없이 동작한다 (Spring Boot 방식).** `src/main/frontend`에 `.tsx`만 두면 된다. Node 설치, Vite 설정, 빌드는 플러그인이 처리한다.
3. **Java DTO가 곧 props다.** TS 타입은 Java 클래스에서 자동 생성한다. 필드 이름이 바뀌면 TS 컴파일 에러로 드러난다.
4. **Spring 기능을 그대로 연결한다.** CSRF, 로그인 사용자, `@Valid` 검증 에러, MessageSource(i18n), 에러 응답을 별도 작업 없이 React에 전달한다.
5. **React는 컴포넌트 작성 수준만 알면 된다.** 라우팅, 전역 상태, API 클라이언트 설정을 배울 필요가 없다.
6. **실패해도 페이지는 뜬다.** SSR이 실패하면 CSR로 전환하고, island 하나가 에러를 내도 나머지 화면은 동작한다.

## 4. 확정된 결정 사항

| # | 항목 | 결정 |
|---|---|---|
| 1 | Java 지원 범위 | **Java 8 이상** |
| 2 | Spring Boot 지원 범위 | **2.7 / 3.5 / 4.x** (최소 2.7) |
| 3 | 빌드 도구 | **Gradle, Maven 모두 지원** |
| 4 | 렌더링 | **CSR, SSR 모두 지원** |
| 5 | React 적용 범위 | **페이지 전체(`@ReactPage`)가 중심이고, Mustache 안에 부분 island를 넣는 것은 보조** |
| 6 | 언어 | **TypeScript 기본** |
| 7 | 라이선스 | **Apache-2.0** |
| 8 | Maven groupId | **`run.iknow`** (Java 패키지는 `run.iknow.mustachereact`) |
| 9 | React 최소 버전 | **React 18 이상** (19 포함) |
| 10 | 웹 스택 | **Spring MVC만 지원** (WebFlux는 후속 검토) |
| 11 | URL 전략 | **같은 URL에서 요청 종류에 따라 HTML 또는 JSON으로 응답** (content negotiation). 끌 수 있음 |
| 12 | 페이지 이동 | **처음엔 일반 페이지 이동.** SPA 방식 이동은 후속 과제 |

## 5. 사용 모습 (목표 API)

### 5-1. `@ReactPage`: API 응답 DTO를 그대로 화면으로 사용 (핵심 기능)

```java
@RestController
public class OrderController {

    @ReactPage("orders/OrderList")
    @GetMapping("/orders")
    public OrderListResponse list(Pageable pageable) {
        return orderService.list(pageable);   // 기존 API 코드를 그대로 사용
    }
}
```

| 요청 | 응답 |
|---|---|
| 브라우저 접속 (`Accept: text/html`) | 기본 레이아웃 HTML. 반환된 DTO가 `OrderList` 컴포넌트의 props가 된다 |
| `fetch`나 앱의 API 호출 (`Accept: application/json`) | 기존처럼 JSON |

```tsx
// src/main/frontend/pages/orders/OrderList.tsx
import type { OrderListResponse } from '@generated/types';   // Java DTO에서 자동 생성

export default function OrderList({ content, totalPages }: OrderListResponse) {
  return <DataGrid rows={content} />;   // 원하는 React 라이브러리 사용
}
```

- 컴포넌트 이름 `"orders/OrderList"`는 `src/main/frontend/pages/orders/OrderList.tsx` 파일에 대응한다(관례 기반).
- 레이아웃은 기본값을 제공하고, `templates/mustache-react/layout.mustache`를 두면 그 파일로 교체된다.
- `@ReactPage`는 `@Controller` 메서드에서도 쓸 수 있다. 반환값이 props가 된다.
- 반환값은 DTO나 `Map`처럼 **JSON 객체가 되는 값**이어야 한다. `null`은 빈 props로 처리한다. `ResponseEntity` 반환은 아직 지원하지 않는다.

**HTML과 JSON 중 무엇으로 응답할지 정하는 규칙**

| `Accept` 헤더 | 응답 |
|---|---|
| `text/html` 또는 `application/xhtml+xml`이 있고, JSON보다 우선순위(q)가 낮지 않음 (브라우저 주소창 이동) | HTML 페이지 |
| 그 외 (`*/*`, `application/json`, 헤더 없음: curl, `fetch`, API 클라이언트) | JSON |

- 두 응답 모두 `Vary: Accept` 헤더를 붙여서 캐시가 둘을 섞지 않게 한다.
- `mustache-react.content-negotiation=false`로 설정하면 항상 HTML로 응답한다.
- props JSON은 Spring MVC에 등록된 **HttpMessageConverter**로 만든다. 그래서 API 응답 JSON과 정확히 같다. Jackson 2(Boot 2.7/3.x), Jackson 3(Boot 4), Gson과 애플리케이션의 Jackson 설정이 모두 그대로 적용된다.

**레이아웃**

`templates/mustache-react/layout.mustache`를 두면 기본 레이아웃 대신 그 파일을 쓴다. 애플리케이션의 Mustache 컴파일러로 컴파일하므로 partial도 쓸 수 있다.

| 변수 | 내용 |
|---|---|
| `{{title}}` | `@ReactPage(title=...)`. 비어 있으면 페이지 이름 |
| `{{lang}}` | 요청 locale의 언어 태그 |
| `{{page}}` | 페이지 이름 (`orders/OrderList`) |
| `{{{mr.head}}}` | CSS와 modulepreload 태그 |
| `{{{mr.page}}}` | React가 렌더링할 요소와 props script |
| `{{{mr.scripts}}}` | 엔트리 script 태그 |

### 5-2. 기존 Mustache 화면에 일부만 React 삽입 (보조 기능)

컨트롤러는 평소처럼 Model만 채운다.

```java
@GetMapping("/dashboard")
public String dashboard(Model model) {
    model.addAttribute("orders", orderService.findAll());
    model.addAttribute("currency", "원");
    return "dashboard";
}
```
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

**문법**

| 문법 | 의미 |
|---|---|
| `{{#react}}이름 a b{{/react}}` | `src/main/frontend/islands/이름.tsx`에 Model의 `a`, `b`를 같은 이름의 props로 전달 |
| `{{#react}}이름 x=a.b{{/react}}` | 경로 `a.b`의 값을 `x`라는 이름의 prop으로 전달 |
| `{{#react}}이름 item=this{{/react}}` | 현재 루프 항목 전체를 `item`으로 전달 |
| `{{{mr.head}}}` | `<head>` 안. 엔트리의 CSS와 preload |
| `{{{mr.scripts}}}` | **마지막 island 뒤** (`</body>` 직전). 페이지에서 쓴 island의 CSS와 preload, 엔트리 script |

- 이름은 `islands/` 아래 경로다. `charts/OrderChart`는 `islands/charts/OrderChart.tsx`를 가리킨다.
- 토큰은 공백이나 줄바꿈으로 구분하므로 여러 줄로 써도 된다.
- 값은 Mustache와 같은 규칙으로 찾는다. 첫 단어는 현재 루프 항목부터 바깥 섹션, Model 순서로 찾고, 나머지 단어는 찾은 값에서 읽는다. 읽는 순서는 `Map` 키, record 방식 접근자 `name()`, getter `getName()`/`isName()`, public 필드다.
- 값이 `null`이면 `null` prop으로 전달한다. 이름 자체가 없으면 템플릿 오류로 렌더링을 중단한다. 오류 메시지에는 어떤 섹션의 어떤 이름인지와 `model.addAttribute(...)` 안내가 들어간다.
- props JSON은 `@ReactPage`와 같은 HttpMessageConverter로 만든다. 따라서 Jackson 설정이 똑같이 적용된다.
- island id(`mr-0`, `mr-1`, ...)는 요청마다 0부터 다시 매긴다. `mr.scripts`가 페이지에서 쓴 island를 알 수 있는 것은 요청 단위로 기록하기 때문이다.
- `react`와 `mr`는 `@ControllerAdvice`가 **모든 Spring MVC Model**에 넣는다. 컨트롤러 코드는 필요 없다.
  - 같은 이름의 Model 값이 이미 있으면 덮어쓰지 않는다.
  - 이름은 `mustache-react.template.react-name`, `mustache-react.template.helpers-name`으로 바꿀 수 있다.
  - `mustache-react.template.model-attributes=false`로 끌 수 있다.
- island를 감싸는 요소는 `<div>`다. `<p>` 같은 인라인 문맥 안에는 넣지 않는다.
- SSR 여부, 마운트 시점 같은 옵션은 템플릿에 쓰지 않고 `application.yml`이나 Java 쪽에서 설정한다(M5).

### 5-3. 폼과 검증

```java
@ReactPage("orders/OrderForm")
@PostMapping("/orders")
public Object create(@Valid @RequestBody OrderForm form, BindingResult result) { ... }
```
```tsx
const { register, errors, submit } = useSpringForm<OrderForm>();   // CSRF, 검증 에러 자동 연동
```

- `BindingResult`의 필드 에러는 그 필드에 맞춰 폼에 표시된다.
- 처리 후 `redirect:`를 반환하면 Spring 방식 그대로 페이지가 이동한다.

### 5-4. 서버 컨텍스트

```tsx
const { user, csrf, locale, messages } = useServerContext();
```

로그인 사용자 정보 노출 범위와 메시지 키 노출 범위는 Java 설정으로 정한다. 기본값은 최소한만 노출한다.

### 5-5. 설정

```yaml
mustache-react:
  ssr: off                     # off | auto | always (M5)
  frontend-dir: src/main/frontend
  content-negotiation: true
  manifest-location: classpath:/static/mustache-react/.vite/manifest.json
  public-path: /mustache-react/
  layout: classpath:/templates/mustache-react/layout.mustache
  dev-server:
    enabled: false             # true: Vite dev server에서 모듈을 받음 (개발 프로파일용)
    url: http://localhost:5173
    auto-start: true           # 앱과 함께 dev server를 실행/종료
    node: ""                   # 기본: .mustache-react/node의 최신 버전, 없으면 PATH의 node
    node-install-dir: .mustache-react/node
    startup-timeout: 30s
  template:
    react-name: react
    helpers-name: mr
    model-attributes: true
```

## 6. 렌더링 흐름

```
Controller ─ @ReactPage 반환값 / Model ─▶ MustacheReact 렌더러
                                           ├─ props → JSON (Jackson + 안전 이스케이프)
                                           ├─ SSR 사용 ? SsrEngine.render(name, props) : 빈 HTML
                                           └─ 레이아웃 Mustache에 삽입
                                               <div data-mr-island="orders/OrderList" data-ssr="true">…</div>
                                               <script type="application/json" data-mr-props>…</script>
브라우저: @mustache-react/client
   ├─ data-ssr=true  → hydrateRoot()
   └─ data-ssr=false → createRoot().render()
```

- JSON 안의 `</script>`, `<!--`, U+2028/2029는 이스케이프해서 XSS를 막는다.
- 서버 쪽 오류(manifest 없음, 잘못된 island 문법, 없는 Model 이름)는 Mustache 템플릿 오류처럼 렌더링을 중단하고 원인을 메시지로 알린다.
- 브라우저 쪽에서는 island 하나가 실패해도 나머지 island는 마운트된다. 개발 모드에서는 실패한 island 자리에 오류 박스를 보여 준다.

### 6-1. 프론트엔드 디렉터리 관례

```
src/main/frontend/
├── pages/            # @ReactPage("orders/OrderList") → pages/orders/OrderList.tsx (default export)
├── islands/          # {{#react}}OrderChart orders{{/react}} → islands/OrderChart.tsx (default export)
└── app.tsx           # (선택) 모든 페이지와 island를 감싸는 컴포넌트: 테마, QueryClient 등
```

- `@mustache-react/vite-plugin`이 클라이언트 엔트리를 자동으로 생성한다. 사용자가 `main.tsx`를 작성할 필요가 없다.
- 페이지 파일은 lazy chunk로 분리한다. 서버는 manifest를 보고 현재 페이지의 chunk를 `modulepreload`로 미리 받게 한다.
- 빌드 결과는 Gradle은 `build/mustache-react/resources/`, Maven은 `target/mustache-react/`에 생성된다. 이 디렉터리가 리소스에 추가되므로 jar의 `static/mustache-react/`에 들어가고, `/mustache-react/**` 경로로 서빙된다.
- 사용자가 두는 파일은 `package.json`(의존성 목록)과 컴포넌트뿐이다. `vite.config.ts`는 필요 없다. 없으면 `mustache-react-vite` CLI가 `@vitejs/plugin-react`와 `mustacheReact()`로 실행한다. 직접 둔 설정 파일이 있으면 그 파일을 쓴다.

## 7. 기술적 제약과 대응

### 7-1. Java 8과 Spring Boot 2.7/3.5/4.x 동시 지원

| Spring Boot | Java | Servlet API | 비고 |
|---|---|---|---|
| 2.7.x | 8+ | `javax.servlet` | OSS 지원 종료. 레거시가 핵심 타깃 |
| 3.5.x | 17+ | `jakarta.servlet` | |
| 4.x | 17+ | `jakarta.servlet` | auto-config 모듈 분리, 패키지 일부 변경 |

- 라이브러리는 **Java 8 바이트코드**(`--release 8`)로 컴파일하고, **Boot 2.7 API를 기준으로** 작성한다.
- **Servlet API에 직접 의존하지 않는다.** 요청 정보는 `RequestContextHolder`와 `RequestAttributes`로 읽는다.
  - 예를 들어 `View` 인터페이스는 메서드 시그니처에 servlet 타입이 들어 있어 구현하지 않는다. 대신 응답을 `ResponseEntity`로 만들어 Spring MVC의 기존 핸들러에 맡긴다.
  - 그 기존 핸들러도 Spring 버전마다 다르다(5.3은 `HttpEntityMethodProcessor`, 6 이상은 이를 감싼 `ResponseEntityReturnValueHandler`). 그래서 클래스 이름으로 찾지 않고 "`ResponseEntity`를 지원하는가"로 찾는다.
- Auto-configuration은 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 하나로 등록한다. 2.7부터 4.x까지 모두 읽는 형식이다.
- 다른 auto-configuration과의 순서는 클래스 대신 이름 문자열(`afterName`)로 지정한다. Boot 4에서 패키지가 바뀌었기 때문이다.
- CI에서 (Boot 2.7, Java 8), (Boot 3.5, Java 17), (Boot 4.x, Java 21) 조합으로 같은 테스트를 실행한다.
- 단일 artifact로 해결이 안 되는 부분이 생기면 그 부분만 `-boot2`, `-boot3` 모듈로 분리한다.

### 7-2. SSR 엔진을 SPI로 분리

Java 8의 Nashorn은 최신 React SSR에 쓸 수 없고, GraalJS는 Java 17 이상이 필요하다. 그래서 `SsrEngine` 인터페이스를 두고 구현체를 고른다.

| 엔진 | 모듈 | Java | 특징 |
|---|---|---|---|
| Node 사이드카 | `mustache-react-ssr-node` | 8+ | HTTP로 Node 렌더 서버 호출. 개발 모드에서는 Spring이 프로세스를 관리 |
| GraalJS | `mustache-react-ssr-graaljs` | 17+ | JVM 안에서 렌더링 |
| (없음) | — | — | CSR만 사용 (기본값) |

### 7-3. Java DTO에서 TS 타입 생성

**annotation processor** `mustache-react-processor`를 쓴다. Java 8에서 동작하고, Gradle/Maven 어느 쪽이든 javac 단계에서 실행된다.

- **Gradle:** 플러그인이 processor 의존성과 `typesFile` 옵션을 자동으로 추가한다(`mustacheReact { types = false }`로 끈다).
- **Maven:** `maven-compiler-plugin`에 다음을 추가한다.

```xml
<annotationProcessorPaths>
  <path><groupId>run.iknow</groupId><artifactId>mustache-react-processor</artifactId><version>…</version></path>
</annotationProcessorPaths>
<compilerArgs><arg>-Amustachereact.typesFile=${project.basedir}/src/main/frontend/generated/types.ts</arg></compilerArgs>
```

**생성 대상과 결과물**

- 대상은 `@ReactPage` 메서드의 반환 타입과 `@ReactType`을 붙인 클래스, 그리고 이 클래스들이 참조하는 모든 클래스다. island props용 DTO에는 `@ReactType`을 붙인다.
- 결과물은 TS 파일 하나다. 클래스마다 `interface`가, enum마다 문자열 union이 만들어진다.
- 페이지 이름과 props 타입을 연결하는 `ReactPages` 맵과 `PageProps<'orders/OrderList'>` 헬퍼도 함께 만든다.

**타입 매핑**

Spring Boot 기본 Jackson 설정이 실제로 내보내는 JSON을 기준으로 맞춘다.

| Java | TypeScript |
|---|---|
| 숫자 타입, `BigDecimal` | `number` |
| `String`, `UUID`, `java.time.*`, `Date` | `string` (날짜는 ISO 문자열) |
| 컬렉션, 배열 | `T[]` |
| `byte[]` | `string` (Base64) |
| `Map` | `Record<string, V>` |
| `Optional<T>`, `@Nullable` | `T \| null` |
| enum | 문자열 union (`@JsonProperty` 반영) |
| 제네릭 클래스 | 제네릭 interface |
| 상속 | 상위 클래스 속성을 펼쳐서 포함 |

- 속성 이름은 Jackson 규칙을 따른다. 예를 들어 `getURL`은 `url`이 된다.
- `@JsonIgnore`, `@JsonProperty`, `@JsonIgnoreProperties`를 반영한다.
- record 컴포넌트와 Lombok `@Data`/`@Getter`/`@Value` 필드도 읽는다.
- 그 밖의 null 가능 여부는 타입에 표시하지 않는다. null이 될 수 있는 값은 `@Nullable`로 표시한다.

**동작 방식**

- 파일 위치는 `-Amustachereact.typesFile` 옵션으로 정한다. 내용이 바뀔 때만 다시 쓰므로 Vite 감시가 불필요하게 반응하지 않는다.
- 생성된 파일은 커밋한다. CI는 Java 빌드 후 이 파일이 커밋본과 다르면 실패한다. 덕분에 DTO를 바꾸면 TS 쪽 타입 에러로 바로 드러난다.

### 7-4. Gradle과 Maven 지원

**사용자 프로젝트 설정**

```kotlin
// build.gradle.kts
plugins { id("run.iknow.mustache-react") version "…" }
```
```xml
<!-- pom.xml -->
<plugin>
  <groupId>run.iknow</groupId><artifactId>mustache-react-maven-plugin</artifactId><version>…</version>
  <executions><execution><goals><goal>build</goal></goals></execution></executions>
</plugin>
```

**두 플러그인이 하는 일** (공통 라이브러리 `mustache-react-build-tools` 사용)

1. Node.js를 프로젝트의 `.mustache-react/node`에 내려받는다.
   - 기본 버전은 24 LTS이고, `nodeVersion`으로 바꿀 수 있다. `nodeDownload=false`면 `PATH`의 Node를 쓴다.
   - 내려받은 파일은 릴리스의 `SHASUMS256.txt`로 검증한다.
   - 압축 해제는 Java로 직접 한다. 경로가 압축 대상 디렉터리 밖으로 나가는 항목은 거부한다.
2. 의존성을 설치한다.
   - 패키지 매니저는 `package.json`의 `packageManager` 필드나 lock 파일로 정한다. 기본값은 npm이다.
   - lock 파일이 있으면 `npm ci`나 `--frozen-lockfile`로 설치한다.
   - `packageManager`에 pnpm이나 yarn 버전이 적혀 있으면 `npm exec`로 정확히 그 버전을 쓴다.
   - `package.json`과 lock 파일이 바뀌지 않았으면 다시 설치하지 않는다. Gradle은 자체 up-to-date 검사로, Maven은 `node_modules`에 남긴 해시로 판단한다.
3. `mustache-react-vite build`로 빌드하고, 결과 디렉터리를 리소스에 추가한다.
4. 타입 생성: Gradle은 processor와 옵션을 자동으로 연결한다. Maven은 위 7-3의 설정을 직접 추가한다.
5. Gradle에서는 `bootRun`/`run`에 프론트 디렉터리와 Node 위치를 시스템 속성으로 전달한다(개발 서버 자동 실행용).

**공통 동작**

- Java만 확인할 때는 Gradle은 `-PmustacheReact.skipFrontend`, Maven은 `-DmustacheReact.skipFrontend`로 프론트 빌드를 건너뛴다.
- 두 플러그인 모두 Java 8 바이트코드로 빌드한다.

**Gradle 플러그인 호환성**

- Gradle 7.6과 8.x에서 동작한다. Gradle 7에 있는 API만 쓴다.
- 설정 캐시와 빌드 캐시를 지원한다. TestKit 테스트로 확인한다.
- `mustacheReactBuild` 태스크는 `@CacheableTask`이다.

**라이브러리 저장소의 구성**

- 라이브러리 저장소는 Gradle(Kotlin DSL)로 빌드한다.
- 빌드 플러그인은 별도 Gradle 빌드(`build-plugins/`)로 둔다.
- 메인 빌드는 `pluginManagement.includeBuild`로 이 빌드를 포함한다. 그래서 예제가 소스 상태의 플러그인을 그대로 적용할 수 있다.
- Maven 플러그인의 `plugin.xml`은 `de.benediktritter.maven-plugin-development`로 생성한다.

### 7-5. 개발 모드 (Vite dev server)

```properties
# application-dev.properties
mustache-react.dev-server.enabled=true
spring.mustache.prefix=file:src/main/resources/templates/   # 템플릿 수정을 빌드 없이 반영
spring.mustache.servlet.cache=false
```

- 페이지는 Spring이 렌더링하고, JS/CSS 모듈은 Vite dev server(`http://localhost:5173`)에서 받는다. 프론트를 빌드하거나 manifest를 만들 필요가 없다.
- 서버는 Vite가 원래 `index.html`에 넣는 태그를 직접 출력한다. React Refresh preamble, `@vite/client`, 생성된 엔트리가 그것이다. preamble은 `@vitejs/plugin-react` 4부터 6까지 동작하는 형식으로 쓴다.
- vite-plugin의 개발 모드 동작은 다음과 같다.
  - 포트를 고정한다(`strictPort`).
  - 에셋 URL이 Spring이 아니라 dev server를 가리키도록 `server.origin`을 설정한다.
  - `pages/`, `islands/`, `app.tsx`를 의존성 스캔 대상으로 지정한다. HTML이 없어 Vite가 스캔할 곳이 없기 때문이며, 덕분에 첫 방문 때 의존성 재최적화로 페이지가 새로고침되지 않는다.
  - `src/main/resources/templates/**/*.mustache`가 바뀌면 브라우저를 새로고침한다.
- 컴포넌트 수정은 HMR로 그 자리에서 반영되고, React 상태도 유지된다.
- 개발 모드에서는 island 마운트가 실패하면 그 자리에 오류 박스를 보여 준다.
- **자동 실행:** 개발 모드에서는 Spring이 앱과 함께 Vite dev server를 띄우고, 앱이 종료될 때 함께 종료한다(`ViteDevServerProcess`).
  - Gradle `bootRun`, Maven `spring-boot:run`, IDE 실행 모두 같은 방식으로 동작한다.
  - Node는 빌드 플러그인이 받아 둔 `.mustache-react/node`의 최신 버전을 쓰고, 없으면 `PATH`의 `node`를 쓴다.
  - 포트에 이미 서버가 떠 있으면 새로 띄우지 않고 그 서버를 쓴다.
  - `mustache-react.dev-server.auto-start=false`로 끌 수 있다.

## 8. 저장소 구조

```
mustache-react/
├── build-logic/                               # Gradle convention plugin
├── java/
│   ├── mustache-react-core                    # Spring 비의존: manifest, 렌더링, JSON 이스케이프, SSR SPI
│   ├── mustache-react-spring                  # Spring MVC 통합: @ReactPage, Mustache 람다
│   ├── mustache-react-spring-boot-autoconfigure
│   ├── mustache-react-spring-boot-starter
│   ├── mustache-react-processor               # DTO → TS 타입 (annotation processor)
│   ├── mustache-react-ssr-node                # (M5)
│   └── mustache-react-ssr-graaljs             # (M7)
├── build-plugins/                             # 별도 Gradle 빌드 (메인 빌드가 includeBuild)
│   ├── mustache-react-build-tools             # Node 설치, 패키지 매니저, 프론트 빌드 실행 (공통)
│   ├── mustache-react-gradle-plugin           # id("run.iknow.mustache-react")
│   └── mustache-react-maven-plugin            # mustache-react:build
├── js/                                        # pnpm workspace, TypeScript
│   ├── client      → @mustache-react/client       # 브라우저 런타임, useServerContext, useSpringForm
│   ├── server      → @mustache-react/server       # SSR 렌더 엔트리
│   └── vite-plugin → @mustache-react/vite-plugin  # Vite 플러그인 + mustache-react-vite CLI (build|dev)
├── examples/
│   └── basic                                  # 같은 소스를 Gradle(build.gradle.kts)과 Maven(pom.xml)으로 빌드
└── docs/
```

## 9. 로드맵

| 단계 | 범위 | 완료 기준 |
|---|---|---|
| ✅ **M0. 뼈대** | Gradle 멀티모듈, pnpm workspace, Boot 버전 매트릭스 CI, 라이선스, 기여 가이드, 예제 앱 | 세 가지 Boot 조합 모두에서 CI 통과, 예제 기동 |
| ✅ **M1. `@ReactPage` CSR MVP** | manifest 리졸버, 레이아웃, `@ReactPage` 반환값 처리, content negotiation, client mount | 예제의 `/orders`가 브라우저에서는 React, curl에서는 JSON으로 응답 |
| ✅ **M2. Mustache island** | `{{#react}}` 람다, `mr.head`/`mr.scripts`, 경로 해석 | 기존 Mustache 화면에 island 2개 삽입 |
| ✅ **M3. 타입 생성과 개발 경험** | annotation processor, Vite dev server와 HMR | DTO를 수정하면 TS 에러로 드러나고, 코드 수정이 즉시 반영 |
| ✅ **M4. 빌드 플러그인** | Gradle 플러그인, Maven 플러그인, Node 자동 설치 | npm을 직접 실행하지 않고 `bootJar`/`package`만으로 배포 가능 |
| **M5. Spring 연동과 SSR** | 서버 컨텍스트, `useSpringForm` + `@Valid`, SsrEngine SPI, ssr-node, fallback | 로그인과 폼 예제, SSR hydrate |
| **M6. 첫 릴리스** | Maven Central, npm 배포 자동화, 문서 사이트 | `0.1.0` 릴리스 |
| **M7. 확장** | GraalJS SSR, SPA 방식 페이지 이동, WebFlux 검토 | — |

## 10. 오픈소스 운영

- **라이선스:** Apache-2.0
- **Maven 좌표:** `run.iknow:mustache-react-*`. Central namespace 등록 시 `iknow.run` 도메인 DNS TXT 인증이 필요하다.
- **npm 스코프:** `@mustache-react/*`. npm org 이름이 사용 가능한지 확인하고 생성해야 한다.
- **버전 정책:** Java artifact와 npm 패키지를 같은 버전으로 함께 릴리스하고, 런타임에 manifest 형식 버전이 서로 맞는지 검증한다.
- **CI/CD:** GitHub Actions. 태그를 push하면 Central과 npm에 배포한다(서명 키와 토큰은 Secrets로 관리).

## 11. 남은 결정 사항

1. npm 스코프 이름 (`@mustache-react` 그대로 갈지, `@iknow` 같은 다른 이름을 쓸지)
2. 기본 레이아웃에 CSS 리셋이나 기본 스타일을 넣을지
