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

라이브러리는 `javax.servlet`과 `jakarta.servlet` 어느 쪽에도 직접 의존하지 않아 하나의 바이너리가 Boot 2.7과 3+ 모두에서 동작합니다.
