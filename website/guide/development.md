# 개발 모드

```properties
# src/main/resources/application-dev.properties
mustache-react.dev-server.enabled=true
spring.mustache.prefix=file:src/main/resources/templates/
spring.mustache.servlet.cache=false
```

```bash
./gradlew bootRun --args=--spring.profiles.active=dev
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- 애플리케이션이 Vite dev server를 함께 띄우고 종료할 때 함께 끕니다. 프론트 빌드가 필요 없습니다(`-PmustacheReact.skipFrontend`로 건너뛸 수 있습니다).
- 컴포넌트 수정은 새로고침 없이 반영되고(HMR) React 상태가 유지됩니다. Mustache 템플릿을 고치면 브라우저가 새로고침합니다.
- island 마운트에 실패하면 그 자리에 오류를 보여 줍니다.
- 이미 5173 포트에 dev server가 떠 있으면 그것을 씁니다. 직접 띄우려면 프론트 디렉터리에서 `npx mustache-react-vite dev`를 실행하고 `mustache-react.dev-server.auto-start=false`로 둡니다.
- Node.js는 빌드 플러그인이 내려받은 `.mustache-react/node`, 없으면 `PATH`의 `node`를 씁니다.
