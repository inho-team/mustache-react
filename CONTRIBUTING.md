# 기여 가이드

## 개발 환경

- **JDK 17 이상:** Gradle 실행과 Boot 3/4 테스트에 필요합니다. Java 8 호환성까지 확인하려면 JDK 8도 설치합니다.
- **Node 20 이상, pnpm 10:** `corepack enable`로 `package.json`의 `packageManager` 버전을 사용할 수 있습니다.

## 빌드와 테스트

```bash
./gradlew build
```

```bash
pnpm install && pnpm build && pnpm typecheck && pnpm test
```

### Spring Boot와 JDK 조합

라이브러리 코드는 **항상 Java 8 바이트코드로, Spring Boot 2.7 API를 기준으로** 컴파일됩니다.
테스트와 예제 앱은 `-PspringBootVersion`으로 지정한 Boot 버전으로 실행됩니다.
CI는 아래 세 조합을 모두 실행합니다.

| `-PspringBootVersion` | `-PtestJavaVersion` |
|---|---|
| `2.7.18` (기본값) | `8` |
| `3.5.16` | `17` |
| `4.1.2` | `21` |

```bash
./gradlew build -PspringBootVersion=2.7.18 -PtestJavaVersion=8
```

`-PtestJavaVersion`은 Gradle toolchain으로 해당 JDK를 찾습니다. 로컬에 없으면 foojay를 통해 자동으로 내려받습니다.

예제 앱은 빌드할 때 프론트(`pnpm`)도 함께 빌드합니다. Node가 없는 환경이나 Java만 확인할 때는 `-PskipFrontend`를 붙입니다. 예제 테스트는 고정된 테스트용 manifest를 쓰므로 프론트 빌드 없이도 통과합니다.

## 코드 규칙

### Java
- `javax.servlet`, `jakarta.servlet` 타입을 **직접 참조하지 않습니다.** 요청 정보는 `RequestContextHolder`로 읽습니다. 이렇게 해야 하나의 바이너리가 Boot 2.7과 3.x/4.x에서 함께 동작합니다.
- Java 8에 없는 API(`List.of`, `var`, `String.isBlank` 등)는 쓰지 않습니다. 컴파일 옵션 `--release 8`이 이를 검사합니다.
- 다른 auto-configuration을 참조할 때는 클래스 대신 이름 문자열(`afterName`)을 씁니다. Boot 4에서 패키지가 바뀌었기 때문입니다.
- Spring 내부 클래스를 `instanceof`로 찾지 않습니다. 버전마다 감싸는 클래스가 달라질 수 있으므로 `supportsReturnType` 같은 동작으로 판단합니다.

### JS/TS
- 공개 패키지는 TypeScript로 작성하고 ESM으로 배포합니다.
- 서버와 브라우저가 공유하는 HTML 표식은 `Protocol.java`와 `js/client/src/protocol.ts`에 똑같이 정의합니다. 두 파일이 일치하는지는 테스트가 검사합니다.

## 커밋과 PR

- 커밋 메시지는 `feat:`, `fix:`, `docs:`, `build:`, `test:`, `refactor:` 접두사를 사용합니다.
- PR을 올리기 전에 Java 빌드와 JS 테스트를 모두 통과시켜 주세요.
- 사용자에게 보이는 API나 동작이 바뀌면 `docs/`도 함께 수정합니다.

## 라이선스

기여한 코드는 [Apache License 2.0](LICENSE)으로 배포됩니다.
