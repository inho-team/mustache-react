# 호환성

| | 지원 | 확인 방법 |
|---|---|---|
| Java | 8, 11, 17, 21 | CI: Boot 2.7 + Java 8, Boot 3.5 + Java 17, Boot 4.1 + Java 21 |
| Spring Boot | 2.7, 3.5, 4.x | 같은 jar로 세 버전 모두 테스트 |
| Spring Security | 5.7, 6.x, 7.x (선택) | 로그인, CSRF 테스트 |
| Jackson | 2, 3 | Spring MVC의 HttpMessageConverter 사용 |
| Gradle | 7.6 이상 | TestKit로 7.6.4, 8.x |
| Maven | 3.6 이상 | 예제를 Maven으로 빌드 |
| React | 18 이상 | |
| Vite | 5 이상 (권장 8) | |
| Node.js | 18 이상, 기본 다운로드 24 LTS | |
| OS | Linux, Windows, macOS | CI: Linux 전체, Windows에서 빌드 플러그인·Java·소비자 테스트 |

라이브러리는 `javax.servlet`과 `jakarta.servlet` 어느 쪽에도 직접 의존하지 않아 하나의 바이너리가 Boot 2.7과 3+ 모두에서 동작합니다.

## 버전 맞추기

Java artifact, Gradle/Maven 플러그인, npm 패키지(`@mustache-react/*`)는 같은 버전으로 함께 릴리스됩니다. 버전이 섞이면 경고합니다.

- 브라우저 콘솔: `@mustache-react/client`와 서버 라이브러리의 버전이 다를 때
- 애플리케이션 로그: 프론트를 빌드한 `@mustache-react/vite-plugin`과 서버 라이브러리의 버전이 다를 때
