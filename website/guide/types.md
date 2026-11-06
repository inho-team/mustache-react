# TypeScript 타입 생성

`mustache-react-processor`(annotation processor)가 컴파일할 때 `src/main/frontend/generated/types.ts`를 씁니다.

```tsx
import type { PageProps, OrderListResponse } from '../../generated/types';

export default function OrderList({ orders, total }: PageProps<'orders/OrderList'>) { ... }
```

- 대상: `@ReactPage` 메서드의 반환 타입, `@ReactType` 클래스, 그리고 이들이 참조하는 클래스
- `ReactPages` 맵과 `PageProps<'페이지 이름'>`으로 페이지 이름과 props 타입이 연결됩니다.
- Java 필드 이름을 바꾸면 TS 컴파일 에러로 드러납니다. 생성된 파일은 커밋해 두면 CI에서 변경을 확인할 수 있습니다.

## 매핑

Spring Boot의 Jackson이 실제로 쓰는 JSON을 따릅니다.

| Java | TypeScript |
|---|---|
| 숫자, `BigDecimal` | `number` |
| `String`, `UUID`, `java.time.*`, `Date` | `string` |
| 컬렉션, 배열 | `T[]` (`byte[]`는 `string`) |
| `Map<K, V>` | `Record<string, V>` |
| `Optional<T>`, `@Nullable` | `T \| null` |
| enum | 문자열 union |
| 제네릭 클래스 | 제네릭 interface |

`@JsonProperty`, `@JsonIgnore`, `@JsonIgnoreProperties`, record, Lombok `@Data`/`@Getter`/`@Value`를 읽습니다. 그 밖의 null 가능성은 `@Nullable`(이름이 `Nullable`인 어느 어노테이션이든)로 표시합니다.

## Gradle

플러그인이 processor와 경로를 설정합니다. 끄려면 `mustacheReact { types = false }`.

## Maven {#maven}

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
    </annotationProcessorPaths>
    <compilerArgs>
      <arg>-Amustachereact.typesFile=${project.basedir}/src/main/frontend/generated/types.ts</arg>
    </compilerArgs>
  </configuration>
</plugin>
```
