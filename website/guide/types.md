# TypeScript 타입 생성

Java DTO에서 TypeScript 타입을 자동으로 만듭니다. Java 필드 이름이나 타입을 바꾸면 React 코드가 **컴파일 에러**로 알려 주므로, 백엔드와 화면이 어긋난 채로 배포되는 일을 막습니다.

`mustache-react-processor`(Java annotation processor)가 **Java를 컴파일할 때** `src/main/frontend/generated/types.ts`를 씁니다. 별도 명령은 없습니다.

```tsx
import type { PageProps, IslandProps, Order } from '../../generated/types';

export default function OrderList({ orders, total }: PageProps<'orders/OrderList'>) { ... }
export default function StatusBadge({ status, orderId }: IslandProps<'StatusBadge'>) { ... }
```

## 무엇이 생성되나

| Java | 생성되는 것 |
|---|---|
| `@ReactPage` 메서드의 반환 타입 (`ResponseEntity<T>`면 `T`) | 그 타입의 interface + `ReactPages` 맵의 한 줄 → `PageProps<'페이지 이름'>` |
| `@ReactIsland("이름")` 클래스 | interface + `ReactIslands` 맵의 한 줄 → `IslandProps<'이름'>` |
| `@ReactType` 클래스 | interface (`fetch` 응답 타입 등 다른 곳에서 쓸 때) |
| 위 타입이 참조하는 클래스 | 필요한 만큼 따라가며 모두 생성 |

생성 예 (예제 앱):

```ts
export interface Order {
  id: number;
  customer: string;
  amount: number;
  status: OrderStatus;
  orderedAt: string;
}

export type OrderStatus = 'PAID' | 'SHIPPED' | 'CANCELLED';

export interface OrderDetailPage {
  id: number;
  order: Order | null;
}

export interface ReactPages {
  'orders/OrderDetail': OrderDetailPage;
  'orders/OrderList': OrderListResponse;
}
export type PageProps<P extends keyof ReactPages> = ReactPages[P];
```

반환 타입이 `void`인 페이지는 `Record<string, never>`(props 없음)입니다. 같은 페이지 이름에 GET은 DTO를, POST는 `Object`(`"redirect:..."` 또는 DTO)를 반환하는 흔한 폼 패턴에서는 구체적인 DTO 쪽이 쓰입니다.

## Java → TypeScript 매핑

Spring Boot 기본 Jackson이 실제로 쓰는 JSON 모양을 따릅니다.

| Java | TypeScript |
|---|---|
| `int`, `long`, `double`, `Integer`, `Long`, `BigDecimal`, `BigInteger`, `AtomicLong`, `OptionalInt` 등 | `number` |
| `boolean`, `Boolean` | `boolean` |
| `String`, `char`, `UUID`, `URI`, `Locale`, `Currency` | `string` |
| `LocalDate`, `LocalDateTime`, `Instant`, `ZonedDateTime`, `Duration`, `Date`, `Calendar` 등 날짜·시간 | `string` (ISO-8601) |
| enum | 이름의 union: `'PAID' \| 'SHIPPED'`. `@JsonProperty("shipped")`가 붙은 상수는 그 값 |
| `List<T>`, `Set<T>`, `Collection<T>`, `Iterable<T>`, `Stream<T>`, `T[]` | `T[]` |
| `byte[]` | `string` (Base64) |
| `Map<K, V>` | `Record<string, V>` |
| `Optional<T>` | `T \| null` |
| `@Nullable`이 붙은 필드나 getter | `T \| null` |
| `Object`, `JsonNode`, 그 밖의 `java.*` 타입 | `unknown` |
| 직접 만든 클래스, record, interface | 같은 이름의 interface |
| 제네릭 클래스 `Page<T>` | 제네릭 interface `Page<T>` |
| 중첩 클래스, 이름이 겹치는 클래스 | 바깥 클래스 이름을 붙여 구분 (`OrderDetail`, `InvoiceDetail`) |

### 어떤 속성이 들어가나

Jackson과 같은 규칙입니다.

- public getter(`getName()` → `name`, `isPaid()` → `paid`, `getURL()` → `url`)
- record 컴포넌트
- public 필드
- Lombok: 클래스에 `@Data`, `@Getter`, `@Value`가 있으면 모든 필드, 필드에 `@Getter`가 있으면 그 필드
- 상속받은 속성도 포함

| 어노테이션 | 효과 |
|---|---|
| `@JsonProperty("generated_at")` | 그 이름으로 |
| `@JsonIgnore` | 제외 |
| `@JsonIgnoreProperties({"internal"})` (클래스) | 나열한 속성 제외 |
| `@Nullable` | `\| null` 추가. 패키지와 상관없이 이름이 `Nullable`이면 됩니다(`jakarta.annotation`, `org.springframework.lang`, `org.jspecify` 등) |

`private` 필드에 getter가 없으면 JSON에 없으므로 타입에도 없습니다.

## 설정

### Gradle

`run.iknow.mustache-react` 플러그인이 processor 의존성과 출력 경로를 알아서 설정합니다.

```kotlin
mustacheReact {
    types = false                                        // 타입 생성 끄기
    typesFile = file("src/main/frontend/types/api.ts")   // 위치 바꾸기
}
```

### Maven {#maven}

`maven-compiler-plugin`에 processor와 출력 경로를 추가합니다.

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <parameters>true</parameters>
    <annotationProcessorPaths>
      <path>
        <groupId>run.iknow</groupId>
        <artifactId>mustache-react-processor</artifactId>
        <version>0.1.0</version>
      </path>
      <!-- Lombok을 쓰면 여기에도 lombok을 함께 둡니다 -->
    </annotationProcessorPaths>
    <compilerArgs>
      <arg>-Amustachereact.typesFile=${project.basedir}/src/main/frontend/generated/types.ts</arg>
    </compilerArgs>
  </configuration>
</plugin>
```

`-Amustachereact.typesFile`가 없으면 파일은 `target/classes/META-INF/mustache-react/types.ts`에 쓰이고 프론트에서 import할 수 없습니다.

## 생성된 파일 관리

- 내용이 바뀔 때만 파일을 다시 씁니다(개발 서버가 불필요하게 새로고침하지 않음).
- **커밋하는 것을 권장합니다.** 코드 리뷰에서 API 변화가 보이고, IDE가 빌드 전에도 타입을 압니다.
- CI에서 "Java를 바꾸고 types.ts를 커밋하지 않은" 경우를 잡으려면 빌드 후 변경이 없는지 확인합니다.

```bash
./gradlew build
git diff --exit-code -- src/main/frontend/generated
```

- 직접 수정하지 마세요. 다음 컴파일 때 덮어씁니다.

## 문제 해결

| 증상 | 원인 |
|---|---|
| `types.ts`가 안 생김 | `@ReactPage`/`@ReactIsland`/`@ReactType`이 하나도 없음, 또는 Maven에서 `annotationProcessorPaths` 누락 |
| 속성이 빠짐 | getter가 없는 private 필드, `@JsonIgnore` |
| 값이 `unknown` | `Object`, `JsonNode`, 또는 매핑에 없는 `java.*` 타입 |
| 실제 JSON과 다름 | Jackson을 커스터마이즈함(`PropertyNamingStrategy`, 날짜를 숫자로 등). 생성기는 기본 설정 기준입니다 |
